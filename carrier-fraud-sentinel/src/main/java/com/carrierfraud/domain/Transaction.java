package com.carrierfraud.domain;

import java.util.Objects;

/**
 * Transaction represents a single transport/freight offer on the WTRANSNET platform.
 *
 * IMMUTABLE by design:
 * - All fields are final
 * - No setters (only getters)
 * - Thread-safe (concurrent rule evaluation)
 * - Non-repudiation: "This alert fired because Transaction state was X at time T"
 *
 * SELF-VALIDATING:
 * - Validation happens in constructor (fail-fast principle)
 * - Invalid data rejected immediately, before reaching fraud rules
 * - No garbage alerts on corrupted data
 *
 * Domain Invariants (Business Rules from Alpega Operations):
 * - carrierName: required, 1-50 chars (valid WTRANSNET carrier identifier)
 * - transportName: required, 1-50 chars (identifies specific transport/shipment)
 * - failedPayments: >= 0 (counts payment failures for this carrier)
 * - succeededPayments: >= 0 (counts successful payments; <10/month = SALES_ALERT, >100 = UPSELL)
 * - offerPrice: > 0 (freight prices never zero or negative; no ceiling)
 * - numberOfOffers: >= 0 (no validation ceiling; business logic in rules)
 * - reportedIncidents: >= 0, <= 50 (>50 triggers COMPLIANCE_REVIEW)
 *
 * Why Immutable?
 * 1. Thread Safety: Multiple rules evaluate same transaction concurrently
 * 2. Security: Audit trail - what was the state when alert fired?
 * 3. Side-Effect Free: Fraud scoring is deterministic (same input = same output)
 * 4. Testability: Easier to reason about state
 *
 * Why Validate in Constructor?
 * - Fail-fast: Bad data rejected immediately
 * - No "garbage in, garbage out" alerts
 * - Cleaner code downstream (no null checks in rules)
 * - Domain invariants are enforced at object creation
 */
public final class Transaction {

    private final String carrierName;
    private final String transportName;
    private final int failedPayments;
    private final int succeededPayments;
    private final double offerPrice;
    private final int numberOfOffers;
    private final int reportedIncidents;

    /**
     * Constructor with validation.
     * Throws IllegalArgumentException immediately if any domain invariant is violated.
     *
     * @param carrierName identifier of the carrier (required, 1-50 chars)
     * @param transportName identifier of the transport (required, 1-50 chars)
     * @param failedPayments count of failed payments (>= 0)
     * @param succeededPayments count of successful payments (>= 0)
     * @param offerPrice freight price (> 0, no ceiling)
     * @param numberOfOffers number of active offers (>= 0, no ceiling)
     * @param reportedIncidents reported incidents against carrier (>= 0, <= 50 triggers review)
     *
     * @throws IllegalArgumentException if any invariant violated
     */
    public Transaction(
            String carrierName,
            String transportName,
            int failedPayments,
            int succeededPayments,
            double offerPrice,
            int numberOfOffers,
            int reportedIncidents
    ) {
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

        // VALIDATION: transportName
        Objects.requireNonNull(transportName, "transportName cannot be null");
        if (transportName.isBlank()) {
            throw new IllegalArgumentException("transportName cannot be empty");
        }
        if (transportName.length() > 50) {
            throw new IllegalArgumentException(
                String.format("transportName too long: %d chars (max 50)", transportName.length())
            );
        }
        this.transportName = transportName;

        // VALIDATION: failedPayments
        if (failedPayments < 0) {
            throw new IllegalArgumentException(
                String.format("failedPayments cannot be negative: %d", failedPayments)
            );
        }
        this.failedPayments = failedPayments;

        // VALIDATION: succeededPayments
        if (succeededPayments < 0) {
            throw new IllegalArgumentException(
                String.format("succeededPayments cannot be negative: %d", succeededPayments)
            );
        }
        this.succeededPayments = succeededPayments;

        // VALIDATION: offerPrice
        // Must be strictly positive (> 0, not >= 0)
        // Freight prices never zero or negative
        if (offerPrice <= 0) {
            throw new IllegalArgumentException(
                String.format("offerPrice must be positive: %.2f", offerPrice)
            );
        }
        // No upper limit (emergency shipments = premium prices)
        this.offerPrice = offerPrice;

        // VALIDATION: numberOfOffers
        // No validation ceiling (business logic is in fraud rules, not validation)
        if (numberOfOffers < 0) {
            throw new IllegalArgumentException(
                String.format("numberOfOffers cannot be negative: %d", numberOfOffers)
            );
        }
        this.numberOfOffers = numberOfOffers;

        // VALIDATION: reportedIncidents
        if (reportedIncidents < 0) {
            throw new IllegalArgumentException(
                String.format("reportedIncidents cannot be negative: %d", reportedIncidents)
            );
        }
        // Alert trigger at 50 (compliance review), but no hard ceiling in validation
        // Why? New carriers might have high incident counts while learning
        // Evaluation is done by COMPLIANCE_REVIEW department, not automatically rejected
        this.reportedIncidents = reportedIncidents;
    }

    // ============ GETTERS (immutable, no setters) ============

    public String getCarrierName() {
        return carrierName;
    }

    public String getTransportName() {
        return transportName;
    }

    public int getFailedPayments() {
        return failedPayments;
    }

    public int getSucceededPayments() {
        return succeededPayments;
    }

    public double getOfferPrice() {
        return offerPrice;
    }

    public int getNumberOfOffers() {
        return numberOfOffers;
    }

    public int getReportedIncidents() {
        return reportedIncidents;
    }

    // ============ COMPUTED PROPERTIES (derived from state) ============

    /**
     * Payment success rate: what % of offers resulted in payment?
     * Used by Rule 1 (Payment Reconciliation Rule) to detect unpaid carriers.
     *
     * @return success rate (0.0 to 1.0), or 0.0 if no offers yet
     */
    public double getPaymentSuccessRate() {
        int totalOffers = failedPayments + succeededPayments;
        if (totalOffers == 0) {
            return 0.0;
        }
        return (double) succeededPayments / totalOffers;
    }

    /**
     * Average price per offer.
     * Used by Rule 2 (Price Escalation Rule) as baseline for comparison.
     *
     * @return average offer price, or 0.0 if no offers
     */
    public double getAveragePricePerOffer() {
        if (numberOfOffers == 0) {
            return 0.0;
        }
        return offerPrice / numberOfOffers;
    }

    /**
     * Incident severity ratio: what % of offers had incident?
     * Used by Rule 3 (Complaint Accumulation Rule) as context.
     *
     * @return ratio (0.0 to 1.0), or 0.0 if no incidents
     */
    public double getIncidentRatio() {
        if (numberOfOffers == 0) {
            return 0.0;
        }
        return (double) reportedIncidents / numberOfOffers;
    }

    // ============ OBJECT CONTRACT (equals, hashCode, toString) ============

    /**
     * Two transactions are equal if all their fields match.
     * Used by tests and collections.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Transaction other = (Transaction) obj;
        return failedPayments == other.failedPayments
            && succeededPayments == other.succeededPayments
            && Double.compare(offerPrice, other.offerPrice) == 0
            && numberOfOffers == other.numberOfOffers
            && reportedIncidents == other.reportedIncidents
            && carrierName.equals(other.carrierName)
            && transportName.equals(other.transportName);
    }

    /**
     * Consistent hash code (required when overriding equals).
     */
    @Override
    public int hashCode() {
        return Objects.hash(
            carrierName,
            transportName,
            failedPayments,
            succeededPayments,
            offerPrice,
            numberOfOffers,
            reportedIncidents
        );
    }

    /**
     * Human-readable representation for debugging.
     */
    @Override
    public String toString() {
        return String.format(
            "Transaction{carrier=%s, transport=%s, failed=%d, succeeded=%d, price=%.2f, offers=%d, incidents=%d}",
            carrierName,
            transportName,
            failedPayments,
            succeededPayments,
            offerPrice,
            numberOfOffers,
            reportedIncidents
        );
    }
}
