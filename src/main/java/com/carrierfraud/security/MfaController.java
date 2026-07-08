package com.carrierfraud.security;

import com.carrierfraud.security.dto.MfaSetupResponse;
import com.carrierfraud.security.dto.MfaVerifySetupRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@Tag(name = "MFA")
@RequestMapping("/api/v1/auth/mfa")
public class MfaController {

    private final MfaService mfaService;

    public MfaController(MfaService mfaService) {
        this.mfaService = Objects.requireNonNull(mfaService);
    }

    @PostMapping("/setup")
    public ResponseEntity<MfaSetupResponse> setup(Authentication authentication) {
        MfaService.MfaSetupResult result = mfaService.startSetup(authentication.getName());
        return ResponseEntity.ok(new MfaSetupResponse(result.secret(), result.otpauthUri()));
    }

    @PostMapping("/verify-setup")
    public ResponseEntity<List<String>> verifySetup(
            @Valid @RequestBody MfaVerifySetupRequest request,
            Authentication authentication) {
        List<String> backupCodes = mfaService.confirmSetup(authentication.getName(), request.code());
        return ResponseEntity.ok(backupCodes);
    }
}