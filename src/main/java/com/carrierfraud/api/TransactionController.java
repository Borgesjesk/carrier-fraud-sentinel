package com.carrierfraud.api;

import com.carrierfraud.application.FraudDetectionService;
import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.infrastructure.RiskAlertRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final FraudDetectionService fraudDetectionService;
    private final RiskAlertRepository alertRepository;

    public TransactionController(
            FraudDetectionService fraudDetectionService,
            RiskAlertRepository alertRepository) {
        this.fraudDetectionService = Objects.requireNonNull(fraudDetectionService);
        this.alertRepository = Objects.requireNonNull(alertRepository);
    }

    @PostMapping("/analyze")
    public ResponseEntity<RiskAlertResponse> analyze(
            @Validated @RequestBody TransactionRequest request
    ) {
        var transaction = request.toDomainTransaction();

        var validator = new com.carrierfraud.domain.TransactionalValidator();
        validator.validate(transaction);

        RiskAlert alert = fraudDetectionService.analyse(transaction);

        if (transaction == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RiskAlertResponse.fromDomainAlert(alert));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<RiskAlertResponse>> getAllAlerts() {
        List<RiskAlert> alerts = alertRepository.findAll();

        List<RiskAlertResponse> responses = alerts.stream()
                .map(RiskAlertResponse::fromDomainAlert)
                .toList();

        return ResponseEntity.ok(responses);
    }
}