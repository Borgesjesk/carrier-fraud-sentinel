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

    @PutMapping("/alerts/{alertId}/accept")
    public ResponseEntity<RiskAlertResponse> acceptAlert(
            @PathVariable String alertId,
            @RequestBody java.util.Map<String, String> body) {

        RiskAlert alert = alertRepository.findByAlertId(alertId)
                .orElseThrow(() -> new com.carrierfraud.domain.BusinessRuleException(
                        "Alert not found: " + alertId));

        String person = body.getOrDefault("person", "unknown");
        alert.accept(person);
        alertRepository.save(alert);

        auditService.record("ACCEPT_ALERT", "RiskAlert", alertId,
                "Accepted by " + person);

        return ResponseEntity.ok(RiskAlertResponse.fromDomainAlert(alert));
    }

    @PutMapping("/alerts/{alertId}/investigate")
    public ResponseEntity<RiskAlertResponse> investigateAlert(
            @PathVariable String alertId) {

        RiskAlert alert = alertRepository.findByAlertId(alertId)
                .orElseThrow(() -> new com.carrierfraud.domain.BusinessRuleException(
                        "Alert not found: " + alertId));

        alert.startInvestigation();
        alertRepository.save(alert);

        auditService.record("START_INVESTIGATION", "RiskAlert", alertId,
                "Investigation started");

        return ResponseEntity.ok(RiskAlertResponse.fromDomainAlert(alert));
    }

    @PutMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<RiskAlertResponse> resolveAlert(
            @PathVariable String alertId,
            @RequestBody java.util.Map<String, String> body) {

        RiskAlert alert = alertRepository.findByAlertId(alertId)
                .orElseThrow(() -> new com.carrierfraud.domain.BusinessRuleException(
                        "Alert not found: " + alertId));

        String notes = body.getOrDefault("notes", "Resolved via dashboard");
        alert.resolve(notes);
        alertRepository.save(alert);

        auditService.record("RESOLVE_ALERT", "RiskAlert", alertId,
                "Resolved: " + notes);

        return ResponseEntity.ok(RiskAlertResponse.fromDomainAlert(alert));
    }

    @PutMapping("/alerts/{alertId}/escalate")
    public ResponseEntity<RiskAlertResponse> escalateAlert(
            @PathVariable String alertId,
            @RequestBody java.util.Map<String, String> body) {

        RiskAlert alert = alertRepository.findByAlertId(alertId)
                .orElseThrow(() -> new com.carrierfraud.domain.BusinessRuleException(
                        "Alert not found: " + alertId));

        String dept = body.getOrDefault("department", "LEGAL");
        alert.escalate(com.carrierfraud.domain.Department.valueOf(dept));
        alertRepository.save(alert);

        auditService.record("ESCALATE_ALERT", "RiskAlert", alertId,
                "Escalated to " + dept);

        return ResponseEntity.ok(RiskAlertResponse.fromDomainAlert(alert));
    }
}