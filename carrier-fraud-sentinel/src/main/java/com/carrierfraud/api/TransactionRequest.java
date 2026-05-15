package com.carrierfraud.api;

import jakarta.validation.constraints.*;

/**
 * TransactionRequest is the API request DTO.
 *
 * DTO = Data Transfer Object
 * - Separates API contract from domain logic
 * - Allows API to evolve independently of domain
 * - Uses Spring validation annotations
 *
 * WHY SEPARATE FROM DOMAIN TRANSACTION?
 * - Transaction class is immutable, strict validation in constructor
 * - API might want to accept data in different format
 * - API validation is for user input (Spring annotations)
 * - Domain validation is for business logic (constructor)
 *
 * VALIDATION FLOW:
 * 1. Spring validates @NotNull, @NotBlank, @Min, etc. (request level)
 * 2. If invalid → HTTP 400 Bad Request (user error)
 * 3. If valid → Convert to Transaction domain object
 * 4. Transaction constructor validates (domain level)
 * 5. If invalid → HTTP 422 Unprocessable Entity (business logic error)
 * 6. If valid → Run fraud detection
 *
 * RECORD vs CLASS:
 * - Using Java record (immutable by default)
 * - Automatically gets equals(), hashCode(), toString()
 * - Perfect for DTO (no setters needed)
 * - Java 16+ feature (we're on Java 21)
 *
 * ANNOTATIONS:
 * - @NotNull: Field cannot be null
 * - @NotBlank: String cannot be empty or whitespace
 * - @Size: Length constraints
 * - @Min/@Max: Numeric constraints
 * - These trigger automatic validation in Spring
 */
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
    /**
     * Convert this API request to a domain Transaction object.
     * This is where DTO converts to domain model.
     *
     * @return Transaction (will validate again in constructor)
     * @throws IllegalArgumentException if domain validation fails
     */
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
