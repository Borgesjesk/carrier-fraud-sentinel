package com.carrierfraud.domain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class Complaint {

    private final String complaintId;
    private final String carrierName;
    private final ComplaintType complaintType;
    private final LocalDateTime createdDate;

    private ComplainStatus status;
    private LocalDateTime resolvedDate;
    private ResolutionReason resolutionReason;
    private String resolutionNotes;

    public Complaint(String complaintId, String carrierName, ComplaintType complaintType) {

        Objects.requireNonNull(complaintId, "Complaint ID cannot be null");
        if (complaintId.isBlank()) {
            throw new IllegalArgumentException("Complaint ID cannot be empty");
        }
        this.complaintId = complaintId;

        Objects.requireNonNull(carrierName, "Carrier name cannot be null");
        if (carrierName.isBlank()) {
            throw new IllegalArgumentException("Carrier name cannot be empty");
        }
        if (carrierName.length() > 50) {
            throw new IllegalArgumentException(
                    String.format("Carrier name too long: %d chars (max 50)", carrierName.length()));
        }
        this.carrierName = carrierName;

        Objects.requireNonNull(complaintType, "ComplaintType cannot be null");
        this.complaintType = complaintType;

        this.createdDate = LocalDateTime.now();
        this.status = ComplainStatus.UNRESOLVED;
        this.resolvedDate = null;
        this.resolutionReason = null;
        this.resolutionNotes = null;
    }

    public String getComplaintId() {
        return complaintId;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public ComplaintType getComplaintType() {
        return complaintType;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public ComplainStatus getStatus() {
        return status;
    }

    public LocalDateTime getResolvedDate() {
        return resolvedDate;
    }

    public ResolutionReason getResolutionReason() {
        return resolutionReason;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void resolve(ResolutionReason reason, String notes) {
        if (status == ComplainStatus.RESOLVED) {
            throw new IllegalStateException(
                    String.format("Cannot resolve complaint %s: already RESOLVED at %s",
                            complaintId,
                            resolvedDate
                    )
            );
        }

        Objects.requireNonNull(reason, "Resolution reason cannot be null");

        this.status = ComplainStatus.RESOLVED;
        this.resolvedDate = LocalDateTime.now();
        this.resolutionReason = reason;
        this.resolutionNotes = notes != null ? notes : "";
    }

    public void reopen() {
        if (status == ComplainStatus.UNRESOLVED) {
            throw new IllegalStateException(
                    String.format("Cannot reopen complaint %s: already UNRESOLVED (check why)",
                            complaintId));
        }
        status = ComplainStatus.UNRESOLVED;
        this.resolvedDate = null;
        this.resolutionReason = null;
        this.resolutionNotes = resolutionNotes + " [REOPENED at " + LocalDateTime.now() + "]";
    }

    public int getDaysOpen() {
        LocalDateTime endDate = (status == ComplainStatus.UNRESOLVED)
                ? LocalDateTime.now() : resolvedDate;

        return (int) ChronoUnit.DAYS.between(createdDate, endDate);
    }

    public boolean isAccident() {
        return complaintType == ComplaintType.ACCIDENT;
    }

    public boolean isActive() {
        return status == ComplainStatus.UNRESOLVED;
    }

    @Override
    public boolean equals(Object ojb) {
        if (this == ojb) return true;
        if (ojb == null || getClass() != ojb.getClass()) return false;

        Complaint other = (Complaint) ojb;
        return complaintId.equals(other.complaintId)
                && carrierName.equals(other.carrierName)
                && complaintType == other.complaintType
                && createdDate.equals(other.createdDate)
                && status == other.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                complaintId, carrierName, complaintType, createdDate, status
        );
    }

    @Override
    public String toString() {
        return String.format(
                "Complaint{id=%s, carrier=%s, type=%s, status=%s, created=%s, dayOpen=%d}",
                complaintId,
                carrierName,
                complaintType,
                status,
                createdDate,
                getDaysOpen()
        );
    }
}