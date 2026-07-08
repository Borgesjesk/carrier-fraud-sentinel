package com.carrierfraud.security;

import com.carrierfraud.domain.User;
import com.carrierfraud.infrastructure.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);
    private static final int BACKUP_CODE_COUNT = 10;
    private static final String ISSUER = "FraudSentinel";

    private final UserRepository userRepository;
    private final GoogleAuthenticator authenticator = new GoogleAuthenticator();
    private final SecureRandom random = new SecureRandom();

    public MfaService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    public MfaSetupResult startSetup(String username) {
        User user = loadUser(username);
        if (user.isMfaEnabled()) {
            throw new IllegalStateException("MFA already enabled for user " + username);
        }

        GoogleAuthenticatorKey key = authenticator.createCredentials();
        user.setMfaSecret(key.getKey());
        userRepository.save(user);

        String otpauthUri = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                ISSUER, user.getUsername(), key.getKey(), ISSUER);

        return new MfaSetupResult(key.getKey(), otpauthUri);
    }

    public List<String> confirmSetup(String username, int totpCode) {
        User user = loadUser(username);
        if (user.getMfaSecret() == null) {
            throw new IllegalStateException("MFA setup not started for user " + username);
        }
        if (user.isMfaEnabled()) {
            throw new IllegalStateException("MFA already enabled for user " + username);
        }

        if (!authenticator.authorize(user.getMfaSecret(), totpCode)) {
            throw new IllegalArgumentException("Invalid verification code");
        }

        List<String> backupCodes = generateBackupCodes();
        user.setBackupCodes(new ArrayList<>(backupCodes));
        user.setMfaEnabled(true);
        userRepository.save(user);

        log.info("MFA enabled for user {}", username);
        return backupCodes;
    }

    public boolean verifyCode(String username, int totpCode) {
        User user = loadUser(username);
        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            return false;
        }
        return authenticator.authorize(user.getMfaSecret(), totpCode);
    }

    public boolean consumeBackupCode(String username, String backupCode) {
        User user = loadUser(username);
        if (!user.isMfaEnabled() || user.getBackupCodes() == null) {
            return false;
        }
        List<String> codes = new ArrayList<>(user.getBackupCodes());
        if (!codes.remove(backupCode)) {
            return false;
        }
        user.setBackupCodes(codes);
        userRepository.save(user);
        log.info("Backup code consumed for user {} ({} remaining)", username, codes.size());
        return true;
    }

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    private List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            codes.add(String.format("%08d", random.nextInt(100_000_000)));
        }
        return codes;
    }

    public record MfaSetupResult(String secret, String otpauthUri) {
    }
}