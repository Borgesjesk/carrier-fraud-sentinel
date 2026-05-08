package com.carrierfraud.application.rules;

import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;
import org.springframework.stereotype.Component;

@Component
public class MarketPriceRule implements StrategyRule {

    private double marketAveragePrice;

    public MarketPriceRule() {
        this.marketAveragePrice = 1500.0;
    }

    @Override
    public double evaluate(Transaction transaction) {
        double offerPrice = transaction.getOfferPrice();
        double difference = offerPrice - marketAveragePrice;
        double percentage = difference / marketAveragePrice;
        if (percentage <= 0) return 0.0;
        if (percentage <= 0.2) return 0.3;
        if (percentage <= 0.5) return 0.6;
        return 1.0;
    }

    @Override
    public String name() {
        return "MarketPriceRule";
    }
}
