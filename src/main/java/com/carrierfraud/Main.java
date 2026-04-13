package com.carrierfraud;

import com.carrierfraud.application.FraudDetectionService;
import com.carrierfraud.application.RiskScoreEvaluator;
import com.carrierfraud.application.rules.FailedPaymentsRule;
import com.carrierfraud.application.rules.HighOfferCountRule;
import com.carrierfraud.application.rules.MarketPriceRule;
import com.carrierfraud.domain.RiskAlert;
import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;

import java.util.List;

public class Main {
    public static void main(String[] args) {

        FailedPaymentsRule failedPaymentsRule = new FailedPaymentsRule();

        MarketPriceRule marketPriceRule = new MarketPriceRule(1500.0);

        HighOfferCountRule highOfferCountRule = new HighOfferCountRule(200);

        List<StrategyRule> rules = List.of(failedPaymentsRule, marketPriceRule, highOfferCountRule);

        FraudDetectionService fraudDetectionService = new FraudDetectionService(rules, 0.5);

        Transaction transaction = new Transaction("WWW123", "PT", 5, 20, 3000.0, 500, "It was the drivers fault", 2, 10, true);

        RiskAlert alert = fraudDetectionService.analyse(transaction);
        if (alert != null) {
            System.out.println("===🚨 FRAUD ALERT DETECTED 🚨===");
            System.out.println("Carrier: " + alert.getCarrierName());
            System.out.println("Risk Score: " + alert.getRiskScore());
            System.out.println("Urgency: " + RiskScoreEvaluator.evaluate(alert.getRiskScore()));
            System.out.println("Date/Time: " + alert.getAlertDateTime());
            System.out.println("Status: " + alert.getAlertStatus());
        }
    }
}
