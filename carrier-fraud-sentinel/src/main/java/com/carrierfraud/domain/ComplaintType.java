package com.carrierfraud.domain;

/**
 * Complaint type classification for fraud investigation and case routing.
 * 
 * These types represent the operational reality of WTRANSNET's complaint handling,
 * as defined from 6 years of fraud investigation expertise at Alpega:
 * 
 * - OPEN_CASE: Complaint filed by transportista, under investigation
 * - ACCIDENT: Transport-related incident (damage, loss, injury during transport)
 * - COMMERCIAL_DISPUTE: Disagreement over commercial terms or service delivery
 * - INSURANCE: Insurance claim (platform doesn't intervene, handled by insurance)
 * - REVIEWING: Internal platform review (missing documentation, clarification needed)
 * 
 * Each type triggers different department routing and SLA windows:
 * ACCIDENT → INSURANCE_DEPT (expedited)
 * COMMERCIAL_DISPUTE → LEGAL_DEPT (high priority, DDD invariant: 3+ = CRITICAL)
 * OPEN_CASE → PAYMENT_RECONCILIATION_TEAM (tracks payment status)
 * INSURANCE → handled separately (no platform intervention)
 * REVIEWING → COMPLIANCE_REVIEW (documentation validation)
 */
public enum ComplaintType {
    OPEN_CASE,
    ACCIDENT,
    COMMERCIAL_DISPUTE,
    INSURANCE,
    REVIEWING
}
