package com.carrierfraud.infrastructure;

import com.carrierfraud.application.AlertObserver;
import com.carrierfraud.domain.RiskAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

@Component
public class AuditLogObserver implements AlertObserver {

    private static final Logger log = LoggerFactory.getLogger(AuditLogObserver.class);
    private static final Path AUDIT_LOG_PATH = Paths.get("audit.log");

    @Override
    public void notify(RiskAlert alert) {
        if (alert == null) {
            log.warn("Received null RiskAlert; skipping audit log write");
            return;
        }
        writeAuditEntry(formatEntry(alert));
    }

    private String formatEntry(RiskAlert alert) {
        return Instant.now() + " ALERT carrier=" + alert.getCarrierName() + System.lineSeparator();
    }

    private void writeAuditEntry(String entry) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                AUDIT_LOG_PATH,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            writer.write(entry);
        } catch (IOException e) {
            log.error("Failed to write audit log to [{}]: {}", AUDIT_LOG_PATH, e.getMessage());
        }
    }
}