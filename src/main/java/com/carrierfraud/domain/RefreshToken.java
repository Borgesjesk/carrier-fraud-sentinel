package com.carrierfraud.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "refresh_tokens")
public class RefreshToken {

    @Id
    private final String tokenId;

    @Indexed
    private final String username;

    private final LocalDateTime issuedAt;
    private final LocalDateTime expiresAt;
    private boolean revoked;

    public RefreshToken(String username, LocalDateTime expiresAt) {
        this(UUID.randomUUID().toString(), username, LocalDateTime.now(), expiresAt, false);
        validate(username, expiresAt);
    }

    @PersistenceCreator
    public RefreshToken(String tokenId, String username, LocalDateTime issuedAt,
                        LocalDateTime expiresAt, boolean revoked) {
        this.tokenId = tokenId;
        this.username = username;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
    }

    private static void validate(String username, LocalDateTime expiresAt) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("expiresAt must be in the future");
        }
    }

    public boolean isValid() {
        return !revoked && expiresAt.isAfter(LocalDateTime.now());
    }

    public void revoke() {
        this.revoked = true;
    }

    public String getTokenId() { return tokenId; }
    public String getUsername() { return username; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
}