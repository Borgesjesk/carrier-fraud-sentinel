package com.carrierfraud.audit;

import com.carrierfraud.domain.Role;
import com.carrierfraud.domain.User;
import com.carrierfraud.infrastructure.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@Tag(name = "Audit")
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditController(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    @GetMapping("/logs")
    public ResponseEntity<java.util.List<AuditLog>> listLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "100") int limit,
            Authentication auth
    ) {
        User caller = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        if (caller.getRole() != Role.ADMIN && caller.getRole() != Role.COMPLIANCE) {
            throw new SecurityException("Only admins and compliance can view audit logs");
        }

        java.util.List<AuditLog> logs = auditLogRepository.findAll(
                PageRequest.of(0, Math.min(limit, 500), Sort.by(Sort.Direction.DESC, "timestamp"))
        ).getContent();

        if (username != null && !username.isBlank()) {
            logs = logs.stream().filter(l -> username.equalsIgnoreCase(l.getUsername())).toList();
        }
        if (action != null && !action.isBlank()) {
            logs = logs.stream().filter(l -> action.equalsIgnoreCase(l.getAction())).toList();
        }
        return ResponseEntity.ok(logs);
    }
}
