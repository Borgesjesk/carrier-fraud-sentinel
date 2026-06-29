package com.carrierfraud.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "alert_reads")
@CompoundIndex(name = "user_alert_idx", def = "{'username': 1, 'alertId': 1}", unique = true)
public class AlertRead {

    @Id
    private final String id;

    private final String username;
    private final String alertId;
    private LocalDateTime lastReadAt;

    public AlertRead(String username, String alertId, LocalDateTime lastReadAt) {
        this(UUID.randomUUID().toString(), username, alertId, lastReadAt);
        validate(username, alertId, lastReadAt);
    }

    @PersistenceCreator
    public AlertRead(String id, String username, String alertId, LocalDateTime lastReadAt) {
        this.id = id;
        this.username = username;
        this.alertId = alertId;
        this.lastReadAt = lastReadAt;
    }

    private static void validate(String username, String alertId, LocalDateTime lastReadAt) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username is required");
        if (alertId == null || alertId.isBlank()) throw new IllegalArgumentException("alertId is required");
        if (lastReadAt == null) throw new IllegalArgumentException("lastReadAt is required");
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getAlertId() { return alertId; }
    public LocalDateTime getLastReadAt() { return lastReadAt; }

    public void touch(LocalDateTime when) {
        this.lastReadAt = when;
    }
}
