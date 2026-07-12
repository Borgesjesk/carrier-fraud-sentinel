package com.carrierfraud.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;
    private String passwordHash;
    private Role role;
    private boolean enabled;
    private boolean accountLocked;
    private Instant createdAt;
    private String email;
    private boolean mfaEnabled;
    private String mfaSecret;
    private java.util.List<String> backupCodes = new java.util.ArrayList<>();
    private java.util.List<String> knownIps = new java.util.ArrayList<>();

    public User(String username, String passwordHash, Role role, String email) {
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role");
        this.email = email;
        this.enabled = true;
        this.accountLocked = false;
        this.createdAt = Instant.now();
    }

    public String getEmail() {
        return email;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public String getMfaSecret() {
        return mfaSecret;
    }

    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
    }

    public java.util.List<String> getBackupCodes() {
        return backupCodes;
    }

    public void setBackupCodes(java.util.List<String> backupCodes) {
        this.backupCodes = backupCodes;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public java.util.List<String> getKnownIps() {
        return knownIps;
    }

    public void setKnownIps(java.util.List<String> knownIps) {
        this.knownIps = knownIps;
    }

    public void addKnownIp(String ip) {
        if (ip != null && !ip.isBlank() && (knownIps == null || !knownIps.contains(ip))) {
            if (knownIps == null) knownIps = new java.util.ArrayList<>();
            knownIps.add(ip);
        }
    }
}