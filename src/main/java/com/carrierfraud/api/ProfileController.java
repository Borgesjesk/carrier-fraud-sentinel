package com.carrierfraud.api;

import com.carrierfraud.domain.User;
import com.carrierfraud.infrastructure.UserRepository;
import com.carrierfraud.infrastructure.RefreshTokenRepository;
import com.carrierfraud.security.MfaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@Tag(name = "Profile")
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MfaService mfaService;

    public ProfileController(UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             RefreshTokenRepository refreshTokenRepository,
                             MfaService mfaService) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository);
        this.mfaService = Objects.requireNonNull(mfaService);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile(Authentication auth) {
        User user = load(auth.getName());
        long activeSessions = refreshTokenRepository.findAll().stream()
                .filter(t -> t.getUsername().equals(user.getUsername()) && t.isValid())
                .count();
        int backupCodesRemaining = user.getBackupCodes() != null ? user.getBackupCodes().size() : 0;

        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "email", user.getEmail() != null ? user.getEmail() : "",
                "role", user.getRole().name(),
                "mfaEnabled", user.isMfaEnabled(),
                "backupCodesRemaining", backupCodesRemaining,
                "activeSessions", activeSessions,
                "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
        ));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                               Authentication auth,
                                               HttpServletRequest request) {
        User user = load(auth.getName());
        requireKnownIp(user, request);
        if (!passwordEncoder.matches(req.oldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        requireMfa(user, req.totpCode());
        if (req.newPassword().length() < 12) {
            throw new IllegalArgumentException("New password must be at least 12 characters");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/email")
    public ResponseEntity<Void> changeEmail(@Valid @RequestBody ChangeEmailRequest req,
                                            Authentication auth,
                                            HttpServletRequest request) {
        User user = load(auth.getName());
        if (user.getRole() != com.carrierfraud.domain.Role.CLIENT
                && user.getRole() != com.carrierfraud.domain.Role.ADMIN) {
            throw new SecurityException("Only clients can self-service change email. Contact admin.");
        }
        requireKnownIp(user, request);
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Password verification failed");
        }
        requireMfa(user, req.totpCode());
        user.setEmail(req.newEmail());
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<Void> disableMfa(@Valid @RequestBody DisableMfaRequest req,
                                           Authentication auth,
                                           HttpServletRequest request) {
        User user = load(auth.getName());
        if (user.getRole() != com.carrierfraud.domain.Role.ADMIN) {
            throw new SecurityException("Only administrators can disable MFA. Contact admin.");
        }
        requireKnownIp(user, request);
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Password verification failed");
        }
        requireMfa(user, req.totpCode());
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setBackupCodes(new java.util.ArrayList<>());
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    private User load(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private void requireKnownIp(User user, HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (user.getKnownIps() == null || !user.getKnownIps().contains(remoteAddr)) {
            throw new SecurityException("Change blocked: request from unrecognized IP address. Contact administrator.");
        }
    }

    private void requireMfa(User user, Integer totpCode) {
        if (!user.isMfaEnabled()) return;
        if (totpCode == null || !mfaService.verifyCode(user.getUsername(), totpCode)) {
            throw new SecurityException("Invalid or missing MFA code");
        }
    }

    @GetMapping("/admin/users")
    public ResponseEntity<java.util.List<Map<String, Object>>> listUsers(Authentication auth) {
        User caller = load(auth.getName());
        if (caller.getRole() != com.carrierfraud.domain.Role.ADMIN) {
            throw new SecurityException("Only administrators can list users");
        }
        java.util.List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(u -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("username", u.getUsername());
                    m.put("email", u.getEmail() != null ? u.getEmail() : "");
                    m.put("role", u.getRole().name());
                    m.put("mfaEnabled", u.isMfaEnabled());
                    m.put("knownIpsCount", u.getKnownIps() != null ? u.getKnownIps().size() : 0);
                    m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/admin/users/{username}/mfa/disable")
    public ResponseEntity<Void> adminDisableMfa(@PathVariable String username, Authentication auth) {
        User caller = load(auth.getName());
        if (caller.getRole() != com.carrierfraud.domain.Role.ADMIN) {
            throw new SecurityException("Only administrators can disable MFA");
        }
        User target = load(username);
        target.setMfaEnabled(false);
        target.setMfaSecret(null);
        target.setBackupCodes(new java.util.ArrayList<>());
        userRepository.save(target);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/users/{username}/reset-ips")
    public ResponseEntity<Void> resetKnownIps(@PathVariable String username, Authentication auth) {
        User caller = load(auth.getName());
        if (caller.getRole() != com.carrierfraud.domain.Role.ADMIN) {
            throw new SecurityException("Only administrators can reset IPs");
        }
        User target = load(username);
        target.setKnownIps(new java.util.ArrayList<>());
        userRepository.save(target);
        return ResponseEntity.noContent().build();
    }

    public record ChangePasswordRequest(
            @NotBlank String oldPassword,
            @NotBlank @Size(min = 12) String newPassword,
            Integer totpCode
    ) {
    }

    public record ChangeEmailRequest(
            @NotBlank @Email String newEmail,
            @NotBlank String password,
            Integer totpCode
    ) {
    }

    public record DisableMfaRequest(
            @NotBlank String password,
            Integer totpCode
    ) {
    }
}