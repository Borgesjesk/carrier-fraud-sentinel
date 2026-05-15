package com.carrierfraud.domain;

/**
 * BusinessRuleException is thrown when a domain object violates business rules.
 *
 * DISTINCTION:
 * - IllegalArgumentException (constructor): "This field is invalid" (carrierName is null)
 * - BusinessRuleException (validator): "These fields violate business logic" (80% failures + 0 successes)
 *
 * WHY SEPARATE?
 * - Different handling paths
 * - Caller can distinguish between:
 *   - Bad input data (IllegalArgumentException) → reject API request
 *   - Nonsensical combination (BusinessRuleException) → alert operations, may be fraud
 *
 * USAGE:
 * ```java
 * try {
 *     Transaction t = new Transaction(...);  // Can throw IllegalArgumentException
 *     validator.validate(t);                  // Can throw BusinessRuleException
 * } catch (IllegalArgumentException e) {
 *     // HTTP 400 Bad Request
 *     return error("Invalid transaction data: " + e.getMessage());
 * } catch (BusinessRuleException e) {
 *     // HTTP 422 Unprocessable Entity (data is valid but nonsensical)
 *     return error("Business rule violation: " + e.getMessage());
 * }
 * ```
 *
 * This is part of clean error handling - different errors need different responses.
 */
public class BusinessRuleException extends RuntimeException {

    /**
     * Create exception with message.
     *
     * @param message describes which business rule was violated
     */
    public BusinessRuleException(String message) {
        super(message);
    }

    /**
     * Create exception with message and cause.
     *
     * @param message describes which business rule was violated
     * @param cause the underlying exception
     */
    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
