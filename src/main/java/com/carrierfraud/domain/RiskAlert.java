package com.carrierfraud.domain;

import java.util.Objects;
import java.time.LocalDateTime;


public class RiskAlert {

    private final String alertId;
    private final String carrierName;
    private final double riskScore;
    private final String triggeredRuleNames;
    private final LocalDateTime createdDate;

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


    public RiskAlert(
            String alertId,
            String carrierName,
            double riskScore,
            String triggeredRuleNames,
            AlertSeverity severity,
            Department department
    ) {

        Objects.requireNonNull(alertId, "Alert ID cannot be null");
        if (alertId.isBlank()) {
            throw new IllegalArgumentException("Alert ID cannot be empty");
        }
        this.alertId = alertId;

        Objects.requireNonNull(carrierName, "Carrier name cannot be null");
        if (carrierName.isBlank()) {
            throw new IllegalArgumentException("Carrier name cannot be empty");
        }
        this.carrierName = carrierName;

        if (riskScore < 0) {
            throw new IllegalArgumentException("Risk score cannot be negative");
        }
        this.riskScore = riskScore;

        Objects.requireNonNull(triggeredRuleNames, "Triggered rule names cannot be null");
        if (triggeredRuleNames.isBlank()) {
            throw new IllegalArgumentException("At least one rule must have triggered");
        }
        this.triggeredRuleNames = triggeredRuleNames;

        Objects.requireNonNull(severity, "Severity cannot be null");
        Objects.requireNonNull(department, "Department cannot be null");

        this.createdDate = LocalDateTime.now();
        this.severity = severity;
        this.assignedDepartment = department;
        this.assignmentStatus = AlertAssignmentStatus.UNASSIGNED;

        this.assignedTo = null;
        this.escalatedTo = null;
        this.acceptedDate = null;
        this.slaDeadline = calculateSlaDeadLine(severity);
        this.resolvedDate = null;
        this.resolutionNotes = null;
        this.escalatedTo = null;
    }

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

    public void accept(String personName) {
        if (assignmentStatus == AlertAssignmentStatus.ACCEPTED
                || assignmentStatus == AlertAssignmentStatus.IN_PROGRESS
                || assignmentStatus == AlertAssignmentStatus.RESOLVED) {
            throw new IllegalArgumentException(
                    String.format("Cannot accept alert %s: already in %s state (accepted by %s)",
                            alertId, assignmentStatus, acceptedBy));
        }

        Objects.requireNonNull(personName, "Person name cannot be null");
        if (personName.isBlank()) {
            throw new IllegalArgumentException("Person name cannot be empty");
        }

        this.acceptedBy = personName;
        this.acceptedDate = LocalDateTime.now();
        this.assignmentStatus = AlertAssignmentStatus.ACCEPTED;
        this.assignedTo = personName;
    }

    public void startInvestigation() {
        if (assignmentStatus != AlertAssignmentStatus.ACCEPTED) {
            throw new IllegalArgumentException(
                    String.format("Cannot start investigation: alert must be ACCEPTED, currently %s",
                            assignmentStatus));
        }
        this.assignmentStatus = AlertAssignmentStatus.IN_PROGRESS;
    }

    public void resolve(String notes) {
        if (assignmentStatus != AlertAssignmentStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    String.format("Cannot resolve: alert mus be IN_PROGRESS, currently %s",
                            assignmentStatus));
        }

        Objects.requireNonNull(notes, "Resolution Notes cannot be null");
        if (notes.isBlank()) {
            throw new IllegalArgumentException("Resolution Notes cannot be empty");
        }
        this.assignmentStatus = AlertAssignmentStatus.RESOLVED;
        this.resolutionNotes = notes;
        this.resolvedDate = LocalDateTime.now();
    }

    public void escalate(Department escalateToDepartment) {
        if (assignmentStatus == AlertAssignmentStatus.RESOLVED) {
            throw new IllegalArgumentException(
                    String.format("Cannot escalate resolved alert %s",
                            alertId));
        }

        Objects.requireNonNull(escalateToDepartment, "Escalation Department cannot be null");

        this.assignmentStatus = AlertAssignmentStatus.ESCALATED;
        this.escalatedTo = escalateToDepartment.toString();
        this.assignedDepartment = escalateToDepartment;
    }

    public boolean isOverdue() {
        if (assignmentStatus == AlertAssignmentStatus.RESOLVED) {
            return false;
        }
        return LocalDateTime.now().isAfter(slaDeadline);
    }

    public long getMinutesUntilSlaDeadline() {
        return java.time.temporal.ChronoUnit.MINUTES.between(LocalDateTime.now(), slaDeadline);
    }

    public boolean wasSlaMet() {
        if (assignmentStatus != AlertAssignmentStatus.RESOLVED) {
            return false;
        }
        return resolvedDate.isBefore(slaDeadline) || resolvedDate.isEqual(slaDeadline);
    }

    public long getTimeToOwnershipMinutes() {
        if (acceptedDate == null) {
            return -1;
        }
        return java.time.temporal.ChronoUnit.MINUTES.between(createdDate, acceptedDate);
    }

    private LocalDateTime calculateSlaDeadLine(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> LocalDateTime.now().plusMinutes(15);
            case HIGH -> LocalDateTime.now().plusHours(1);
            case MEDIUM -> LocalDateTime.now().plusDays(1);
            case LOW -> LocalDateTime.now().plusDays(5);
            case INFO -> LocalDateTime.now().plusDays(10);
        };
    }

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