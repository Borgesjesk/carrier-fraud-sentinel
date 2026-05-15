package com.carrierfraud.api;

import com.carrierfraud.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler handles all exceptions across all controllers.
 *
 * CENTRALIZED ERROR HANDLING:
 * - One place to handle all exceptions
 * - Consistent error response format
 * - Appropriate HTTP status codes
 * - User-friendly error messages
 *
 * ERROR TYPES:
 * 1. Validation errors (400 Bad Request)
 *    - Spring validation failed (@NotNull, @NotBlank, etc.)
 *    - User sent invalid data
 *
 * 2. Business rule errors (422 Unprocessable Entity)
 *    - Data is valid but doesn't make business sense
 *    - Example: 100 failures + 0 successes (caught by validator)
 *
 * 3. General errors (500 Internal Server Error)
 *    - Unexpected errors
 *    - Should never happen in production
 *
 * HTTP STATUS CODES:
 * - 400: Client error (bad request data)
 * - 422: Client error (data valid but nonsensical)
 * - 500: Server error (unexpected failure)
 *
 * @RestControllerAdvice = applies to all controllers globally
 * @ExceptionHandler = method handles specific exception type
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle Spring validation errors.
     * Triggered when @NotNull, @NotBlank, @Size, etc. fail.
     *
     * @param ex the validation exception from Spring
     * @return 400 Bad Request with field-level errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> fieldErrors = new HashMap<>();

        // Extract field-level error messages
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse response = new ErrorResponse(
            "Validation failed",
            400,
            fieldErrors.toString(),
            LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle business rule violations.
     * Triggered when data is valid but violates business logic.
     *
     * Example: 100 failures + 0 successes (caught by TransactionValidator)
     *
     * @param ex the business rule exception
     * @return 422 Unprocessable Entity
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleException(
            BusinessRuleException ex
    ) {
        ErrorResponse response = new ErrorResponse(
            "Business rule violation",
            422,
            ex.getMessage(),
            LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    /**
     * Handle unexpected exceptions.
     * Should rarely happen in production.
     *
     * @param ex any uncaught exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse response = new ErrorResponse(
            "Internal server error",
            500,
            ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
            LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Standard error response format.
     * All errors return this structure.
     */
    public record ErrorResponse(
        String error,
        int status,
        String message,
        LocalDateTime timestamp
    ) {}
}
