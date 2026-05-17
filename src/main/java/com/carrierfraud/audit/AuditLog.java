package com.carrierfraud.audit;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    private String username;
    private String action;
    private String resourceType;
    private String resourceId;
    private String details;
    private String ipAddress;
    private Instant timestamp;

    public AuditLog() {
    }

    public AuditLog(
            String username,
            String action,
            String resourceType,
            String resourceId,
            String details,
            String ipAddress
    ) {
        this.username = username;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.details = details;
        this.ipAddress = ipAddress;
        this.timestamp = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
