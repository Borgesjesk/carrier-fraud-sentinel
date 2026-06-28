package com.carrierfraud.api;

import com.carrierfraud.audit.AuditService;
import com.carrierfraud.domain.Comment;
import com.carrierfraud.domain.Role;
import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.infrastructure.CommentRepository;
import com.carrierfraud.infrastructure.RiskAlertRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts/{alertId}/comments")
public class CommentController {

    private final RiskAlertRepository alertRepository;
    private final CommentRepository commentRepository;
    private final AuditService auditService;

    public CommentController(RiskAlertRepository alertRepository,
                             CommentRepository commentRepository,
                             AuditService auditService) {
        this.alertRepository = alertRepository;
        this.commentRepository = commentRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> list(
            @PathVariable String alertId,
            Authentication authentication) {

        RiskAlert alert = alertRepository.findByAlertId(alertId)
                .orElseThrow(() -> new com.carrierfraud.domain.BusinessRuleException(
                        "Alert not found: " + alertId));

        ensureAccess(alert, authentication);

        List<CommentResponse> comments = commentRepository
                .findByAlertIdOrderByCreatedAtAsc(alertId)
                .stream()
                .map(CommentResponse::from)
                .toList();

        return ResponseEntity.ok(comments);
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @PathVariable String alertId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {

        RiskAlert alert = alertRepository.findByAlertId(alertId)
                .orElseThrow(() -> new com.carrierfraud.domain.BusinessRuleException(
                        "Alert not found: " + alertId));

        ensureAccess(alert, authentication);

        String role = extractRole(authentication);
        Comment comment = new Comment(alertId, authentication.getName(), role, request.content());
        Comment saved = commentRepository.save(comment);

        auditService.record("ADD_COMMENT", "Comment", saved.getCommentId(),
                "alert=" + alertId + " author=" + authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.from(saved));
    }

    private void ensureAccess(RiskAlert alert, Authentication authentication) {
        String roleName = extractRole(authentication);
        Role role = Role.valueOf(roleName);

        if (role == Role.CLIENT) {
            if (!authentication.getName().equals(alert.getCreatedBy())) {
                throw new AccessDeniedException("Clients can only access their own alerts");
            }
            return;
        }

        if (role == Role.ADMIN) {
            return;
        }

        if (!role.visibleDepartments().contains(alert.getAssignedDepartment())) {
            throw new AccessDeniedException(
                    "Role " + role + " cannot access alerts in " + alert.getAssignedDepartment());
        }
    }

    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("No role assigned"));
    }
}
