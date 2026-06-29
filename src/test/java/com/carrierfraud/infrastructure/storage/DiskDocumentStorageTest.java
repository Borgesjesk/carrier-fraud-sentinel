package com.carrierfraud.infrastructure.storage;

import com.carrierfraud.domain.DocumentMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiskDocumentStorageTest {

    @TempDir
    Path tempDir;

    private DiskDocumentStorage storage;

    @BeforeEach
    void setUp() {
        storage = new DiskDocumentStorage(tempDir.toString());
        storage.init();
    }

    @Test
    void store_validPdf_persistsFileAndReturnsMetadata() {
        MockMultipartFile file = new MockMultipartFile(
                "doc", "invoice.pdf", "application/pdf", "fake-pdf-content".getBytes());

        DocumentMetadata metadata = storage.store(file);

        assertThat(metadata.originalFilename()).isEqualTo("invoice.pdf");
        assertThat(metadata.contentType()).isEqualTo("application/pdf");
        assertThat(metadata.sizeBytes()).isEqualTo(16);
        assertThat(Files.exists(Path.of(metadata.storedPath()))).isTrue();
    }

    @Test
    void store_emptyFile_throwsIllegalArgument() {
        MockMultipartFile empty = new MockMultipartFile(
                "doc", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> storage.store(empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void store_unsupportedContentType_throwsIllegalArgument() {
        MockMultipartFile exe = new MockMultipartFile(
                "doc", "malware.exe", "application/x-msdownload", "evil".getBytes());

        assertThatThrownBy(() -> storage.store(exe))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported content type");
    }

    @Test
    void store_fileExceedsMaxSize_throwsIllegalArgument() {
        byte[] tooLarge = new byte[11 * 1024 * 1024];
        MockMultipartFile huge = new MockMultipartFile(
                "doc", "huge.pdf", "application/pdf", tooLarge);

        assertThatThrownBy(() -> storage.store(huge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max size");
    }

    @Test
    void load_pathTraversalAttempt_throwsIllegalArgument() {
        assertThatThrownBy(() -> storage.load("/etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("traversal");
    }

    @Test
    void load_storedFile_returnsContent() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "doc", "test.pdf", "application/pdf", "hello".getBytes());
        DocumentMetadata metadata = storage.store(file);

        try (InputStream in = storage.load(metadata.storedPath())) {
            assertThat(new String(in.readAllBytes())).isEqualTo("hello");
        }
    }
}
