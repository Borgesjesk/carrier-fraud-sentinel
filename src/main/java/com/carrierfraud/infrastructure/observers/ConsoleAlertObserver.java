package com.carrierfraud.infrastructure.observers;

import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.infrastructure.AlertObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleAlertObserver implements AlertObserver {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleAlertObserver.class);

    @Override
    public void notify(RiskAlert alert) {
        logger.warn(
                "FRAUD ALERT FIRED: " +
                        "AlertID={}, " +
                        "Carrier={}, " +
                        "Score={}, " +
                        "Severity={}, " +
                        "RuleTriggered={}, " +
                        "RoutedTo={}",
                alert.getAlertId(),
                alert.getCarrierName(),
                String.format("%.2f", alert.getRiskScore()),
                alert.getSeverity(),
                alert.getTriggeredRuleNames(),
                alert.getAssignedDepartment()
        );
    }
}