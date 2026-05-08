package com.carrierfraud.api;

import com.carrierfraud.application.FraudDetectionService;
import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.domain.Transaction;
import com.carrierfraud.infrastructure.RiskAlertRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final FraudDetectionService fraudDetectionService;
    private final RiskAlertRepository alertRepository;

    public TransactionController(FraudDetectionService fraudDetectionService, RiskAlertRepository alertRepository) {
        this.fraudDetectionService = fraudDetectionService;
        this.alertRepository = alertRepository;
    }

    @PostMapping("/analyze")
    public ResponseEntity<RiskAlert> analyze(@RequestBody Transaction transaction) {
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