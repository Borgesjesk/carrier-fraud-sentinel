package com.carrierfraud.api;

import com.carrierfraud.audit.AuditService;
import com.carrierfraud.domain.DocumentMetadata;
import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.infrastructure.RiskAlertRepository;
import com.carrierfraud.infrastructure.storage.DocumentStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplaintControllerTest {

    @Mock private RiskAlertRepository alertRepository;
    @Mock private DocumentStorage documentStorage;
    @Mock private AuditService auditService;

    private ComplaintController controller;

    @BeforeEach
    void setUp() {
        controller = new ComplaintController(alertRepository, documentStorage, auditService);
        org.mockito.Mockito.lenient().when(alertRepository.save(any(RiskAlert.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Authentication clientAuth() {
        return new UsernamePasswordAuthenticationToken(
                "client1", null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
    }

    private Authentication adminAuth() {
        return new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void submitComplaint_paymentType_routesToMediation() {
        ComplaintRequest request = new ComplaintRequest(
                "Carrier-X",
                "Twenty character description here at minimum",
                "PAYMENT");

        ResponseEntity<RiskAlertResponse> response = controller.submitComplaint(
                request, new MultipartFile[]{}, clientAuth());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().assignedDepartment()).isEqualTo("MEDIATION");
        verify(auditService).record(eq("SUBMIT_COMPLAINT"), any(), any(), any());
    }

    @Test
    void submitComplaint_fraudType_routesToFraudInvestigation() {
        ComplaintRequest request = new ComplaintRequest(
                "Carrier-Y",
                "Suspicious activity detected on multiple shipments",
                "FRAUD");

        ResponseEntity<RiskAlertResponse> response = controller.submitComplaint(
                request, new MultipartFile[]{}, clientAuth());

        assertThat(response.getBody().assignedDepartment()).isEqualTo("FRAUD_INVESTIGATION");
    }

    @Test
    void submitComplaint_byNonClientRole_throwsAccessDenied() {
        ComplaintRequest request = new ComplaintRequest(
                "Carrier-Z",
                "Description that is at least twenty chars",
                "PAYMENT");

        assertThatThrownBy(() -> controller.submitComplaint(
                request, new MultipartFile[]{}, adminAuth()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("CLIENT");
    }

    @Test
    void submitComplaint_withDocuments_callsStorageForEach() {
        MockMultipartFile file1 = new MockMultipartFile(
                "documents", "a.pdf", "application/pdf", "a".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "documents", "b.pdf", "application/pdf", "b".getBytes());

        when(documentStorage.store(any())).thenReturn(new DocumentMetadata(
                "doc-id", "a.pdf", "/path", "application/pdf", 1, LocalDateTime.now(),
                com.carrierfraud.domain.DocumentCategory.OTHER));

        ComplaintRequest request = new ComplaintRequest(
                "Carrier-W",
                "Twenty character description here at min len",
                "INSURANCE");

        controller.submitComplaint(request, new MultipartFile[]{file1, file2}, clientAuth());

        verify(documentStorage, times(2)).store(any());
    }
}