package com.carrierfraud.api;

import com.carrierfraud.audit.AuditService;
import com.carrierfraud.domain.AlertAssignmentStatus;
import com.carrierfraud.domain.AlertSeverity;
import com.carrierfraud.domain.Comment;
import com.carrierfraud.domain.Department;
import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.infrastructure.CommentRepository;
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
class CommentControllerTest {

    @Mock private RiskAlertRepository alertRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private AuditService auditService;

    private CommentController controller;

    @BeforeEach
    void setUp() {
        controller = new CommentController(alertRepository, commentRepository, auditService);
    }

    private Authentication auth(String username, String role) {
        return new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private RiskAlert sampleAlert(String alertId, Department dept) {
        RiskAlert alert = new RiskAlert(alertId, "Carrier-A", 0.5, "Rule", AlertSeverity.MEDIUM, dept);
        return alert;
    }

    @Test
    void list_clientOnOwnAlert_returnsComments() {
        RiskAlert alert = sampleAlert("A1", Department.MEDIATION);
        alert.setCreatedBy("client1");
        when(alertRepository.findByAlertId("A1")).thenReturn(Optional.of(alert));
        when(commentRepository.findByAlertIdOrderByCreatedAtAsc("A1")).thenReturn(List.of());

        ResponseEntity<List<CommentResponse>> response = controller.list("A1", auth("client1", "CLIENT"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void list_clientOnOthersAlert_throwsAccessDenied() {
        RiskAlert alert = sampleAlert("A2", Department.MEDIATION);
        alert.setCreatedBy("client2");
        when(alertRepository.findByAlertId("A2")).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> controller.list("A2", auth("client1", "CLIENT")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void list_analystOnAlertOutsideDepartment_throwsAccessDenied() {
        RiskAlert alert = sampleAlert("A3", Department.LEGAL);
        when(alertRepository.findByAlertId("A3")).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> controller.list("A3", auth("alice", "ANALYST")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_validRequest_returnsCreatedComment() {
        RiskAlert alert = sampleAlert("A4", Department.MEDIATION);
        when(alertRepository.findByAlertId("A4")).thenReturn(Optional.of(alert));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentRequest request = new CommentRequest("Started investigation today.");

        ResponseEntity<CommentResponse> response = controller.create("A4", request, auth("admin", "ADMIN"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().author()).isEqualTo("admin");
        assertThat(response.getBody().authorRole()).isEqualTo("ADMIN");
        verify(auditService).record(any(), any(), any(), any());
    }
}