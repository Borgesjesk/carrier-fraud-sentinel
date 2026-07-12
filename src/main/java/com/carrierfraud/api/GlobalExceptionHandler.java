package com.carrierfraud.api;

import com.carrierfraud.domain.BusinessRuleException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String BASE_TYPE = "https://fraudsentinel.carrierfraud.com/problems/";
    private static final String PROP_TIMESTAMP = "timestamp";
    private static final String PROP_TRACKING_ID = "trackingId";
    private static final String PROP_ERRORS = "errors";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Validation failed for {}: {}", request.getRequestURI(), fieldErrors);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setDetail("One or more request fields failed validation.");
        return populateProblem(problem, "validation-failed", "Validation Failed", request, Map.of(PROP_ERRORS, fieldErrors));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRuleException(BusinessRuleException ex, HttpServletRequest request) {
        log.warn("Business rule violation at [{}]: {}", request.getRequestURI(), sanitizeForLog(ex.getMessage()));

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problem.setDetail(ex.getMessage());
        return populateProblem(problem, "business-rule-violation", "Business Rule Violation", request, Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument at [{}]: {}", request.getRequestURI(), sanitizeForLog(ex.getMessage()));

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setDetail("Invalid request parameters.");
        return populateProblem(problem, "invalid-argument", "Invalid Argument", request, Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed at [{}] from [{}]: {}",
                request.getRequestURI(),
                request.getRemoteAddr(),
                ex.getClass().getSimpleName());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setDetail("Authentication is required to access this resource.");
        return populateProblem(problem, "unauthorized", "Unauthorized", request, Map.of());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ProblemDetail> handleSecurity(SecurityException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setType(URI.create("https://fraudsentinel.carrierfraud.com/problems/forbidden"));
        problem.setTitle("Forbidden");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String principal = (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";

        log.warn("Access denied at [{}] for user [{}] from [{}]: {}",
                request.getRequestURI(),
                principal,
                request.getRemoteAddr(),
                sanitizeForLog(ex.getMessage()));

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setDetail("You do not have permission to access this resource.");
        return populateProblem(problem, "forbidden", "Forbidden", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception ex, HttpServletRequest request) {
        String trackingId = UUID.randomUUID().toString();
        log.error("Internal Server Error. trackingId={}, path={}", trackingId, request.getRequestURI(), ex);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setDetail("An unexpected internal error occurred. Please contact support with the tracking reference.");
        return populateProblem(problem, "internal-error", "Internal Server Error", request, Map.of(PROP_TRACKING_ID, trackingId));
    }

    private ProblemDetail populateProblem(
            ProblemDetail problem,
            String subType,
            String title,
            HttpServletRequest request,
            Map<String, Object> extensions
    ) {
        problem.setType(URI.create(BASE_TYPE + subType));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty(PROP_TIMESTAMP, Instant.now());
        extensions.forEach(problem::setProperty);
        return problem;
    }

    /** Strips CRLF from untrusted input to prevent CWE-117 log forging. */
    private String sanitizeForLog(String input) {
        if (input == null) return "null";
        return input.replaceAll("[\n\r\t]", "_");
    }
}