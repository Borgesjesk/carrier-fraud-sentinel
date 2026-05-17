package com.carrierfraud;

import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;
import org.springframework.stereotype.Component;

@Component
public class HighOfferCountRule implements StrategyRule {

    private int maxNormalOffers;

    public HighOfferCountRule() {
        this.maxNormalOffers = 200;
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
