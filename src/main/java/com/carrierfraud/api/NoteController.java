package com.carrierfraud.api;

import com.carrierfraud.audit.AuditService;
import com.carrierfraud.domain.Note;
import com.carrierfraud.domain.Role;
import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.infrastructure.NoteRepository;
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
@io.swagger.v3.oas.annotations.tags.Tag(name = "Notes")
@RequestMapping("/api/v1/alerts/{alertId}/notes")
public class NoteController {

    private final RiskAlertRepository alertRepository;
    private final NoteRepository noteRepository;
    private final AuditService auditService;

    public NoteController(RiskAlertRepository alertRepository,
                          NoteRepository noteRepository,
                          AuditService auditService) {
        this.alertRepository = alertRepository;
        this.noteRepository = noteRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> list(
            @PathVariable String alertId,
            Authentication authentication) {

        ensureStaff(authentication);
        RiskAlert alert = ensureAlertExists(alertId);

        List<NoteResponse> notes = noteRepository
                .findByAlertIdOrderByCreatedAtAsc(alertId)
                .stream()
                .map(NoteResponse::from)
                .toList();

        return ResponseEntity.ok(notes);
    }

    @PostMapping
    public ResponseEntity<NoteResponse> create(
            @PathVariable String alertId,
            @Valid @RequestBody NoteRequest request,
            Authentication authentication) {

        ensureStaff(authentication);
        RiskAlert alert = ensureAlertExists(alertId);

        String role = extractRole(authentication);
        Note note = new Note(alertId, authentication.getName(), role, request.content());
        Note saved = noteRepository.save(note);
        alert.touchActivity();
        alertRepository.save(alert);

        auditService.record("ADD_NOTE", "Note", saved.getNoteId(),
                "alert=" + alertId + " author=" + authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(NoteResponse.from(saved));
    }

    private void ensureStaff(Authentication authentication) {
        String role = extractRole(authentication);
        if (Role.CLIENT.name().equals(role)) {
            throw new AccessDeniedException("Clients cannot access internal notes");
        }
    }

    private RiskAlert ensureAlertExists(String alertId) {
        return alertRepository.findByAlertId(alertId)
                .orElseThrow(() -> new com.carrierfraud.domain.BusinessRuleException(
                        "Alert not found: " + alertId));
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
