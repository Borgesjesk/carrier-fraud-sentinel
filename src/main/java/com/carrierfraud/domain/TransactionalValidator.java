package com.carrierfraud.domain;

import java.util.Objects;

public class TransactionalValidator {

    public void validate(Transaction transaction) {
        Objects.requireNonNull(transaction, "Transaction cannot be null for validation");

        validatePaymentHistoryRatio(transaction);
        validateIncidentRatio(transaction);
        validateOfferPriceConsistency(transaction);
        validateNewCarrierPattern(transaction);
        validateSuspendedCarrierPattern(transaction);
    }

    private void validatePaymentHistoryRatio(Transaction t) {
        int totalAttempts = t.getFailedPayments() + t.getSucceededPayments();

        if (totalAttempts == 0) {
            return;
        }

        double failureRatio = (double) t.getFailedPayments() / totalAttempts;

        if (failureRatio > 0.8 && t.getSucceededPayments() == 0) {
            throw new BusinessRuleException(
                    String.format(
                            "Carrier '%s' has impossible payment pattern: %d failures, %d successes. "
                                    + "No carrier fails this consistently without ever succeeding. "
                                    + "Data corruption or fraudulent carrier.",
                            t.getCarrierName(),
                            t.getFailedPayments(),
                            t.getSucceededPayments()));
        }

        if (totalAttempts >= 50 && failureRatio >= 0.9) {
            throw new BusinessRuleException(
                    String.format(
                            "Carrier '%s' shows no learning pattern: %d failures out of %d attempts. "
                                    + "After 50+ transactions, carriers should succeed at least occasionally.",
                            t.getCarrierName(),
                            t.getFailedPayments(),
                            totalAttempts));
        }
    }

    private void validateIncidentRatio(Transaction t) {
        if (t.getNumberOfOffers() == 0) {

            if (t.getReportedIncidents() > 0) {
                throw new BusinessRuleException(
                        String.format(
                                "Carrier '%s' has %d incidents but 0 offers. "
                                        + "Incidents should correspond to actual offers.",
                                t.getCarrierName(),
                                t.getReportedIncidents()));
            }
            return;
        }

        if (t.getReportedIncidents() > t.getNumberOfOffers()) {
            throw new BusinessRuleException(
                    String.format(
                            "Carrier '%s' has impossible ratio: %d incidents on only %d offers. "
                                    + "Each offer can have at most 1 incident.",
                            t.getCarrierName(),
                            t.getReportedIncidents(),
                            t.getNumberOfOffers()));
        }

        double incidentRatio = (double) t.getReportedIncidents() / t.getNumberOfOffers();
        if (incidentRatio > 0.5 && t.getNumberOfOffers() >= 10) {

        }
    }

    private void validateOfferPriceConsistency(Transaction t) {
        if (t.getNumberOfOffers() == 0) {
            return; // New carrier with no offers — nothing to validate
        }

        double avgPricePerOffer = t.getAveragePricePerOffer();

        if (avgPricePerOffer < 50) {
            throw new BusinessRuleException(
                    String.format(
                            "Carrier '%s' has suspiciously low average offer price: €%.2f. "
                                    + "Freight market minimum is ~€50 per shipment.",
                            t.getCarrierName(),
                            avgPricePerOffer));
        }
    }

    private void validateNewCarrierPattern(Transaction t) {

        int totalOffers = t.getSucceededPayments() + t.getFailedPayments();

        if (totalOffers == 0 && t.getReportedIncidents() > 0) {
            throw new BusinessRuleException(
                    String.format(
                            "Brand new carrier '%s' has no offers but %d incidents reported. "
                                    + "Data corruption detected.",
                            t.getCarrierName(),
                            t.getReportedIncidents()));

        }
    }

    private void validateSuspendedCarrierPattern(Transaction t) {
        boolean highFailureCount = t.getFailedPayments() >= 30;
        boolean highIncidentCount = t.getReportedIncidents() >= 30;

        if (highFailureCount && highIncidentCount) {
            throw new BusinessRuleException(
                    String.format(
                            "Carrier '%s' show DANGER ZONE pattern: %d failed payment + %d incidents. "
                                    + "This carrier should likely be suspended. "
                                    + "Forwarding to COMPLIANCE for immediate review.",
                            t.getCarrierName(),
                            t.getFailedPayments(),
                            t.getReportedIncidents()));
        }
    }
}