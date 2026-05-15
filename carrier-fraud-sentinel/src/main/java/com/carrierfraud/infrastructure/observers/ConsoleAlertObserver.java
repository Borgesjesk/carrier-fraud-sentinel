package com.carrierfraud.infrastructure.observers;

import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.infrastructure.AlertObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ConsoleAlertObserver logs alerts to console.
 *
 * PURPOSE:
 * - Real-time visibility during development
 * - Operator sees alerts immediately
 * - Easy to grep logs for specific carriers
 *
 * LOG OUTPUT EXAMPLE:
 * 2025-05-14 16:35:42 [ALERT] CarrierA triggered CRITICAL alert
 * Score: 1.75
 * Rules: PaymentReconciliationRule, ComplaintAccumulationRule
 * Routed to: LEGAL
 *
 * @Component = Spring registers this as a bean
 * = Automatically discovered and injected into FraudDetectionService
 */
@Component
public class ConsoleAlertObserver implements AlertObserver {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleAlertObserver.class);

    /**
     * Log alert to console and file (via SLF4J/Logback).
     *
     * @param alert the alert to log
     */
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
