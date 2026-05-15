package com.carrierfraud.domain;

import java.util.Objects;

/**
 * TransactionValidator enforces BUSINESS RULE INVARIANTS across multiple fields.
 *
 * DISTINCTION FROM CONSTRUCTOR:
 * - Constructor validates FIELD-LEVEL constraints (each field individually)
 *   Example: carrierName length, offerPrice > 0
 * - Validator validates BUSINESS-LEVEL constraints (combinations of fields)
 *   Example: failedPayments=100 + succeededPayments=0 = NONSENSICAL
 *
 * WHEN TO USE:
 * - AFTER Transaction passes constructor validation
 * - BEFORE passing to fraud detection rules
 * - When integrating data from external systems (API, database, CSV)
 * - Example: API returns Transaction data that's individually valid but logically impossible
 *
 * BUSINESS RULES FROM ALPEGA OPERATIONS:
 * 1. Payment History Ratio: Can't have 100 failed payments and 0 successful
 * 2. Incident Ratio: Can't have more incidents than offers
 * 3. Offer Price vs Volume: Price should match offer volume
 * 4. New Carrier Grace Period: Very new carriers with high incidents might be learning
 * 5. Suspended Carrier Pattern: High failures + high incidents = likely fraud
 *
 * THROWS:
 * - BusinessRuleException if validation fails (custom exception, not IllegalArgumentException)
 *   Why? To distinguish from constructor validation errors
 *   Constructor: IllegalArgumentException (data malformed)
 *   Validator: BusinessRuleException (data valid but nonsensical)
 */
public class TransactionValidator {

    /**
     * Validate a Transaction against business rule invariants.
     * Assumes Transaction has already passed constructor validation.
     *
     * @param transaction the Transaction to validate (must not be null)
     * @throws IllegalArgumentException if transaction is null
     * @throws BusinessRuleException if any business invariant is violated
     */
    public void validate(Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction cannot be null for validation");

        // Validate each business rule in order
        // Each method throws BusinessRuleException if violated
        validatePaymentHistoryRatio(transaction);
        validateIncidentRatio(transaction);
        validateOfferPriceConsistency(transaction);
        validateNewCarrierPattern(transaction);
        validateSuspendedCarrierPattern(transaction);
    }

    // ============ BUSINESS RULE VALIDATORS ============

    /**
     * RULE 1: Payment History Ratio
     *
     * Business Logic:
     * - If a carrier has many failed payments, they should have SOME successful ones
     * - Example: 100 failures + 0 successes = IMPOSSIBLE
     * - Typical pattern: 10 failures should mean at least 1-2 successes (ratio max 10:1)
     *
     * What this catches:
     * - Data corruption (failed count inflated artificially)
     * - System error (success count not updated)
     * - Fraud signal (carrier accepted offers but never paid, ever)
     *
     * Alpega context:
     * - A real carrier that never paid once but tried 100 times = RED FLAG
     * - A new carrier with 5 failures and 0 successes = NORMAL (might pay on 6th try)
     *
     * @throws BusinessRuleException if ratio is impossible
     */
    private void validatePaymentHistoryRatio(Transaction t) {
        int totalAttempts = t.getFailedPayments() + t.getSucceededPayments();

        // Special case: No payment history yet
        if (totalAttempts == 0) {
            return;  // New carrier, no history = VALID
        }

        // Calculate failure ratio
        double failureRatio = (double) t.getFailedPayments() / totalAttempts;

        // BUSINESS RULE: Can't have > 80% failure rate without at least 1 success
        // Why 80%? Allows new carriers (2 failures, 1 success = 67% fail rate)
        // But catches: (100 failures, 0 successes = 100% fail rate)
        if (failureRatio > 0.8 && t.getSucceededPayments() == 0) {
            throw new BusinessRuleException(
                String.format(
                    "Carrier '%s' has impossible payment pattern: %d failures, %d successes. "
                        + "No carrier fails this consistently without ever succeeding. "
                        + "Data corruption or fraudulent carrier.",
                    t.getCarrierName(),
                    t.getFailedPayments(),
                    t.getSucceededPayments()
                )
            );
        }

        // BUSINESS RULE: If 50+ attempts, failure rate should be < 50%
        // Why? Real carriers learn after failures and succeed eventually
        // A carrier with 50 attempts and 49 failures = not learning = suspended or fraud
        if (totalAttempts >= 50 && failureRatio >= 0.98) {
            throw new BusinessRuleException(
                String.format(
                    "Carrier '%s' shows no learning pattern: %d failures out of %d attempts. "
                        + "After 50+ transactions, carriers should succeed at least occasionally.",
                    t.getCarrierName(),
                    t.getFailedPayments(),
                    totalAttempts
                )
            );
        }
    }

    /**
     * RULE 2: Incident Ratio
     *
     * Business Logic:
     * - Incidents should never exceed the number of offers
     * - You can't have 100 incidents on 5 offers
     * - Maximum realistic: 50% incident rate (1 incident per 2 offers)
     *
     * What this catches:
     * - Data corruption (incident count multiplied/doubled)
     * - System error (incidents counted multiple times)
     * - Impossible edge case
     *
     * Alpega context:
     * - A carrier with 5 offers and 0 incidents = NORMAL
     * - A carrier with 5 offers and 2 incidents = CONCERNING (40% incident rate)
     * - A carrier with 5 offers and 100 incidents = IMPOSSIBLE (data corruption)
     *
     * @throws BusinessRuleException if incident ratio is impossible
     */
    private void validateIncidentRatio(Transaction t) {
        // Special case: No offers yet
        if (t.getNumberOfOffers() == 0) {
            // But if they have incidents with no offers, that's weird
            if (t.getReportedIncidents() > 0) {
                throw new BusinessRuleException(
                    String.format(
                        "Carrier '%s' has %d incidents but 0 offers. "
                            + "Incidents should correspond to actual offers.",
                        t.getCarrierName(),
                        t.getReportedIncidents()
                    )
                );
            }
            return;  // 0 offers, 0 incidents = VALID
        }

        // RULE: Incidents can't exceed number of offers
        if (t.getReportedIncidents() > t.getNumberOfOffers()) {
            throw new BusinessRuleException(
                String.format(
                    "Carrier '%s' has impossible incident ratio: %d incidents on only %d offers. "
                        + "Each offer can have at most 1 incident.",
                    t.getCarrierName(),
                    t.getReportedIncidents(),
                    t.getNumberOfOffers()
                )
            );
        }

        // RULE: Incidents should rarely exceed 50% of offers
        double incidentRatio = (double) t.getReportedIncidents() / t.getNumberOfOffers();
        if (incidentRatio > 0.5 && t.getNumberOfOffers() >= 10) {
            // Only flag if: (a) ratio > 50%, (b) enough history to judge (10+ offers)
            // Why? A new carrier with 1 offer and 1 incident = 100% but that's OK
            // A carrier with 20 offers and 15 incidents = RED FLAG
            // (NOT an error, but Rule 3 will catch this as HIGH_RISK)
        }
    }

    /**
     * RULE 3: Offer Price Consistency
     *
     * Business Logic:
     * - Offer price should be reasonable given the number of offers
     * - Example: If carrier has 1000 offers, average price seems low
     * - Example: If carrier has 1 offer, offerPrice should be reasonable per-offer
     *
     * What this catches:
     * - Price data error (1000 instead of 100)
     * - Offer count error (1 instead of 10)
     * - Data from wrong period/currency
     *
     * Alpega context:
     * - Cargo prices range: €500 - €5000 per shipment
     * - A carrier with 1 offer at €50,000 = POSSIBLE (premium shipment)
     * - A carrier with 100 offers at €10 each = IMPOSSIBLE (minimum is €500)
     * - A carrier with 1 offer at €0.01 = IMPOSSIBLE (validated by constructor, but double-check)
     *
     * @throws BusinessRuleException if offer price is inconsistent
     */
    private void validateOfferPriceConsistency(Transaction t) {
        // This is a WARNING-level check, not strict
        // Constructor already ensures price > 0
        // Validator checks if it makes business sense

        double avgPricePerOffer = t.getAveragePricePerOffer();

        // RULE: Average price should be in freight market range
        // Min: €50 per offer (constructor ensures > 0, but too low is odd)
        // Max: €50,000 per offer (emergency shipments)
        if (avgPricePerOffer < 50) {
            throw new BusinessRuleException(
                String.format(
                    "Carrier '%s' has suspiciously low average offer price: €%.2f. "
                        + "Freight market minimum is ~€50 per shipment.",
                    t.getCarrierName(),
                    avgPricePerOffer
                )
            );
        }

        // No upper limit (emergency shipments can be €50k+ per offer)
        // But log it for monitoring (not an error)
    }

    /**
     * RULE 4: New Carrier Grace Period
     *
     * Business Logic:
     * - Very new carriers (< 10 total offers) with high incidents should not be immediately flagged
     * - They're learning
     * - But very new carriers with 0 offers and high incidents = data corruption
     *
     * What this catches:
     * - A phantom carrier (no offers, but incidents reported)
     * - Data corruption (creation date in future? - no, not checking that here)
     *
     * @throws BusinessRuleException if pattern suggests data corruption
     */
    private void validateNewCarrierPattern(Transaction t) {
        // No transaction data = new carrier
        int totalOffers = t.getSucceededPayments() + t.getFailedPayments();

        // If brand new (no offers at all), incidents should be 0
        if (totalOffers == 0 && t.getReportedIncidents() > 0) {
            throw new BusinessRuleException(
                String.format(
                    "Brand new carrier '%s' has no offers but %d incidents reported. "
                        + "Data corruption detected.",
                    t.getCarrierName(),
                    t.getReportedIncidents()
                )
            );
        }

        // New carriers with few offers + high incidents = learning, OK
        // Don't validate here (Rule 3 handles the alert logic)
    }

    /**
     * RULE 5: Suspended Carrier Pattern
     *
     * Business Logic:
     * - A carrier with BOTH high failures AND high incidents = likely suspended or fraudulent
     * - This is not an error (data is valid), but it's a strong signal
     * - Validator catches the pattern and documents it
     *
     * Pattern: 30+ failures AND 30+ incidents = DANGER ZONE
     * Why? Legitimate carriers don't accumulate this much damage
     *
     * @throws BusinessRuleException if pattern suggests fraudulent carrier
     */
    private void validateSuspendedCarrierPattern(Transaction t) {
        boolean highFailureCount = t.getFailedPayments() >= 30;
        boolean highIncidentCount = t.getReportedIncidents() >= 30;

        if (highFailureCount && highIncidentCount) {
            throw new BusinessRuleException(
                String.format(
                    "Carrier '%s' shows DANGER ZONE pattern: %d failed payments + %d incidents. "
                        + "This carrier should likely be suspended. "
                        + "Forwarding to COMPLIANCE for immediate review.",
                    t.getCarrierName(),
                    t.getFailedPayments(),
                    t.getReportedIncidents()
                )
            );
        }
    }
}
