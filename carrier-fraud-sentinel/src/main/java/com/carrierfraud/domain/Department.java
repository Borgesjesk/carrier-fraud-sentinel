package com.carrierfraud.domain;

/**
 * Department routing for intelligent alert assignment.
 * 
 * Each alert auto-routes to the appropriate department based on fraud rule type:
 * 
 * LEGAL: 3+ unresolved commercial disputes = CRITICAL escalation
 * INSURANCE: 2+ accidents/month OR insurance-related complaints
 * PAYMENT_RECONCILIATION: 80%+ unpaid rate OR 5+ open cases simultaneously
 * FRAUD_INVESTIGATION: Price escalation >20% OR suspicious payment patterns
 * SALES: < 10 successful offers/month (retention crisis - soft alert)
 * ACCOUNT_MANAGEMENT: > 100 successful offers/month (upsell opportunity - soft alert)
 * COMPLIANCE_REVIEW: 50+ reported incidents (company evaluation)
 * OPERATIONS_MANAGEMENT: 10+ cases/week mediated (volume crisis)
 * 
 * Workflow:
 * 1. Alert fires with rule evaluation
 * 2. System auto-routes to department
 * 3. Department members see UNASSIGNED alert on dashboard
 * 4. Team member clicks ACCEPT → alert becomes theirs (ASSIGNED state)
 * 5. SLA timer starts
 * 6. Status transitions: UNASSIGNED → ASSIGNED → ACCEPTED → IN_PROGRESS → RESOLVED
 * 
 * This solves WTRANSNET bottleneck: no more waiting for luck/person availability.
 */
public enum Department {
    LEGAL,
    INSURANCE,
    PAYMENT_RECONCILIATION,
    FRAUD_INVESTIGATION,
    SALES,
    ACCOUNT_MANAGEMENT,
    COMPLIANCE_REVIEW,
    OPERATIONS_MANAGEMENT
}
