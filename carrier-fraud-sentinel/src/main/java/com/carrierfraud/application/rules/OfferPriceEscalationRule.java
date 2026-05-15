package com.carrierfraud.application.rules;

import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RULE 2: Offer Price Escalation Rule
 *
 * THRESHOLD-BASED SCORING:
 * - 0-10% above baseline  = 0.0 (CLEAN: normal market variation)
 * - 10-20% above baseline = 0.2 (LOW: slightly above market)
 * - 20-50% above baseline = 0.5 (MEDIUM: suspicious pricing)
 * - 50-100% above baseline = 0.8 (HIGH: price manipulation)
 * - 100%+ above baseline  = 1.0 (CRITICAL: extreme escalation)
 *
 * BUSINESS CONTEXT:
 * Carriers who see a €1500 offer, accept it, then escalate
 * to €3000 after transport is in progress.
 * Market baseline is configurable (default €1500).
 *
 * FUTURE: Dynamic baseline from platform average (MarketPriceService).
 */
@Component
public class OfferPriceEscalationRule implements StrategyRule {

    @Value("${fraud.rule.price.baseline:1500.0}")
    private double marketBaseline;

    @Value("${fraud.rule.price.threshold.clean:0.10}")
    private double thresholdClean;

    @Value("${fraud.rule.price.threshold.low:0.20}")
    private double thresholdLow;

    @Value("${fraud.rule.price.threshold.medium:0.50}")
    private double thresholdMedium;

    @Value("${fraud.rule.price.threshold.high:1.00}")
    private double thresholdHigh;

    @Override
    public double evaluate(Transaction transaction) {
        double offerPrice = transaction.getOfferPrice();
        double deviation = (offerPrice - marketBaseline) / marketBaseline;

        if (deviation <= 0) {
            return 0.0;
        }

        if (deviation <= thresholdClean) {
            return 0.0;
        } else if (deviation <= thresholdLow) {
            return 0.2;
        } else if (deviation <= thresholdMedium) {
            return 0.5;
        } else if (deviation <= thresholdHigh) {
            return 0.8;
        } else {
            return 1.0;
        }
    }

    @Override
    public String name() {
        return "OfferPriceEscalationRule";
    }
}
