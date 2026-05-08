package com.carrierfraud.infrastructure;

import com.carrierfraud.application.AlertObserver;
import com.carrierfraud.application.RiskScoreEvaluator;
import com.carrierfraud.domain.RiskAlert;
import org.springframework.stereotype.Component;

@Component
public class ConsoleAlertObserver implements AlertObserver {

    @Override
    public void notify(RiskAlert alert) {
        System.out.println("🚨 ALERT: " + alert.getCarrierName() + " | Score: " + alert.getRiskScore() + " | Urgency: " + RiskScoreEvaluator.evaluate(alert.getRiskScore()) + " | " + alert.getAlertStatus());
    }
}
