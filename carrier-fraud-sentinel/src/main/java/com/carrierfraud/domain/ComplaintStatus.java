package com.carrierfraud.domain;

/**
 * Complaint lifecycle status - represents state transitions in case management.
 * 
 * This is a state machine with strict transitions:
 * UNRESOLVED (initial) → SOLVED (terminal)
 * 
 * - UNRESOLVED: Complaint filed but not yet resolved
 *   Meaning: Still waiting for investigation completion, documentation, or resolution
 *   Rule 3 counts unresolved complaints: 10+ per week = ALERT, 3+ disputes = CRITICAL
 *   
 * - SOLVED: Complaint investigation completed and closed
 *   Meaning: Case resolved (regardless of outcome - paid, rejected, escalated, etc.)
 *   Resolution reason is tracked in resolutionReason field
 *   No longer counted in active complaint metrics
 * 
 * DDD Invariant: A complaint cannot transition to SOLVED without:
 * 1. Required documentation (CMR, ALBARAN, communication trail)
 * 2. A valid resolution reason
 * 3. A resolved date timestamp
 */
public enum ComplaintStatus {
    UNRESOLVED,
    SOLVED
}
