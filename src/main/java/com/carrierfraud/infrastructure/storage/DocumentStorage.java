package com.carrierfraud.infrastructure.storage;

import com.carrierfraud.domain.DocumentCategory;
import com.carrierfraud.domain.DocumentMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface DocumentStorage {
    DocumentMetadata store(MultipartFile file, DocumentCategory category);

    InputStream load(String storedPath);

    void delete(String storedPath);
}
