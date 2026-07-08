package com.carrierfraud.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    private final String tokenId;

    @Indexed
    private final String username;

    private final LocalDateTime issuedAt;
    private final LocalDateTime expiresAt;
    private boolean used;

    public PasswordResetToken(String username, LocalDateTime expiresAt) {
        this(UUID.randomUUID().toString(), username, LocalDateTime.now(), expiresAt, false);
    }

    @PersistenceCreator
    public PasswordResetToken(String tokenId, String username, LocalDateTime issuedAt,
                              LocalDateTime expiresAt, boolean used) {
        this.tokenId = tokenId;
        this.username = username;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.used = used;
    }

    public boolean isValid() {
        return !used && expiresAt.isAfter(LocalDateTime.now());
    }

    public void markUsed() {
        this.used = true;
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }
}