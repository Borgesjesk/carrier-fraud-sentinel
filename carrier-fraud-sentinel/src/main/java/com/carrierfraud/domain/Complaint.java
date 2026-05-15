package com.carrierfraud.domain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Complaint represents a case filed by a transportista (carrier) against non-payment or incident.
 *
 * MUTABLE by design (unlike Transaction):
 * - Complaint has a lifecycle: UNRESOLVED → SOLVED
 * - State transitions are explicit and guarded
 * - Behavior: resolution requires proof (documentation)
 *
 * STATE MACHINE:
 * UNRESOLVED (initial state)
 *   - Complaint filed by transportista
 *   - Waiting for investigation/resolution
 *   - Counted in Rule 3 alerts: 10+ per week = ALERT
 *   - Can transition to SOLVED only with documentation + resolution reason
 *
 * SOLVED (terminal state)
 *   - Investigation complete, case closed
 *   - No longer counted in active complaint metrics
 *   - Resolution reason recorded (RECEIVED_PAYMENT, NOT_RECEIVED, ACCIDENT, etc.)
 *
 * BUSINESS RULES (From Jess's Alpega Operations):
 * - Filed by: Always the transportista (carrier) who didn't receive payment
 * - Required docs: CMR, ALBARAN, ORDEN_DE_CARGA, communication trail, proof of filing
 * - Time windows: 10+ unresolved/week = ALERT, 20+/month = ALERT
 * - Dispute tracking: 3+ unresolved commercial disputes = CRITICAL
 * - Accident tracking: 2+ accidents/month = ALERT
 *
 * DDD INVARIANTS:
 * 1. complaintId: UUID or Long (immutable identifier)
 * 2. carrierName: String (who filed it - always the transportista)
 * 3. complaintType: Enum (OPEN_CASE, ACCIDENT, COMMERCIAL_DISPUTE, INSURANCE, REVIEWING)
 * 4. createdDate: LocalDateTime (when was it filed - immutable)
 * 5. status: ComplaintStatus (UNRESOLVED or SOLVED)
 * 6. resolvedDate: LocalDateTime (null if UNRESOLVED, set when SOLVED)
 * 7. resolutionReason: ResolutionReason (null if UNRESOLVED, required when SOLVED)
 * 8. Cannot transition to SOLVED without documentation + reason
 *
 * Why Mutable?
 * - Complaint has behavior (resolve with documentation)
 * - State transitions are part of the domain logic
 * - We need to track status changes over time
 * - Unlike Transaction (snapshot of data), Complaint is a process
 */
public class Complaint {

    // ============ IMMUTABLE FIELDS (Identity & Creation) ============

    private final String complaintId;
    private final String carrierName;
    private final ComplaintType complaintType;
    private final LocalDateTime createdDate;

    // ============ MUTABLE FIELDS (State & Transitions) ============

    private ComplaintStatus status;
    private LocalDateTime resolvedDate;
    private ResolutionReason resolutionReason;
    private String resolutionNotes;

    // ============ CONSTRUCTOR ============

    /**
     * Create a new complaint in UNRESOLVED state.
     *
     * @param complaintId unique identifier (UUID or Long)
     * @param carrierName transportista who filed the complaint
     * @param complaintType type of complaint (OPEN_CASE, ACCIDENT, etc.)
     *
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public Complaint(String complaintId, String carrierName, ComplaintType complaintType) {
        // VALIDATION: complaintId
        Objects.requireNonNull(complaintId, "complaintId cannot be null");
        if (complaintId.isBlank()) {
            throw new IllegalArgumentException("complaintId cannot be empty");
        }
        this.complaintId = complaintId;

        // VALIDATION: carrierName
        Objects.requireNonNull(carrierName, "carrierName cannot be null");
        if (carrierName.isBlank()) {
            throw new IllegalArgumentException("carrierName cannot be empty");
        }
        if (carrierName.length() > 50) {
            throw new IllegalArgumentException(
                String.format("carrierName too long: %d chars (max 50)", carrierName.length())
            );
        }
        this.carrierName = carrierName;

        // VALIDATION: complaintType
        Objects.requireNonNull(complaintType, "complaintType cannot be null");
        this.complaintType = complaintType;

        // INITIALIZATION: State at creation
        this.createdDate = LocalDateTime.now();
        this.status = ComplaintStatus.UNRESOLVED;
        this.resolvedDate = null;
        this.resolutionReason = null;
        this.resolutionNotes = null;
    }

    // ============ GETTERS (Immutable Fields) ============

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

    // ============ GETTERS (Mutable State) ============

    public ComplaintStatus getStatus() {
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

    // ============ STATE TRANSITIONS (Behavior) ============

    /**
     * Resolve this complaint with a reason and documentation.
     *
     * INVARIANT ENFORCEMENT:
     * - Can only be called when status = UNRESOLVED
     * - Requires a valid resolution reason
     * - Sets resolvedDate to now
     * - Transitions status to SOLVED
     *
     * Business Logic:
     * - COMMERCIAL_DISPUTE resolutions count toward Rule 3 CRITICAL threshold
     * - ACCIDENT resolutions count toward Rule 3 (2+ accidents/month)
     * - NOT_RECEIVED resolutions escalate to legal team
     *
     * @param reason how the complaint was resolved (RECEIVED_PAYMENT, NOT_RECEIVED, etc.)
     * @param notes investigation findings or resolution details
     *
     * @throws IllegalStateException if already SOLVED
     * @throws IllegalArgumentException if reason is null
     */
    public void resolve(ResolutionReason reason, String notes) {
        // GUARD CLAUSE: State machine invariant
        if (status == ComplaintStatus.SOLVED) {
            throw new IllegalStateException(
                String.format(
                    "Cannot resolve complaint %s: already SOLVED at %s",
                    complaintId,
                    resolvedDate
                )
            );
        }

        // GUARD CLAUSE: Resolution reason required
        Objects.requireNonNull(reason, "resolution reason cannot be null");

        // STATE TRANSITION
        this.status = ComplaintStatus.SOLVED;
        this.resolutionDate = LocalDateTime.now();
        this.resolutionReason = reason;
        this.resolutionNotes = notes != null ? notes : "";
    }

    /**
     * Reopen a resolved complaint (escalation scenario).
     * For cases that were resolved incorrectly or require further investigation.
     *
     * Business use case:
     * - Transportista disputes the resolution
     * - Investigation reveals new evidence
     * - Legal escalation requires reopening
     *
     * @throws IllegalStateException if complaint is UNRESOLVED (already open)
     */
    public void reopen() {
        if (status == ComplaintStatus.UNRESOLVED) {
            throw new IllegalStateException(
                String.format(
                    "Cannot reopen complaint %s: already UNRESOLVED (never was SOLVED)",
                    complaintId
                )
            );
        }

        this.status = ComplaintStatus.UNRESOLVED;
        this.resolvedDate = null;
        this.resolutionReason = null;
        this.resolutionNotes = resolutionNotes + " [REOPENED at " + LocalDateTime.now() + "]";
    }

    // ============ COMPUTED PROPERTIES (Business Intelligence) ============

    /**
     * How many days has this complaint been open?
     * Used by Rule 3 for time window calculations.
     *
     * If UNRESOLVED: days since created until now
     * If SOLVED: days from created to resolved
     *
     * @return number of days open
     */
    public int getDaysOpen() {
        LocalDateTime endDate = (status == ComplaintStatus.UNRESOLVED)
            ? LocalDateTime.now()
            : resolvedDate;

        return (int) ChronoUnit.DAYS.between(createdDate, endDate);
    }

    /**
     * Is this complaint a commercial dispute that was never resolved?
     * Critical for Rule 3: 3+ unresolved disputes = CRITICAL alert.
     *
     * @return true if type=COMMERCIAL_DISPUTE and status=UNRESOLVED
     */
    public boolean isUnresolvedDispute() {
        return complaintType == ComplaintType.COMMERCIAL_DISPUTE
            && status == ComplaintStatus.UNRESOLVED;
    }

    /**
     * Is this complaint an accident within the time window?
     * Used by Rule 3: 2+ accidents/month = ALERT.
     *
     * @return true if type=ACCIDENT
     */
    public boolean isAccident() {
        return complaintType == ComplaintType.ACCIDENT;
    }

    /**
     * Is this complaint in an active state (requiring attention)?
     *
     * @return true if status = UNRESOLVED
     */
    public boolean isActive() {
        return status == ComplaintStatus.UNRESOLVED;
    }

    // ============ OBJECT CONTRACT ============

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Complaint other = (Complaint) obj;
        return complaintId.equals(other.complaintId)
            && carrierName.equals(other.carrierName)
            && complaintType == other.complaintType
            && createdDate.equals(other.createdDate)
            && status == other.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(complaintId, carrierName, complaintType, createdDate, status);
    }

    @Override
    public String toString() {
        return String.format(
            "Complaint{id=%s, carrier=%s, type=%s, status=%s, created=%s, daysOpen=%d}",
            complaintId,
            carrierName,
            complaintType,
            status,
            createdDate,
            getDaysOpen()
        );
    }
}
