package com.carrierfraud.security;

import com.carrierfraud.domain.PasswordResetToken;
import com.carrierfraud.domain.User;
import com.carrierfraud.infrastructure.PasswordResetTokenRepository;
import com.carrierfraud.infrastructure.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_TTL_MINUTES = 15;
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository resetRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.resetRepository = Objects.requireNonNull(resetRepository);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
    }

    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            log.info("Password reset requested with blank email, silently ignoring");
            return;
        }

        Optional<User> maybeUser = userRepository.findByEmail(email);
        if (maybeUser.isEmpty()) {
            log.info("Password reset requested for unknown email={}, silently ignoring", email);
            return;
        }

        User user = maybeUser.get();
        PasswordResetToken token = new PasswordResetToken(
                user.getUsername(),
                LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES)
        );
        resetRepository.save(token);

        log.warn("PASSWORD RESET LINK for {} (dev-only, would be emailed in prod): http://localhost:5173/reset-password?token={}",
                user.getUsername(), token.getTokenId());
    }

    public void applyReset(String tokenId, String newPassword) {
        if (tokenId == null || tokenId.isBlank()) {
            throw new AuthenticationException("Reset token missing") {
            };
        }
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "New password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        PasswordResetToken token = resetRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new AuthenticationException("Reset token not found") {
                });

        if (!token.isValid()) {
            throw new AuthenticationException("Reset token expired or already used") {
            };
        }

        User user = userRepository.findByUsername(token.getUsername())
                .orElseThrow(() -> new AuthenticationException("User not found") {
                });

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.markUsed();
        resetRepository.save(token);

        log.info("Password reset applied for username={}", user.getUsername());
    }
}