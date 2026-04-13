package com.carrierfraud.infrastructure;

import com.carrierfraud.application.AlertObserver;
import com.carrierfraud.domain.RiskAlert;

import java.io.FileWriter;
import java.io.IOException;

public class AuditLogObserver implements AlertObserver {


    @Override
    public void notify(RiskAlert alert) {
        try (
            FileWriter writer = new FileWriter("audit.log", true)) {
            writer.write("ALERT: " + alert.getCarrierName() + "\n");
        } catch (IOException e) {
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }
}
