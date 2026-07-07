package com.carrierfraud.api;

import com.carrierfraud.audit.AuditService;
import com.carrierfraud.domain.AlertSeverity;
import com.carrierfraud.domain.Department;
import com.carrierfraud.domain.Note;
import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.infrastructure.NoteRepository;
import com.carrierfraud.infrastructure.RiskAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteControllerTest {

    @Mock private RiskAlertRepository alertRepository;
    @Mock private NoteRepository noteRepository;
    @Mock private AuditService auditService;

    private NoteController controller;

    @BeforeEach
    void setUp() {
        controller = new NoteController(alertRepository, noteRepository, auditService);
    }

    private Authentication auth(String username, String role) {
        return new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private RiskAlert alert() {
        return new RiskAlert("A1", "Carrier-A", 0.5, "Rule", AlertSeverity.MEDIUM, Department.MEDIATION);
    }

    @Test
    void list_staff_returnsNotes() {
        when(alertRepository.findByAlertId("A1")).thenReturn(Optional.of(alert()));
        when(noteRepository.findByAlertIdOrderByCreatedAtAsc("A1")).thenReturn(List.of());

        ResponseEntity<List<NoteResponse>> response = controller.list("A1", auth("admin", "ADMIN"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void list_client_throwsAccessDenied() {
        assertThatThrownBy(() -> controller.list("A1", auth("client1", "CLIENT")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Clients");
    }

    @Test
    void create_staff_returnsCreatedNote() {
        when(alertRepository.findByAlertId("A1")).thenReturn(Optional.of(alert()));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(alertRepository.save(any(RiskAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        NoteRequest request = new NoteRequest("Internal followup pending.");
        ResponseEntity<NoteResponse> response = controller.create("A1", request, auth("alice", "ANALYST"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().author()).isEqualTo("alice");
        assertThat(response.getBody().authorRole()).isEqualTo("ANALYST");
        verify(auditService).record(any(), any(), any(), any());
    }

    @Test
    void create_client_throwsAccessDenied() {
        NoteRequest request = new NoteRequest("Trying to post as client.");

        assertThatThrownBy(() -> controller.create("A1", request, auth("client1", "CLIENT")))
                .isInstanceOf(AccessDeniedException.class);
    }
}