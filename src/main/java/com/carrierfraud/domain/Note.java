package com.carrierfraud.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "notes")
public class Note {

    @Id
    private final String noteId;

    @Indexed
    private final String alertId;

    private final String author;
    private final String authorRole;
    private final String content;
    private final LocalDateTime createdAt;

    public Note(String alertId, String author, String authorRole, String content) {
        this(UUID.randomUUID().toString(), alertId, author, authorRole, content, LocalDateTime.now());
        validate(alertId, author, authorRole, content);
    }

    @PersistenceCreator
    public Note(String noteId, String alertId, String author, String authorRole, String content, LocalDateTime createdAt) {
        this.noteId = noteId;
        this.alertId = alertId;
        this.author = author;
        this.authorRole = authorRole;
        this.content = content;
        this.createdAt = createdAt;
    }

    private static void validate(String alertId, String author, String authorRole, String content) {
        if (alertId == null || alertId.isBlank()) throw new IllegalArgumentException("alertId is required");
        if (author == null || author.isBlank()) throw new IllegalArgumentException("author is required");
        if (authorRole == null || authorRole.isBlank()) throw new IllegalArgumentException("authorRole is required");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("content is required");
        if (content.length() > 5000) throw new IllegalArgumentException("content cannot exceed 5000 characters");
    }

    public String getNoteId() { return noteId; }
    public String getAlertId() { return alertId; }
    public String getAuthor() { return author; }
    public String getAuthorRole() { return authorRole; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
