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
        String clientVisibleStatus,
        LocalDateTime createdDate,
        String description,
        List<DocumentMetadata> documents,
        String createdBy,
        String assignedTo,
        LocalDateTime lastTransferAt,
        String lastTransferBy,
        String lastTransferFromDept
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
                computeClientVisibleStatus(alert),
                alert.getCreatedDate(),
                alert.getDescription(),
                alert.getDocuments(),
                alert.getCreatedBy(),
                alert.getAssignedTo(),
                alert.getLastTransferAt(),
                alert.getLastTransferBy(),
                alert.getLastTransferFromDept()
        );
    }

    private static String computeClientVisibleStatus(RiskAlert alert) {
        String status = alert.getAssignmentStatus().toString();
        if ("RESOLVED".equals(status)) return "RESOLVED";
        if (alert.getAcceptedDate() != null) return "IN_PROGRESS";
        return status;
    }
}
