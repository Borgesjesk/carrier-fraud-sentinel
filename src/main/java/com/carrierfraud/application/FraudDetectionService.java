package com.carrierfraud.application;

import com.carrierfraud.domain.*;
import com.carrierfraud.infrastructure.RiskAlertRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class FraudDetectionService {

    private final List<StrategyRule> rules;
    private final List<AlertObserver> observers;
    private final RiskAlertRepository alertRepository;

    private static final double THRESHOLD = 0.5;
    private static final double CRITICAL_THRESHOLD = 1.5;

    public FraudDetectionService(
            List<StrategyRule> rules,
            List<AlertObserver> observers,
            RiskAlertRepository alertRepository
    ) {
        this.rules = rules;
        this.observers = observers;
        this.alertRepository = alertRepository;
    }

    public RiskAlert analyseAndTag(Transaction transaction, String createdBy) {
        RiskAlert alert = analyse(transaction);
        if (alert != null && createdBy != null) {
            alert.setCreatedBy(createdBy);
            alertRepository.save(alert);
        }
        return alert;
    }

    public RiskAlert analyse(Transaction transaction) {
        Objects.requireNonNull(transaction, "Transaction cannot be null");

        double totalScore = 0.0;
        StringBuilder triggeredRules = new StringBuilder();

        for (StrategyRule rule : rules) {
            double ruleScore = rule.evaluate(transaction);

            if (ruleScore > 0.0) {
                totalScore += ruleScore;
                if (triggeredRules.length() > 0) {
                    triggeredRules.append(", ");
                }
                triggeredRules.append(rule.name());
            }
        }

        if (totalScore < THRESHOLD) {
            return null;
        }

        AlertSeverity severity = determineSeverity(totalScore);

        Department department = determineRountingDepartment(
                totalScore,
                triggeredRules.toString());

        String alertId = generateAlertId(transaction.getCarrierName());
        RiskAlert alert = new RiskAlert(
                alertId,
                transaction.getCarrierName(),
                totalScore,
                triggeredRules.toString(),
                severity,
                department
        );

        alertRepository.save(alert);

        for (AlertObserver observer : observers) {
            observer.notify(alert);
        }

        return alert;
    }

    private AlertSeverity determineSeverity(double score) {
        if (score >= CRITICAL_THRESHOLD) {
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

    private Department determineRountingDepartment(double score, String triggeredRules) {
        if (score >= CRITICAL_THRESHOLD) {
            return Department.LEGAL;
        }

        if (triggeredRules.contains("ComplaintAccumulationRule")) {
            if (triggeredRules.contains("Accident")) {
                return Department.INSURANCE;
            }
            return Department.LEGAL;
        }

        if (triggeredRules.contains("PaymentReconciliationRule")) {
            return Department.MEDIATION;
        }

        if (triggeredRules.contains("OfferPriceEscalationRule")) {
            return Department.FRAUD_INVESTIGATION;
        }

        if (score >= 1.0) {
            return Department.MEDIATION;
        } else if (score >= 0.6) {
            return Department.DEPARTMENT_MANAGER;
        } else {
            return Department.COMPLIANCE_REVIEW;
        }
    }

    private String generateAlertId(String carrierName) {
        return String.format(
                "ALERT_%s_%d_%d",
                carrierName.toUpperCase().replaceAll("[^A-Z0-9]", ""),
                System.currentTimeMillis(),
                System.nanoTime() % 10000
        );
    }
}