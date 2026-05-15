package com.carrierfraud.domain;

/**
 * How a complaint was ultimately resolved.
 * 
 * Only set when status transitions to SOLVED.
 * 
 * - RECEIVED_PAYMENT: Transportista received the disputed payment
 * - NOT_RECEIVED: Payment never came, case closed without resolution (escalated to legal)
 * - INCIDENT: Non-payment caused by incident during transport
 * - ACCIDENT: Accident during transport (damage/loss)
 * - COMMERCIAL_DISPUTE: Dispute over commercial terms (resolution depends on negotiation)
 * - INSURANCE: Resolved via insurance claim (platform delegates to insurance)
 * 
 * Business Rules:
 * - COMMERCIAL_DISPUTE + status=UNRESOLVED BEFORE closing = counts toward Rule 3 CRITICAL
 * - ACCIDENT + within 30 days = counts toward Rule 3 (2+ accidents = ALERT)
 * - NOT_RECEIVED = legal escalation (requires LEGAL_DEPT attention)
 */
public enum ResolutionReason {
    RECEIVED_PAYMENT,
    NOT_RECEIVED,
    INCIDENT,
    ACCIDENT,
    COMMERCIAL_DISPUTE,
    INSURANCE
}
