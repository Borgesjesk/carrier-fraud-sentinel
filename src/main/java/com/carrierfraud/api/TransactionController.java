package com.carrierfraud.api;

import com.carrierfraud.application.FraudDetectionService;
import com.carrierfraud.audit.AuditService;
import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.infrastructure.RiskAlertRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final FraudDetectionService fraudDetectionService;
    private final RiskAlertRepository alertRepository;
    private final AuditService auditService;

    public TransactionController(
            FraudDetectionService fraudDetectionService,
            RiskAlertRepository alertRepository,
            AuditService auditService) {
        this.fraudDetectionService = Objects.requireNonNull(fraudDetectionService);
        this.alertRepository = Objects.requireNonNull(alertRepository);
        this.auditService = Objects.requireNonNull(auditService);
    }

    @PostMapping("/analyze")
    public ResponseEntity<RiskAlertResponse> analyze(
            @Validated @RequestBody TransactionRequest request
    ) {
        var transaction = request.toDomainTransaction();

        var validator = new com.carrierfraud.domain.TransactionalValidator();
        validator.validate(transaction);

        RiskAlert alert = fraudDetectionService.analyse(transaction);

        if (alert == null) {
            auditService.record("ANALYZE_TRANSACTION", "Transaction",
                    transaction.getCarrierName(), "No alert generated");
            return ResponseEntity.noContent().build();
        }

        auditService.record("ANALYZE_TRANSACTION", "RiskAlert",
                alert.getAlertId(),
                "severity=" + alert.getSeverity() + ", score=" + alert.getRiskScore());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RiskAlertResponse.fromDomainAlert(alert));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<RiskAlertResponse>> getAllAlerts() {
        List<RiskAlert> alerts = alertRepository.findAll();

        auditService.record("LIST_ALERTS", "RiskAlert", null,
                "count=" + alerts.size());

        List<RiskAlertResponse> responses = alerts.stream()
                .map(RiskAlertResponse::fromDomainAlert)
                .toList();

        return ResponseEntity.ok(responses);
    }
}