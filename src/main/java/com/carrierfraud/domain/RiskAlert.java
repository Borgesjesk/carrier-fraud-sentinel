package com.carrierfraud.domain;

import java.time.LocalDateTime;

public class RiskAlert {

    private final String carrierName;
    private final double riskScore;
    private final String triggeredRule;
    private final LocalDateTime alertDateTime;
    private final AlertStatus alertStatus;

    public RiskAlert(String carrierName, double riskScore, String triggeredRule, LocalDateTime alertDateTime, AlertStatus alertStatus) {
        this.carrierName = carrierName;
        this.riskScore = riskScore;
        this.triggeredRule = triggeredRule;
        this.alertDateTime = alertDateTime;
        this.alertStatus = alertStatus;
    }
    public String getCarrierName() {
        return carrierName;
    }
    public double getRiskScore() {
        return riskScore;
    }
    public String getTriggeredRule() {
        return triggeredRule;
    }
    public LocalDateTime getAlertDateTime() {
        return alertDateTime;
    }
    public AlertStatus getAlertStatus() {
        return alertStatus;
    }
}
