package com.carrierfraud.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * RiskAlert represents a fraud detection alert that has fired and needs investigation.
 *
 * LIFECYCLE:
 * Alert fires → Auto-routed to department → Person accepts → SLA tracking → Resolution
 *
 * This is the operational hub: where fraud detection meets business workflow.
 *
 * STATE MACHINE:
 * UNASSIGNED (alert just fired)
 *   - Routed to correct department
 *   - Visible on department dashboard
 *   - SLA clock starts (15 min for CRITICAL, 1 hour for HIGH, etc.)
 *
 * ASSIGNED (routed to department)
 *   - Department received the alert
 *   - On department queue
 *   - Waiting for person to claim it
 *
 * ACCEPTED (person claimed it)
 *   - Specific person owns the investigation
 *   - Timestamp: acceptedDate and acceptedBy recorded
 *   - SLA window tightens (must complete investigation before deadline)
 *
 * IN_PROGRESS (under investigation)
 *   - Person is actively working on it
 *   - Notes being added
 *   - Evidence being collected
 *
 * RESOLVED (investigation complete)
 *   - Decision made (fraud confirmed, false positive, etc.)
 *   - Case closed
 *   - SLA met or exceeded (tracked for metrics)
 *
 * ESCALATED (elevated to higher authority)
 *   - Manager override, legal escalation
 *   - Reassigned to different department/person
 *   - Requires human judgment beyond original scope
 *
 * DIFFERENTIATOR FEATURE:
 * Smart routing means:
 * - COMMERCIAL_DISPUTE → LEGAL (expert can decide immediately)
 * - ACCIDENT → INSURANCE (specialist handles claim)
 * - PAYMENT_ISSUE → PAYMENT_RECONCILIATION (operational team can fix)
 *
 * This solves WTRANSNET bottleneck: no more "waiting for lucky person."
 * Cases reach right expertise in MINUTES, not days.
 */
public class RiskAlert {

    // ============ IMMUTABLE FIELDS (Alert Identity & Detection) ============

    private final String alertId;
    private final String carrierName;
    private final double riskScore;
    private final String triggeredRuleNames;
    private final LocalDateTime createdDate;

    // ============ MUTABLE FIELDS (Routing & Assignment) ============

    private AlertSeverity severity;
    private Department assignedDepartment;
    private AlertAssignmentStatus assignmentStatus;

    private String assignedTo;
    private String acceptedBy;
    private LocalDateTime acceptedDate;
    private LocalDateTime slaDeadline;
    private LocalDateTime resolvedDate;
    private String resolutionNotes;
    private String escalatedTo;

    // ============ CONSTRUCTOR ============

    /**
     * Create a new alert that just fired from fraud detection rules.
     * Alert starts in UNASSIGNED state and will be auto-routed.
     *
     * @param alertId unique identifier for this alert
     * @param carrierName the carrier that triggered the alert
     * @param riskScore fraud rule score (0.0 to 1.0+)
     * @param triggeredRuleNames which rules fired (e.g., "PaymentReconciliationRule, ComplaintAccumulationRule")
     * @param severity alert severity (CRITICAL, HIGH, MEDIUM, LOW, INFO)
     * @param department which department should handle this
     *
     * @throws IllegalArgumentException if parameters invalid
     */
    public RiskAlert(
            String alertId,
            String carrierName,
            double riskScore,
            String triggeredRuleNames,
            AlertSeverity severity,
            Department department
    ) {
        // VALIDATION: alertId
        Objects.requireNonNull(alertId, "alertId cannot be null");
        if (alertId.isBlank()) {
            throw new IllegalArgumentException("alertId cannot be empty");
        }
        this.alertId = alertId;

        // VALIDATION: carrierName
        Objects.requireNonNull(carrierName, "carrierName cannot be null");
        if (carrierName.isBlank()) {
            throw new IllegalArgumentException("carrierName cannot be empty");
        }
        if (carrierName.length() > 50) {
            throw new IllegalArgumentException("carrierName too long");
        }
        this.carrierName = carrierName;

        // VALIDATION: riskScore (can be > 1.0 if multiple rules accumulate)
        if (riskScore < 0) {
            throw new IllegalArgumentException("riskScore cannot be negative");
        }
        this.riskScore = riskScore;

        // VALIDATION: triggeredRuleNames
        Objects.requireNonNull(triggeredRuleNames, "triggeredRuleNames cannot be null");
        if (triggeredRuleNames.isBlank()) {
            throw new IllegalArgumentException("at least one rule must have triggered");
        }
        this.triggeredRuleNames = triggeredRuleNames;

        // VALIDATION: severity and department
        Objects.requireNonNull(severity, "severity cannot be null");
        Objects.requireNonNull(department, "department cannot be null");

        // INITIALIZATION: Alert just fired
        this.createdDate = LocalDateTime.now();
        this.severity = severity;
        this.assignedDepartment = department;
        this.assignmentStatus = AlertAssignmentStatus.UNASSIGNED;

        // Not yet assigned to person
        this.assignedTo = null;
        this.acceptedBy = null;
        this.acceptedDate = null;
        this.slaDeadline = calculateSlaDeadline(severity);
        this.resolvedDate = null;
        this.resolutionNotes = null;
        this.escalatedTo = null;
    }

    // ============ GETTERS (Immutable) ============

    public String getAlertId() {
        return alertId;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public String getTriggeredRuleNames() {
        return triggeredRuleNames;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    // ============ GETTERS (Mutable State) ============

    public AlertSeverity getSeverity() {
        return severity;
    }

    public Department getAssignedDepartment() {
        return assignedDepartment;
    }

    public AlertAssignmentStatus getAssignmentStatus() {
        return assignmentStatus;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public String getAcceptedBy() {
        return acceptedBy;
    }

    public LocalDateTime getAcceptedDate() {
        return acceptedDate;
    }

    public LocalDateTime getSlaDeadline() {
        return slaDeadline;
    }

    public LocalDateTime getResolvedDate() {
        return resolvedDate;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public String getEscalatedTo() {
        return escalatedTo;
    }

    // ============ STATE TRANSITIONS (Behavior) ============

    /**
     * Person accepts the alert - takes ownership.
     * Transitions from ASSIGNED → ACCEPTED.
     *
     * This is the critical moment where accountability begins.
     * The alert is no longer in the queue - it's someone's responsibility.
     *
     * @param personName who is accepting (email or username)
     *
     * @throws IllegalStateException if already accepted
     */
    public void accept(String personName) {
        if (assignmentStatus == AlertAssignmentStatus.ACCEPTED
            || assignmentStatus == AlertAssignmentStatus.IN_PROGRESS
            || assignmentStatus == AlertAssignmentStatus.RESOLVED) {
            throw new IllegalStateException(
                String.format(
                    "Cannot accept alert %s: already in %s state (accepted by %s)",
                    alertId,
                    assignmentStatus,
                    acceptedBy
                )
            );
        }

        Objects.requireNonNull(personName, "personName cannot be null");
        if (personName.isBlank()) {
            throw new IllegalArgumentException("personName cannot be empty");
        }

        this.acceptedBy = personName;
        this.acceptedDate = LocalDateTime.now();
        this.assignmentStatus = AlertAssignmentStatus.ACCEPTED;
        this.assignedTo = personName;
    }

    /**
     * Start investigation.
     * Transitions to IN_PROGRESS.
     * SLA clock is running.
     *
     * @throws IllegalStateException if not accepted yet
     */
    public void startInvestigation() {
        if (assignmentStatus != AlertAssignmentStatus.ACCEPTED) {
            throw new IllegalStateException(
                String.format(
                    "Cannot start investigation: alert must be ACCEPTED, currently %s",
                    assignmentStatus
                )
            );
        }

        this.assignmentStatus = AlertAssignmentStatus.IN_PROGRESS;
    }

    /**
     * Resolve the alert with findings.
     * Transitions to RESOLVED.
     * Closes the SLA window.
     *
     * @param notes investigation findings and decision
     *
     * @throws IllegalStateException if not in progress
     */
    public void resolve(String notes) {
        if (assignmentStatus != AlertAssignmentStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                String.format(
                    "Cannot resolve: alert must be IN_PROGRESS, currently %s",
                    assignmentStatus
                )
            );
        }

        Objects.requireNonNull(notes, "resolution notes cannot be null");
        if (notes.isBlank()) {
            throw new IllegalArgumentException("resolution notes cannot be empty");
        }

        this.assignmentStatus = AlertAssignmentStatus.RESOLVED;
        this.resolvedDate = LocalDateTime.now();
        this.resolutionNotes = notes;
    }

    /**
     * Escalate to higher authority.
     * Can happen from any non-resolved state.
     *
     * @param escalatedToDepartment where to escalate (usually LEGAL or management)
     *
     * @throws IllegalStateException if already resolved
     */
    public void escalate(Department escalatedToDepartment) {
        if (assignmentStatus == AlertAssignmentStatus.RESOLVED) {
            throw new IllegalStateException(
                String.format(
                    "Cannot escalate resolved alert %s",
                    alertId
                )
            );
        }

        Objects.requireNonNull(escalatedToDepartment, "escalation department cannot be null");

        this.assignmentStatus = AlertAssignmentStatus.ESCALATED;
        this.escalatedTo = escalatedToDepartment.toString();
        this.assignedDepartment = escalatedToDepartment;
    }

    // ============ COMPUTED PROPERTIES (SLA & Metrics) ============

    /**
     * Is this alert overdue (past SLA deadline)?
     * Used by dashboard to show red/yellow alerts.
     *
     * @return true if deadline has passed and not yet resolved
     */
    public boolean isOverdue() {
        if (assignmentStatus == AlertAssignmentStatus.RESOLVED) {
            return false; // Resolved, SLA no longer applies
        }
        return LocalDateTime.now().isAfter(slaDeadline);
    }

    /**
     * How many minutes until SLA deadline?
     * Negative = already overdue.
     *
     * @return minutes remaining, or negative if overdue
     */
    public long getMinutesUntilSlaDeadline() {
        return java.time.temporal.ChronoUnit.MINUTES.between(LocalDateTime.now(), slaDeadline);
    }

    /**
     * Was SLA met? (resolved before deadline)
     *
     * @return true if resolved before deadline
     */
    public boolean wasSlaMetr() {
        if (assignmentStatus != AlertAssignmentStatus.RESOLVED) {
            return false; // Not resolved yet
        }
        return resolvedDate.isBefore(slaDeadline) || resolvedDate.isEqual(slaDeadline);
    }

    /**
     * How long from alert fire to acceptance?
     * Time to Ownership metric (should be < 1 hour).
     *
     * @return minutes, or -1 if not accepted yet
     */
    public long getTimeToOwnershipMinutes() {
        if (acceptedDate == null) {
            return -1;
        }
        return java.time.temporal.ChronoUnit.MINUTES.between(createdDate, acceptedDate);
    }

    // ============ PRIVATE HELPERS ============

    /**
     * Calculate SLA deadline based on severity.
     * CRITICAL: 15 minutes
     * HIGH: 1 hour
     * MEDIUM: 24 hours
     * LOW: 5 days
     * INFO: 10 days
     */
    private LocalDateTime calculateSlaDeadline(AlertSeverity sev) {
        return switch (sev) {
            case CRITICAL -> LocalDateTime.now().plusMinutes(15);
            case HIGH -> LocalDateTime.now().plusHours(1);
            case MEDIUM -> LocalDateTime.now().plusDays(1);
            case LOW -> LocalDateTime.now().plusDays(5);
            case INFO -> LocalDateTime.now().plusDays(10);
        };
    }

    // ============ OBJECT CONTRACT ============

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        RiskAlert other = (RiskAlert) obj;
        return alertId.equals(other.alertId)
            && carrierName.equals(other.carrierName)
            && createdDate.equals(other.createdDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alertId, carrierName, createdDate);
    }

    @Override
    public String toString() {
        return String.format(
            "RiskAlert{id=%s, carrier=%s, score=%.2f, severity=%s, status=%s, deadline=%s}",
            alertId,
            carrierName,
            riskScore,
            severity,
            assignmentStatus,
            slaDeadline
        );
    }
}
