package com.carrierfraud.security;

import com.carrierfraud.config.CookieProperties;
import com.carrierfraud.security.dto.AuthResponse;
import com.carrierfraud.security.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "Authentication")
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final CookieProperties cookieProperties;
    private final PasswordResetService passwordResetService;
    private final com.carrierfraud.infrastructure.RefreshTokenRepository refreshTokenRepository;

    public AuthController(AuthenticationService authenticationService,
                          CookieProperties cookieProperties,
                          com.carrierfraud.infrastructure.RefreshTokenRepository refreshTokenRepository,
                          PasswordResetService passwordResetService) {
        this.authenticationService = authenticationService;
        this.cookieProperties = cookieProperties;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {
        LoginResult result = authenticationService.login(request, httpRequest.getRemoteAddr());

        if (result.mfaRequired()) {
            return ResponseEntity.ok(java.util.Map.of("mfaRequired", true));
        }


        com.carrierfraud.domain.RefreshToken refreshToken = new com.carrierfraud.domain.RefreshToken(
                request.username(),
                java.time.LocalDateTime.now().plusDays(7)
        );
        refreshTokenRepository.save(refreshToken);

        ResponseCookie sessionCookie = ResponseCookie.from(cookieProperties.name(), result.token())
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .maxAge(cookieProperties.maxAgeSeconds())
                .path(cookieProperties.path())
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("FS_REFRESH", refreshToken.getTokenId())
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .maxAge(7 * 24 * 60 * 60)
                .path("/api/v1/auth")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(result.body());
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        String username = authentication.getName();
        String role = extractRole(authentication);
        return ResponseEntity.ok(new AuthResponse(username, role));
    }

    @PostMapping("/login/mfa")
    public ResponseEntity<AuthResponse> loginMfa(
            @Valid @RequestBody com.carrierfraud.security.dto.LoginMfaRequest request,
            HttpServletRequest httpRequest) {

        LoginResult result = authenticationService.loginMfa(
                new LoginRequest(request.username(), request.password()),
                request.code(),
                httpRequest.getRemoteAddr()
        );

        com.carrierfraud.domain.RefreshToken refreshToken = new com.carrierfraud.domain.RefreshToken(
                request.username(),
                java.time.LocalDateTime.now().plusDays(7)
        );
        refreshTokenRepository.save(refreshToken);

        ResponseCookie sessionCookie = ResponseCookie.from(cookieProperties.name(), result.token())
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .maxAge(cookieProperties.maxAgeSeconds())
                .path(cookieProperties.path())
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("FS_REFRESH", refreshToken.getTokenId())
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .maxAge(7 * 24 * 60 * 60)
                .path("/api/v1/auth")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(result.body());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @org.springframework.web.bind.annotation.CookieValue(name = "FS_REFRESH", required = false) String refreshTokenId) {

        if (refreshTokenId != null && !refreshTokenId.isBlank()) {
            refreshTokenRepository.findByTokenId(refreshTokenId).ifPresent(rt -> {
                rt.revoke();
                refreshTokenRepository.save(rt);
            });
        }

        ResponseCookie clearSession = ResponseCookie.from(cookieProperties.name(), "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .maxAge(0)
                .path(cookieProperties.path())
                .build();

        ResponseCookie clearRefresh = ResponseCookie.from("FS_REFRESH", "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .maxAge(0)
                .path("/api/v1/auth")
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearSession.toString())
                .header(HttpHeaders.SET_COOKIE, clearRefresh.toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @org.springframework.web.bind.annotation.CookieValue(name = "FS_REFRESH", required = false) String refreshTokenId) {

        AuthenticationService.RefreshResult result = authenticationService.refreshAccessToken(refreshTokenId);

        ResponseCookie sessionCookie = ResponseCookie.from(cookieProperties.name(), result.accessToken())
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .maxAge(cookieProperties.maxAgeSeconds())
                .path(cookieProperties.path())
                .build();

        ResponseCookie newRefreshCookie = ResponseCookie.from("FS_REFRESH", result.refreshTokenId())
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .maxAge(7 * 24 * 60 * 60)
                .path("/api/v1/auth")
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .header(HttpHeaders.SET_COOKIE, newRefreshCookie.toString())
                .build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody com.carrierfraud.security.dto.ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody com.carrierfraud.security.dto.ResetPasswordRequest request) {
        passwordResetService.applyReset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("No role assigned to authenticated user"));
    }
}
