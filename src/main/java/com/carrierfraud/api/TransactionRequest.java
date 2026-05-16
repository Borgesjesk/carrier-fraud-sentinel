package com.carrierfraud.api;

import com.carrierfraud.domain.Transaction;
import jakarta.validation.constraints.*;


public record TransactionRequest(
        @NotBlank(message = "Carrier name is required")
        @Size(min = 1, max = 50, message = "Carrier name must be 1-50 characters")
        String carrierName,

        @NotBlank(message = "Transport name is required")
        @Size(min = 1, max = 50, message = "Transport name must be 1-50 characters")
        String transportName,

        @Min(value = 0, message = "Failed payments cannot be negative")
        int failedPayments,

        @Min(value = 0, message = "Succeeded payments cannot be negative")
        int succeededPayments,

        @DecimalMin(value = "0.01", message = "Offer price must be greater than 0")
        double offerPrice,

        @Min(value = 0, message = "Number of offers cannot be negative")
        int numberOfOffers,

        @Min(value = 0, message = "Reported incidents cannot be negative")
        int reportedIncidents
) {

    public Transaction toDomainTransaction() {
        return new Transaction(
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