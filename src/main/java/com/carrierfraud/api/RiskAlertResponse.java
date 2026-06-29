package com.carrierfraud.api;

import com.carrierfraud.domain.DocumentMetadata;
import com.carrierfraud.domain.RiskAlert;

import java.time.LocalDateTime;
import java.util.List;

public record RiskAlertResponse(
        String alertId,
        String carrierName,
        double riskScore,
        String triggeredRules,
        String severity,
        String assignedDepartment,
        String status,
        LocalDateTime createdDate,
        String description,
        List<DocumentMetadata> documents,
        String createdBy
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
                alert.getCreatedDate(),
                alert.getDescription(),
                alert.getDocuments(),
                alert.getCreatedBy()
        );
    }
}
