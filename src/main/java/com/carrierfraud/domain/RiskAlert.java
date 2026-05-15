package com.carrierfraud.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "alerts")
public class RiskAlert {

    @Id
    private String id;
    private String carrierName;
    private double riskScore;
    private String triggeredRule;
    private LocalDateTime alertDateTime;
    private AlertAssignmentStatus alertStatus;

    public RiskAlert() {
    }

    public RiskAlert(String carrierName, double riskScore, String triggeredRule, LocalDateTime alertDateTime, AlertAssignmentStatus alertStatus) {
        this.carrierName = carrierName;
        this.riskScore = riskScore;
        this.triggeredRule = triggeredRule;
        this.alertDateTime = alertDateTime;
        this.alertStatus = alertStatus;
    }

    public String getId() {
        return id;
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

    public AlertAssignmentStatus getAlertStatus() {
        return alertStatus;
    }
}