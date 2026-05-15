package com.carrierfraud.api;

import com.carrierfraud.application.FraudDetectionService;
import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.infrastructure.RiskAlertRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * TransactionController exposes fraud detection via REST API.
 *
 * ENDPOINTS:
 * 1. POST /api/transactions/analyze
 *    - Accept transaction data
 *    - Validate
 *    - Run fraud detection
 *    - Return alert (if fired)
 *
 * 2. GET /api/alerts
 *    - Retrieve all alerts
 *    - Return alert list
 *
 * FLOW FOR POST /analyze:
 * 1. Client sends JSON (TransactionRequest)
 * 2. Spring validates @NotNull, @NotBlank, etc.
 *    - If validation fails → GlobalExceptionHandler returns 400
 * 3. Controller converts DTO to domain Transaction
 * 4. Transaction constructor validates business rules
 *    - If validation fails → GlobalExceptionHandler returns 422
 * 5. TransactionValidator checks business logic invariants
 *    - If validation fails → GlobalExceptionHandler returns 422
 * 6. FraudDetectionService runs all fraud rules
 * 7. If score >= threshold → RiskAlert created
 * 8. Response:
 *    - Alert fired: HTTP 201 Created + alert JSON
 *    - No alert: HTTP 204 No Content
 *
 * @RestController = combines @Controller + @ResponseBody
 *   - All methods return JSON (not HTML templates)
 *
 * @RequestMapping = base path for all endpoints
 *   - All endpoints under /api/transactions
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final FraudDetectionService fraudDetectionService;
    private final RiskAlertRepository alertRepository;

    /**
     * Constructor with dependency injection.
     * Spring provides service and repository.
     *
     * @param fraudDetectionService the orchestrator
     * @param alertRepository for persisting alerts
     */
    public TransactionController(
            FraudDetectionService fraudDetectionService,
            RiskAlertRepository alertRepository
    ) {
        this.fraudDetectionService = Objects.requireNonNull(fraudDetectionService);
        this.alertRepository = Objects.requireNonNull(alertRepository);
    }

    /**
     * POST /api/transactions/analyze
     *
     * Analyze a transaction for fraud.
     *
     * REQUEST:
     * POST /api/transactions/analyze
     * Content-Type: application/json
     *
     * {
     *   "carrierName": "CarrierA",
     *   "transportName": "Transport123",
     *   "failedPayments": 10,
     *   "succeededPayments": 2,
     *   "offerPrice": 1500.0,
     *   "numberOfOffers": 50,
     *   "reportedIncidents": 5
     * }
     *
     * RESPONSE (if alert fires):
     * HTTP 201 Created
     * {
     *   "alertId": "ALERT_CARRIERA_...",
     *   "carrierName": "CarrierA",
     *   "riskScore": 0.75,
     *   "triggeredRules": "PaymentReconciliationRule, ...",
     *   "severity": "HIGH",
     *   "assignedDepartment": "PAYMENT_RECONCILIATION",
     *   "status": "UNASSIGNED",
     *   "createdDate": "2025-05-14T16:35:42"
     * }
     *
     * RESPONSE (if clean):
     * HTTP 204 No Content
     * (no body)
     *
     * ERROR RESPONSES:
     * HTTP 400 Bad Request - validation failed
     * HTTP 422 Unprocessable Entity - business rule violated
     * HTTP 500 Internal Server Error - unexpected failure
     *
     * @param request the transaction data (validated automatically by Spring)
     * @return 201 + alert if fired, 204 if clean
     */
    @PostMapping("/analyze")
    public ResponseEntity<RiskAlertResponse> analyze(
            @Valid @RequestBody TransactionRequest request
    ) {
        // STEP 1: Convert API DTO to domain Transaction
        // This triggers Transaction constructor validation
        var transaction = request.toDomainTransaction();

        // STEP 2: Validate business logic invariants
        // This catches nonsensical combinations
        var validator = new com.carrierfraud.domain.TransactionValidator();
        validator.validate(transaction);

        // STEP 3: Run fraud detection
        // Returns RiskAlert if score >= threshold, null otherwise
        RiskAlert alert = fraudDetectionService.analyse(transaction);

        // STEP 4: Return appropriate response
        if (alert == null) {
            // No alert fired, carrier is clean
            return ResponseEntity.noContent().build();
        }

        // Alert fired, return it with 201 Created status
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(RiskAlertResponse.fromDomainAlert(alert));
    }

    /**
     * GET /api/alerts
     *
     * Retrieve all fraud alerts from database.
     *
     * REQUEST:
     * GET /api/alerts
     *
     * RESPONSE (example):
     * HTTP 200 OK
     * [
     *   {
     *     "alertId": "ALERT_CARRIERA_...",
     *     "carrierName": "CarrierA",
     *     "riskScore": 0.75,
     *     "severity": "HIGH",
     *     ...
     *   },
     *   {
     *     "alertId": "ALERT_CARRIERB_...",
     *     ...
     *   }
     * ]
     *
     * RESPONSE (if no alerts):
     * HTTP 200 OK
     * []
     * (empty list)
     *
     * @return list of all alerts in database
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<RiskAlertResponse>> getAllAlerts() {
        // Query database for all alerts
        List<RiskAlert> alerts = alertRepository.findAll();

        // Convert domain objects to response DTOs
        List<RiskAlertResponse> responses = alerts.stream()
            .map(RiskAlertResponse::fromDomainAlert)
            .toList();

        // Return list (200 OK even if empty)
        return ResponseEntity.ok(responses);
    }
}
