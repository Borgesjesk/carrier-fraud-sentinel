package com.carrierfraud.application;

import com.carrierfraud.domain.Transaction;
import com.carrierfraud.domain.RiskAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class AlertGeneratorScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertGeneratorScheduler.class);
    private static final Random RANDOM = new Random();

    private static final List<String> CARRIER_NAMES = List.of(
            "TransEurope SA", "FastCargo Ltd", "GlobalHaul GmbH", "IberiaTrans SL",
            "NorthLink Logistics", "SouthRoute Express", "AtlanticShip Corp", "BalticFreight OU",
            "MediterraneanTrans SPA", "AlpineHaul AG", "NordicCarrier AS", "CentralHaul Kft",
            "SuspectTransport SL", "RiskCargo Ltd", "FraudulentHaul SA", "ShadyLogistics LLC"
    );

    private static final List<String> TRANSPORT_NAMES = List.of(
            "Volvo FH", "Scania S", "Mercedes Actros", "MAN TGX", "DAF XF",
            "Iveco Stralis", "Renault T", "Peterbilt 579"
    );

    private final FraudDetectionService fraudDetectionService;

    @Value("${app.alert-generator.enabled:true}")
    private boolean enabled;

    public AlertGeneratorScheduler(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @Scheduled(fixedDelayString = "${app.alert-generator.interval-ms:60000}")
    public void generateRandomTransaction() {
        if (!enabled) return;
        try {
            Transaction t = createTransaction();
            RiskAlert alert = fraudDetectionService.analyseAndTag(t, "SIEM_SCHEDULER");
            if (alert != null) {
                
                
                log.info("SIEM auto-generated alert: carrier={} severity={} rule={}",
                        alert.getCarrierName(), alert.getSeverity(), alert.getTriggeredRuleNames());
            }
        } catch (Exception e) {
            log.warn("Alert generator failed: {}", e.getMessage());
        }
    }

    private Transaction createTransaction() {
        String carrier = CARRIER_NAMES.get(RANDOM.nextInt(CARRIER_NAMES.size()));
        String transport = TRANSPORT_NAMES.get(RANDOM.nextInt(TRANSPORT_NAMES.size()));
        boolean makeSuspicious = RANDOM.nextInt(100) < 40;

        if (makeSuspicious) {
            int failedPayments = 5 + RANDOM.nextInt(15);
            int succeeded = 1 + RANDOM.nextInt(5);
            double offerPrice = 2000 + RANDOM.nextInt(6000);
            int offers = 1 + RANDOM.nextInt(10);
            int incidents = 2 + RANDOM.nextInt(8);
            return new Transaction(carrier, transport, failedPayments, succeeded, offerPrice, offers, incidents);
        } else {
            int failedPayments = RANDOM.nextInt(3);
            int succeeded = 20 + RANDOM.nextInt(50);
            double offerPrice = 1000 + RANDOM.nextInt(1000);
            int offers = 5 + RANDOM.nextInt(20);
            int incidents = RANDOM.nextInt(2);
            return new Transaction(carrier, transport, failedPayments, succeeded, offerPrice, offers, incidents);
        }
    }
}
