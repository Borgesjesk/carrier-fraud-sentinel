package com.carrierfraud.application.rules;

import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;

public class HighOfferCountRule implements StrategyRule {

    private final int maxNormalOffers;

    public HighOfferCountRule(int maxNormalOffers) {
        this.maxNormalOffers = maxNormalOffers;
    }

    @Override
    public double evaluate(Transaction transaction) {
        int numberOfOffers = transaction.getNumberOfOffers();
        if (numberOfOffers <= maxNormalOffers) return 0.0;
        if (numberOfOffers <= maxNormalOffers * 2) return 0.3;
        if (numberOfOffers <= maxNormalOffers * 3) return 0.6;
        return 1.0;
    }

    @Override
    public String name() {
        return "HighOfferCountRule";
    }
}
