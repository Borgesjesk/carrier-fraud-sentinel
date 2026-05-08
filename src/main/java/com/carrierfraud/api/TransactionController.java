package com.carrierfraud.api;

import com.carrierfraud.application.FraudDetectionService;
import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.domain.Transaction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final FraudDetectionService fraudDetectionService;

    public TransactionController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
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
}