package com.carrierfraud.domain;

public class Transaction {

    private final String carrierName;
    private final String transportName;
    private final int failedPayments;
    private final int succeededPayments;
    private final double offerPrice;
    private final int numberOfOffers;
    private final String incidentExplanation;
    private final int reviewScore;
    private final int reportedIncidents;
    private final boolean missingData;

    public Transaction(String carrierName, String transportName, int failedPayments, int succeededPayments, double offerPrice, int numberOfOffers, String incidentExplanation, int reviewScore, int reportedIncidents, boolean missingData) {
        this.carrierName = carrierName;
        this.transportName = transportName;
        this.failedPayments = failedPayments;
        this.succeededPayments = succeededPayments;
        this.offerPrice = offerPrice;
        this.numberOfOffers = numberOfOffers;
        this.incidentExplanation = incidentExplanation;
        this.reviewScore = reviewScore;
        this.reportedIncidents = reportedIncidents;
        this.missingData = missingData;
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

    public String getIncidentExplanation() {
        return incidentExplanation;
    }

    public int getReviewScore() {
        return reviewScore;
    }

    public int getReportedIncidents() {
        return reportedIncidents;
    }

    public boolean isMissingData() {
        return missingData;
    }
}
