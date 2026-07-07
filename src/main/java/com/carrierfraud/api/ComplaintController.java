package com.carrierfraud.api;

import com.carrierfraud.audit.AuditService;
import com.carrierfraud.domain.AlertSeverity;
import com.carrierfraud.domain.Department;
import com.carrierfraud.domain.DocumentMetadata;
import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.infrastructure.RiskAlertRepository;
import com.carrierfraud.infrastructure.storage.DocumentStorage;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "Complaints")
@RequestMapping("/api/v1/complaints")
public class ComplaintController {

    private static final String CLIENT_ROLE = "ROLE_CLIENT";

    private final RiskAlertRepository alertRepository;
    private final DocumentStorage documentStorage;
    private final AuditService auditService;

    public ComplaintController(RiskAlertRepository alertRepository,
                               DocumentStorage documentStorage,
                               AuditService auditService) {
        this.alertRepository = alertRepository;
        this.documentStorage = documentStorage;
        this.auditService = auditService;
    }

    @PostMapping
    public ResponseEntity<RiskAlertResponse> submitComplaint(
            @Valid @RequestPart("complaint") ComplaintRequest request,
            @RequestPart(value = "documents", required = false) MultipartFile[] documents,
            @RequestParam(value = "categories", required = false) String[] categories,
            Authentication authentication) {

        ensureClientRole(authentication);

        Department department = routeByComplaintType(request.complaintType());
        String alertId = generateAlertId(request.carrierName());

        RiskAlert alert = new RiskAlert(
                alertId,
                request.carrierName(),
                0.5,
                "ClientComplaint:" + request.complaintType(),
                AlertSeverity.MEDIUM,
                department
        );
        alert.setDescription(request.description());
        alert.setCreatedBy(authentication.getName());
        alert.touchActivity();

        if (documents != null) {
            for (int i = 0; i < documents.length; i++) {
                MultipartFile file = documents[i];
                if (file != null && !file.isEmpty()) {
                    com.carrierfraud.domain.DocumentCategory category = parseCategory(categories, i);
                    DocumentMetadata metadata = documentStorage.store(file, category);
                    alert.addDocument(metadata);
                }
            }
        }

        RiskAlert saved = alertRepository.save(alert);
        auditService.record("SUBMIT_COMPLAINT", "RiskAlert", saved.getAlertId(),
                "client=" + authentication.getName() + " docs=" + saved.getDocuments().size());

        return ResponseEntity.ok(RiskAlertResponse.fromDomainAlert(saved));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<RiskAlertResponse>> myComplaints(Authentication authentication) {
        ensureClientRole(authentication);

        List<RiskAlert> alerts = alertRepository.findByCreatedByOrderByCreatedDateDesc(authentication.getName());
        auditService.record("LIST_MY_COMPLAINTS", "RiskAlert", null,
                "client=" + authentication.getName() + " count=" + alerts.size());

        List<RiskAlertResponse> responses = alerts.stream()
                .map(RiskAlertResponse::fromDomainAlert)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{alertId}/documents/{documentId}")
    public ResponseEntity<InputStreamResource> downloadDocument(
            @PathVariable String alertId,
            @PathVariable String documentId,
            @org.springframework.web.bind.annotation.RequestParam(value = "inline", required = false, defaultValue = "false") boolean inline,
            Authentication authentication) {

        RiskAlert alert = alertRepository.findByAlertId(alertId)
                .orElseThrow(() -> new com.carrierfraud.domain.BusinessRuleException(
                        "Alert not found: " + alertId));

        DocumentMetadata document = alert.getDocuments().stream()
                .filter(d -> d.documentId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new com.carrierfraud.domain.BusinessRuleException(
                        "Document not found: " + documentId));

        auditService.record("DOWNLOAD_DOCUMENT", "Document", documentId,
                "user=" + authentication.getName() + " alert=" + alertId);

        InputStreamResource resource = new InputStreamResource(documentStorage.load(document.storedPath()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (inline ? "inline" : "attachment") + "; filename=\"" + document.originalFilename() + "\"")
                .body(resource);
    }

    private void ensureClientRole(Authentication authentication) {
        boolean isClient = authentication.getAuthorities().stream()
                .anyMatch(a -> CLIENT_ROLE.equals(a.getAuthority()));
        if (!isClient) {
            throw new AccessDeniedException("Only CLIENT users can submit complaints");
        }
    }

    private Department routeByComplaintType(String complaintType) {
        return switch (complaintType.toUpperCase()) {
            case "INSURANCE", "ACCIDENT" -> Department.INSURANCE;
            case "PAYMENT", "REVIEWING" -> Department.MEDIATION;
            case "COMMERCIAL_DISPUTE", "FRAUD" -> Department.FRAUD_INVESTIGATION;
            default -> Department.LEGAL;
        };
    }

    private String generateAlertId(String carrierName) {
        String normalized = carrierName.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return "COMPLAINT_" + normalized + "_" + Instant.now().toEpochMilli();
    }

    private com.carrierfraud.domain.DocumentCategory parseCategory(String[] categories, int index) {
        if (categories == null || index >= categories.length || categories[index] == null) {
            return com.carrierfraud.domain.DocumentCategory.OTHER;
        }
        try {
            return com.carrierfraud.domain.DocumentCategory.valueOf(categories[index].toUpperCase());
        } catch (IllegalArgumentException ex) {
            return com.carrierfraud.domain.DocumentCategory.OTHER;
        }
    }
}
