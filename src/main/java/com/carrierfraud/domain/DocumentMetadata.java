package com.carrierfraud.domain;

import java.time.LocalDateTime;

public record DocumentMetadata(
        String documentId,
        String originalFilename,
        String storedPath,
        String contentType,
        long sizeBytes,
        LocalDateTime uploadedAt,
        DocumentCategory category
) {
    public DocumentMetadata {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId is required");
        }
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("originalFilename is required");
        }
        if (storedPath == null || storedPath.isBlank()) {
            throw new IllegalArgumentException("storedPath is required");
        }
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("sizeBytes must be positive");
        }
        if (category == null) {
            category = DocumentCategory.OTHER;
        }
    }
}
