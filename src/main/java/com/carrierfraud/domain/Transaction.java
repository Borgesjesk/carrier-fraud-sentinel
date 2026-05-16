package com.carrierfraud.domain;

import java.util.Objects;

public final class Transaction {

    private final String carrierName;
    private final String transportName;
    private final int failedPayments;
    private final int succeededPayments;
    private final double offerPrice;
    private final int numberOfOffers;
    private final int reportedIncidents;

    public Transaction(
            String carrierName,
            String transportName,
            int failedPayments,
            int succeededPayments,
            double offerPrice,
            int numberOfOffers,
            int reportedIncidents
    ) {

        Objects.requireNonNull(carrierName, "Carrier name cannot be null");
        if (carrierName.isBlank()) {
            throw new IllegalArgumentException("Carrier name cannot be empty");
        }
        if (carrierName.length() > 50) {
            throw new IllegalArgumentException(
                    String.format("Carrier name too long: %d chars (max 50)", carrierName.length()));
        }
        this.carrierName = carrierName;

        Objects.requireNonNull(transportName, "Transport name cannot be null");
        if (transportName.isBlank()) {
            throw new IllegalArgumentException("Transport name cannot be empty");
        }
        if (transportName.length() > 50) {
            throw new IllegalArgumentException(
                    String.format("Transport name too long: %d chars (max 50)", transportName.length()));
        }
        this.transportName = transportName;

        if (failedPayments < 0) {
            throw new IllegalArgumentException(
                    String.format("Failed payments cannot be negative: %d", failedPayments));
        }
        this.failedPayments = failedPayments;

        if (succeededPayments < 0) {
            throw new IllegalArgumentException(
                    String.format("Succeeded payments cannot be negative: %d", succeededPayments));
        }
        this.succeededPayments = succeededPayments;

        if (offerPrice <= 0) {
            throw new IllegalArgumentException(
                    String.format("Offer price must be positive: %.2f", offerPrice));
        }
        this.offerPrice = offerPrice;

        if (numberOfOffers < 0) {
            throw new IllegalArgumentException(
                    String.format("Number of offers cannot be negative: %d", numberOfOffers));
        }
        this.numberOfOffers = numberOfOffers;

        if (reportedIncidents < 0) {
            throw new IllegalArgumentException(
                    String.format("Reported incidents cannot be negative: %d", reportedIncidents));
        }
        this.reportedIncidents = reportedIncidents;

    }

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

    public double getPaymentSuccessRate() {
        int totalOffers = failedPayments + succeededPayments;
        if (totalOffers == 0) {
            return 0.0;
        }
        return (double) succeededPayments / totalOffers;
    }

    public double getAveragePricePerOffer() {
        if (numberOfOffers == 0) {
            return 0.0;
        }
        return offerPrice / numberOfOffers;
    }

    public double getIncidentRatio() {
        if (numberOfOffers == 0) {
            return 0.0;
        }
        return (double) reportedIncidents / numberOfOffers;
    }

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

    @Override
    public String toString() {
        return String.format(
                "Transaction{carrier=%s, transport=%s, failed=%d, succeeded=%d, price=%.2f, offer=%d, incidents=%d, explanation=%s, requiredDocuments=%s}",
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