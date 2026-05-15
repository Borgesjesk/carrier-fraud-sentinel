package com.carrierfraud.domain;

/**
 * StrategyRule defines the interface for fraud detection rules.
 *
 * DESIGN PATTERN: Strategy Pattern
 * - Each rule is independent (can be added/removed without changing others)
 * - Rules are pluggable (loaded at runtime from Spring container)
 * - Rules compose (orchestrator combines results)
 *
 * SCORING SYSTEM:
 * - Each rule returns a double (0.0 to 1.0, can exceed 1.0 if combining)
 * - 0.0 = CLEAN (no risk)
 * - 0.5 = MEDIUM RISK
 * - 1.0 = HIGH RISK
 * - > 1.0 = CRITICAL (multiple rules fired)
 *
 * IMPLEMENTATION:
 * - Rule1: PaymentReconciliationRule (Rule 1: payment unpaid rate)
 * - Rule2: OfferPriceEscalationRule (Rule 2: price jumps)
 * - Rule3: ComplaintAccumulationRule (Rule 3: complaint thresholds) - uses Complaint list
 *
 * WHY INTERFACE?
 * - Future rules can be added without changing orchestrator
 * - Each rule is testable in isolation
 * - Rules are Spring components (auto-discovered)
 */
public interface StrategyRule {

    /**
     * Evaluate this rule against a transaction.
     *
     * Rule-specific implementations will determine what score to return.
     * Orchestrator combines all rule scores.
     *
     * @param transaction the transaction to evaluate
     * @return score from 0.0 (clean) to 1.0+ (critical)
     */
    double evaluate(Transaction transaction);

    /**
     * Human-readable name of this rule.
     * Used in alert messages: "Triggered by: PaymentReconciliationRule, ComplaintAccumulationRule"
     *
     * @return name of this rule
     */
    String name();
}
