package com.carrierfraud.api;

import com.carrierfraud.domain.RiskAlert;

import java.time.LocalDateTime;

public record RiskAlertResponse(
        String alertId,
        String carrierName,
        double riskScore,
        String triggeredRules,
        String severity,
        String assignedDepartment,
        String status,
        LocalDateTime createdDate
) {

    public static RiskAlertResponse fromDomainAlert(RiskAlert alert) {
        return new RiskAlertResponse(
                alert.getAlertId(),
                alert.getCarrierName(),
                alert.getRiskScore(),
                alert.getTriggeredRuleNames(),
                alert.getSeverity().toString(),
                alert.getAssignedDepartment().toString(),
                alert.getAssignmentStatus().toString(),
                alert.getCreatedDate()
        );
    }
}