package com.carrierfraud.application;

import com.carrierfraud.domain.AlertStatus;
import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;

import java.time.LocalDateTime;
import java.util.List;

public class FraudDetectionService {

    private final List<StrategyRule> rules;
    private final double threshold;
    private final List<AlertObserver> observers;

    public FraudDetectionService(List<StrategyRule> rules, double threshold, List<AlertObserver> observers) {
        this.rules = rules;
        this.threshold = threshold;
        this.observers = observers;
    }

    public List<StrategyRule> getRules() {
        return rules;
    }

    public double getThreshold() {
        return threshold;
    }

    public List<AlertObserver> getObservers() {
        return observers;
    }

    public RiskAlert analyse(Transaction transaction) {
        double totalScore = 0.0;
        for (StrategyRule rule : rules) {
            totalScore += rule.evaluate(transaction);

        }
        if (totalScore >= threshold) {
            RiskAlert alert = new RiskAlert(transaction.getCarrierName(), totalScore, "Multiple rules", LocalDateTime.now(), AlertStatus.NEW);
            for (AlertObserver observer : observers) {
                observer.notify(alert);
            }
            return alert;
        }
        return null;
    }
}
