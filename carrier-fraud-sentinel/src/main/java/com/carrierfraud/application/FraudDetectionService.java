package com.carrierfraud.application;

import com.carrierfraud.domain.*;
import com.carrierfraud.infrastructure.RiskAlertRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * FraudDetectionService orchestrates the fraud detection pipeline.
 *
 * PIPELINE:
 * 1. Accept Transaction (already validated by constructor + validator)
 * 2. Run all fraud detection rules
 * 3. Calculate combined fraud score
 * 4. Determine severity and routing
 * 5. Create RiskAlert
 * 6. Notify all observers (Console, AuditLog, Database)
 * 7. Return alert to caller
 *
 * SCORING:
 * - Each rule returns 0.0 to 1.0+
 * - Rules are summed (can exceed 1.0 if multiple rules fire)
 * - If totalScore >= threshold → ALERT
 * - If totalScore >= 1.5 → CRITICAL
 *
 * SEVERITY DETERMINATION:
 * - 1.5+ = CRITICAL (SLA: 15 minutes)
 * - 1.0-1.5 = HIGH (SLA: 1 hour)
 * - 0.6-1.0 = MEDIUM (SLA: 24 hours)
 * - 0.3-0.6 = LOW (SLA: 5 days)
 * - <0.3 = INFO (SLA: 10 days)
 *
 * AUTO-ROUTING:
 * - Severity + rule combination determines department
 * - Example: CRITICAL + ComplaintAccumulation → LEGAL
 * - Example: HIGH + PaymentReconciliation → PAYMENT_RECONCILIATION
 * - Routing happens via determineRoutingDepartment()
 *
 * OBSERVER PATTERN:
 * - All observers are notified when alert fires
 * - Console: Prints alert (real-time visibility)
 * - AuditLog: Writes to file (compliance)
 * - Database: Saves to MongoDB (operational history)
 *
 * THREAD SAFETY:
 * - Transaction is immutable (rules can run concurrently)
 * - Each call to analyze() is independent
 * - No shared state between calls
 */
@Service
public class FraudDetectionService {

    private final List<StrategyRule> rules;
    private final List<AlertObserver> observers;
    private final RiskAlertRepository alertRepository;

    @Value("${fraud.detection.threshold:0.5}")
    private double threshold;

    @Value("${fraud.detection.critical-threshold:1.5}")
    private double criticalThreshold;

    /**
     * Constructor receives all rules and observers via dependency injection.
     * Spring auto-discovers @Component classes and injects them.
     *
     * @param rules all StrategyRule implementations (auto-discovered)
     * @param observers all AlertObserver implementations (auto-discovered)
     * @param alertRepository MongoDB repository for persisting alerts
     */
    public FraudDetectionService(
            List<StrategyRule> rules,
            List<AlertObserver> observers,
            RiskAlertRepository alertRepository
    ) {
        Objects.requireNonNull(rules, "rules cannot be null");
        Objects.requireNonNull(observers, "observers cannot be null");
        Objects.requireNonNull(alertRepository, "alertRepository cannot be null");

        this.rules = rules;
        this.observers = observers;
        this.alertRepository = alertRepository;
    }

    /**
     * Analyze a transaction for fraud.
     *
     * PIPELINE:
     * 1. Run each fraud rule
     * 2. Sum scores
     * 3. If score >= threshold, create alert
     * 4. Determine severity and route to department
     * 5. Notify all observers
     * 6. Save to database
     * 7. Return alert
     *
     * @param transaction the transaction to analyze
     * @return RiskAlert if fraud detected, null if clean
     *
     * @throws IllegalArgumentException if transaction is null
     */
    public RiskAlert analyse(Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction cannot be null");

        // STEP 1: Run all rules
        double totalScore = 0.0;
        StringBuilder triggeredRules = new StringBuilder();

        for (StrategyRule rule : rules) {
            double ruleScore = rule.evaluate(transaction);

            // Only record rules that contributed (score > 0)
            if (ruleScore > 0.0) {
                totalScore += ruleScore;
                if (triggeredRules.length() > 0) {
                    triggeredRules.append(", ");
                }
                triggeredRules.append(rule.name());
            }
        }

        // STEP 2: Check if alert should fire
        if (totalScore < threshold) {
            return null;  // No alert, carrier is clean
        }

        // STEP 3: Determine severity
        AlertSeverity severity = determineSeverity(totalScore);

        // STEP 4: Determine routing department
        Department department = determineRoutingDepartment(totalScore, triggeredRules.toString());

        // STEP 5: Create alert
        String alertId = generateAlertId(transaction.getCarrierName());
        RiskAlert alert = new RiskAlert(
            alertId,
            transaction.getCarrierName(),
            totalScore,
            triggeredRules.toString(),
            severity,
            department
        );

        // STEP 6: Save to database
        alertRepository.save(alert);

        // STEP 7: Notify observers
        for (AlertObserver observer : observers) {
            observer.notify(alert);
        }

        // STEP 8: Return alert
        return alert;
    }

    // ============ HELPER METHODS ============

    /**
     * Determine alert severity based on fraud score.
     *
     * @param score the total fraud score
     * @return severity level (CRITICAL, HIGH, MEDIUM, LOW, INFO)
     */
    private AlertSeverity determineSeverity(double score) {
        if (score >= criticalThreshold) {
            return AlertSeverity.CRITICAL;
        } else if (score >= 1.0) {
            return AlertSeverity.HIGH;
        } else if (score >= 0.6) {
            return AlertSeverity.MEDIUM;
        } else if (score >= 0.3) {
            return AlertSeverity.LOW;
        } else {
            return AlertSeverity.INFO;
        }
    }

    /**
     * Determine which department should handle this alert.
     * Auto-routing based on fraud score and which rules triggered.
     *
     * ROUTING LOGIC:
     * - CRITICAL (1.5+) → LEGAL (highest authority)
     * - Complaints (Rule 3) → LEGAL (disputes) or INSURANCE (accidents)
     * - Payment (Rule 1) → PAYMENT_RECONCILIATION
     * - Price (Rule 2) → FRAUD_INVESTIGATION
     * - Default HIGH → PAYMENT_RECONCILIATION
     * - Default MEDIUM → OPERATIONS_MANAGEMENT
     *
     * @param score the total fraud score
     * @param triggeredRules which rules contributed
     * @return department to route to
     */
    private Department determineRoutingDepartment(double score, String triggeredRules) {
        // CRITICAL → Always LEGAL
        if (score >= criticalThreshold) {
            return Department.LEGAL;
        }

        // Route based on which rules triggered
        if (triggeredRules.contains("ComplaintAccumulationRule")) {
            // Complaints are serious
            if (triggeredRules.contains("Accident")) {
                return Department.INSURANCE;
            }
            return Department.LEGAL;
        }

        if (triggeredRules.contains("PaymentReconciliationRule")) {
            return Department.PAYMENT_RECONCILIATION;
        }

        if (triggeredRules.contains("OfferPriceEscalationRule")) {
            return Department.FRAUD_INVESTIGATION;
        }

        // Default routing by severity
        if (score >= 1.0) {
            return Department.PAYMENT_RECONCILIATION;  // HIGH
        } else if (score >= 0.6) {
            return Department.OPERATIONS_MANAGEMENT;  // MEDIUM
        } else {
            return Department.COMPLIANCE_REVIEW;  // LOW
        }
    }

    /**
     * Generate unique alert ID.
     * Format: ALERT_{CARRIER}_{TIMESTAMP}_{RANDOM}
     *
     * @param carrierName the carrier name
     * @return unique alert ID
     */
    private String generateAlertId(String carrierName) {
        return String.format(
            "ALERT_%s_%d_%d",
            carrierName.toUpperCase().replaceAll("[^A-Z0-9]", ""),
            System.currentTimeMillis(),
            System.nanoTime() % 10000
        );
    }
}
