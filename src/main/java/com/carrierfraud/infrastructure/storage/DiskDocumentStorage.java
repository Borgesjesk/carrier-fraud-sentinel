package com.carrierfraud.infrastructure.storage;

import com.carrierfraud.domain.DocumentMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Component
public class DiskDocumentStorage implements DocumentStorage {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private final Path storageRoot;

    public DiskDocumentStorage(@Value("${app.storage.root:./uploads}") String storageRoot) {
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot create storage root: " + storageRoot, ex);
        }
    }

    @Override
    public DocumentMetadata store(MultipartFile file, com.carrierfraud.domain.DocumentCategory category) {
        validate(file);
        String documentId = UUID.randomUUID().toString();
        String safeName = documentId + extensionOf(file.getOriginalFilename());
        Path destination = storageRoot.resolve(safeName).normalize();

        if (!destination.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Path traversal attempt detected");
        }

        try {
            file.transferTo(destination);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store document: " + file.getOriginalFilename(), ex);
        }

        return new DocumentMetadata(
                documentId,
                file.getOriginalFilename(),
                destination.toString(),
                file.getContentType(),
                file.getSize(),
                LocalDateTime.now(),
                category
        );
    }

    @Override
    public InputStream load(String storedPath) {
        Path path = Paths.get(storedPath).normalize();
        if (!path.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Path traversal attempt detected");
        }
        try {
            return Files.newInputStream(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read document: " + storedPath, ex);
        }
    }

    @Override
    public void delete(String storedPath) {
        Path path = Paths.get(storedPath).normalize();
        if (!path.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Path traversal attempt detected");
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete document: " + storedPath, ex);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds max size of 10 MB");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Unsupported content type: " + file.getContentType());
        }
    }

    private String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return (dot >= 0 && dot < filename.length() - 1) ? filename.substring(dot) : "";
    }
}
