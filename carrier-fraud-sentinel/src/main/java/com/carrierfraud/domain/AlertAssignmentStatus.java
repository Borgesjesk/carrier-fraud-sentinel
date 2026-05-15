package com.carrierfraud.domain;

/**
 * Alert assignment lifecycle - tracks accountability and SLA compliance.
 * 
 * Strict state machine:
 * UNASSIGNED (alert fires) 
 *   → ASSIGNED (routed to department queue)
 *   → ACCEPTED (person clicks ACCEPT - they own it)
 *   → IN_PROGRESS (person is investigating)
 *   → RESOLVED (case closed, decision made)
 *   [or ESCALATED at any point if situation changes]
 * 
 * - UNASSIGNED: Alert fired, routed to department, waiting for someone to take it
 *   SLA: Must be ACCEPTED within 15 minutes (critical) / 1 hour (high) / 24 hours (medium)
 *   
 * - ASSIGNED: Department received alert, visible in their dashboard
 *   Meaning: Alert is officially routed, waiting for person to click ACCEPT
 *   
 * - ACCEPTED: Person claimed the alert (clicked ACCEPT button)
 *   Timestamp: acceptedDate recorded, acceptedBy recorded
 *   SLA: Investigation must complete within severity window (2hrs for HIGH, 24hrs for MEDIUM)
 *   
 * - IN_PROGRESS: Person is actively investigating
 *   Activity: notes being added, case status being tracked
 *   
 * - RESOLVED: Investigation complete, decision made, case closed
 *   Outcome: Resolution reason recorded
 *   
 * - ESCALATED: Moved to higher authority (manager override, legal escalation)
 *   Reason: Requires human judgment beyond original department
 * 
 * Metrics:
 * - UNASSIGNED → ACCEPTED = Time to Ownership (target: <1 hour)
 * - ASSIGNED → RESOLVED = Total Case Time (target: varies by severity)
 * - SLA breaches = alerts that weren't ACCEPTED before deadline
 */
public enum AlertAssignmentStatus {
    UNASSIGNED,
    ASSIGNED,
    ACCEPTED,
    IN_PROGRESS,
    RESOLVED,
    ESCALATED
}
