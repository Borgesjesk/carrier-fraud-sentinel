package com.carrierfraud.api;

import com.carrierfraud.domain.AlertSeverity;
import com.carrierfraud.domain.Department;
import com.carrierfraud.domain.RiskAlert;
import java.time.LocalDateTime;

/**
 * RiskAlertResponse is the API response DTO.
 *
 * Returned when fraud is detected.
 * Contains only fields needed by frontend (not internal details).
 *
 * CONVERSION:
 * RiskAlert (domain) → RiskAlertResponse (API response)
 * - Converts timestamp to ISO format
 * - Includes only public information
 * - Ready to serialize to JSON
 *
 * STATIC FACTORY:
 * Using static method `fromDomainAlert()` to convert domain object to DTO.
 * This is a common pattern for clean conversions.
 *
 * Example:
 * ```java
 * RiskAlert alert = fraudDetectionService.analyse(transaction);
 * RiskAlertResponse response = RiskAlertResponse.fromDomainAlert(alert);
 * return ResponseEntity.ok(response);
 * ```
 *
 * WHY NOT JUST RETURN RRISKALERT?
 * - Domain model might have internal fields we don't want to expose
 * - API contract is separate from domain contract
 * - Frontend only needs certain fields
 * - Security: don't expose internal implementation details
 */
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
    /**
     * Convert domain RiskAlert to API response.
     *
     * @param alert the domain alert object
     * @return response DTO ready to serialize to JSON
     */
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
