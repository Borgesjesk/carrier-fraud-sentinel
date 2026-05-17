package com.carrierfraud;

import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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