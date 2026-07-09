package com.carrierfraud.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Public info")
@RequestMapping("/api/v1/info")
public class InfoController {

    @GetMapping("/features")
    public ResponseEntity<Map<String, Object>> features() {
        return ResponseEntity.ok(Map.of(
                "product", "FraudSentinel",
                "version", "1.0.0",
                "features", List.of(
                        "HttpOnly cookie JWT authentication",
                        "Refresh token rotation with theft detection",
                        "TOTP-based multi-factor authentication",
                        "Rate limiting per IP",
                        "Password reset with time-limited tokens",
                        "RBAC with 4 roles across 11 departments",
                        "Multi-channel alert workflow",
                        "Real-time client-staff messaging",
                        "Staff-only internal notes",
                        "72h SLA stale detection",
                        "Cross-department case transfer",
                        "Document upload with categorization",
                        "Inline document preview"
                )
        ));
    }
}
