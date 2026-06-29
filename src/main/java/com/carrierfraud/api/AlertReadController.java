package com.carrierfraud.api;

import com.carrierfraud.application.AlertReadService;
import com.carrierfraud.infrastructure.RiskAlertRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AlertReadController {

    private final AlertReadService alertReadService;
    private final RiskAlertRepository alertRepository;

    public AlertReadController(AlertReadService alertReadService,
                               RiskAlertRepository alertRepository) {
        this.alertReadService = alertReadService;
        this.alertRepository = alertRepository;
    }

    @PostMapping("/api/v1/alerts/{alertId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String alertId, Authentication authentication) {
        alertReadService.markAsRead(authentication.getName(), alertId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/alerts/unread-counts")
    public ResponseEntity<Map<String, Long>> unreadCounts(Authentication authentication) {
        List<String> allAlertIds = alertRepository.findAll().stream()
                .map(a -> a.getAlertId())
                .toList();
        Map<String, Long> counts = alertReadService.unreadCounts(authentication.getName(), allAlertIds);
        return ResponseEntity.ok(counts);
    }
}
