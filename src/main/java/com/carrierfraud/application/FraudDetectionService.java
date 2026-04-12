package com.carrierfraud.application;

import com.carrierfraud.domain.AlertStatus;
import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class FraudDetectionService {

    private final List<StrategyRule> rules;
    private final double threshold;

    public FraudDetectionService(List<StrategyRule> rules, double threshold) {
        this.rules = rules;
        this.threshold = threshold;
    }

    public List<StrategyRule> getRules() {
        return rules;
    }

    public double getThreshold() {
        return threshold;
    }

    public RiskAlert analyse(Transaction transaction) {
        double totalScore = 0.0;
        for (StrategyRule rule : rules) {
            totalScore += rule.evaluate(transaction);

        }
        if (totalScore >= threshold) {
            return new RiskAlert(transaction.getCarrierName(), totalScore, "Multiple rules", LocalDateTime.now(), AlertStatus.NEW);
        } else {
            return null;
        }
    }
}
