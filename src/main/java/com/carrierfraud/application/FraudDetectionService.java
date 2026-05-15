package com.carrierfraud.application;

import com.carrierfraud.domain.AlertAssignmentStatus;
import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;
import com.carrierfraud.infrastructure.RiskAlertRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FraudDetectionService {

    private final List<StrategyRule> rules;
    private final List<AlertObserver> observers;
    private final RiskAlertRepository alertRepository;
    private final double threshold = 0.5;


    public FraudDetectionService(List<StrategyRule> rules, List<AlertObserver> observers, RiskAlertRepository alertRepository) {
        this.rules = rules;
        this.observers = observers;
        this.alertRepository = alertRepository;
    }

    public RiskAlert analyse(Transaction transaction) {
        double totalScore = 0.0;
        for (StrategyRule rule : rules) {
            totalScore += rule.evaluate(transaction);
        }
        if (totalScore >= threshold) {
            RiskAlert alert = new RiskAlert(transaction.getCarrierName(), totalScore, "Multiple rules", LocalDateTime.now(), AlertAssignmentStatus.NEW);
            alertRepository.save(alert);
            for (AlertObserver observer : observers) {
                observer.notify(alert);
            }
            return alert;
        }
        return null;
    }
}