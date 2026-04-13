package com.carrierfraud.infrastructure;

public record TransactionDTO(

        String carrierName,
        String transportName,
        int failedPayments,
        int succeededPayments,
        double offerPrice,
        int numberOfOffers,
        String incidentExplanation,
        boolean missingData
) {
}

