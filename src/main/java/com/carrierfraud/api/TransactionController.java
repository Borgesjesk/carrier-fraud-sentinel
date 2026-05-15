package com.carrierfraud.api;

import com.carrierfraud.application.FraudDetectionService;
import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.domain.Transaction;
import com.carrierfraud.infrastructure.RiskAlertRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.runtime.ObjectMethods;
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
    public ResponseEntity<RiskAlert> analyze(@Valid @RequestBody TransactionRequest request) {
        if (transaction == null) {
            return ResponseEntity.badRequest().build();
        }
        RiskAlert alert = fraudDetectionService.analyse(transaction);
        if (alert == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(alert);
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<RiskAlert>> getAllAlerts() {
        List<RiskAlert> alerts = alertRepository.findAll();
        if (alerts.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(alerts);
    }
}