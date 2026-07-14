package com.carrierfraud.application.rules;

import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;
import org.springframework.stereotype.Component;

@Component
public class OfferPriceEscalationRule implements StrategyRule {

    private static final double MARKET_BASELINE = 1500.0;
    private static final double THRESHOLD_CLEAN = 0.10;
    private static final double THRESHOLD_LOW = 0.20;
    private static final double THRESHOLD_MEDIUM = 0.50;
    private static final double THRESHOLD_HIGH = 1.00;

    @Override
    public double evaluate(Transaction transaction) {
        double offerPrice = transaction.getOfferPrice();
        double deviation = (offerPrice - MARKET_BASELINE) / MARKET_BASELINE;

        if (deviation <= 0) {
            return 0.0;
        }

        if (deviation <= THRESHOLD_CLEAN) {
            return 0.0;
        } else if (deviation <= THRESHOLD_LOW) {
            return 0.2;
        } else if (deviation <= THRESHOLD_MEDIUM) {
            return 0.5;
        } else if (deviation <= THRESHOLD_HIGH) {
            return 0.8;
        } else {
            return 1.0;
        }
    }

    @Override
    public String name() {
        return "OfferPriceEscalationRule";
    }

    @Override
    public String explain(Transaction transaction, double score) {
        double offerPrice = transaction.getOfferPrice();
        double deviation = ((offerPrice - MARKET_BASELINE) / MARKET_BASELINE) * 100;
        return String.format(
                "Offer price €%.2f is %.1f%% above market baseline of €%.2f. " +
                        "Deviations of over 50%% typically indicate carriers testing shipper acceptance " +
                        "of inflated rates or attempting to lock in above-market contracts before dispute.",
                offerPrice, deviation, MARKET_BASELINE
        );
    }
}