package com.carrierfraud.infrastructure;

import com.carrierfraud.application.AlertObserver;
import com.carrierfraud.domain.RiskAlert;
import org.springframework.stereotype.Component;

@Component
public class DatabaseAlertObserver implements AlertObserver {


    @Override
    public void notify(RiskAlert alert) {
        System.out.println("💾 Saving alert to database: " + alert.getCarrierName());
    }
}
