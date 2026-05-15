package com.carrierfraud.domain;

/**
 * Alert severity level - determines department routing priority and SLA windows.
 * 
 * Severity is calculated from fraud rule scores:
 * - CRITICAL (0.8+): Immediate escalation (LEGAL, COMPLIANCE)
 * - HIGH (0.6-0.8): Within 2 hours (FRAUD_INVESTIGATION, INSURANCE)
 * - MEDIUM (0.4-0.6): Within 24 hours (PAYMENT_RECONCILIATION, OPERATIONS)
 * - LOW (0.2-0.4): Within 5 days (SALES, ACCOUNT_MANAGEMENT)
 * - INFO (< 0.2): Informational only (no SLA)
 * 
 * Examples:
 * CRITICAL: 3+ unresolved commercial disputes (Rule 3)
 * HIGH: 2+ accidents in 1 month (Rule 3) OR 80%+ unpaid rate (Rule 1)
 * MEDIUM: 5+ open cases simultaneously (Rule 3) OR price escalation >20% (Rule 2)
 * LOW: 10+ cases/week mediated (Rule 3 early warning)
 * INFO: 20+ cases/month (trend tracking)
 */
public enum AlertSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO
}
