package com.carrierfraud.application.rules;

import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;
import org.springframework.stereotype.Component;

@Component
public class RapidChangeRule implements StrategyRule {

    private static final int NORMAL_OFFERS = 15;
    private static final int SUSPICIOUS_OFFERS = 25;
    private static final int CRITICAL_OFFERS = 40;

    @Override
    public double evaluate(Transaction transaction) {
        int offers = transaction.getNumberOfOffers();
        if (offers <= NORMAL_OFFERS) {
            return 0.0;
        } else if (offers <= SUSPICIOUS_OFFERS) {
            return 0.3;
        } else if (offers <= CRITICAL_OFFERS) {
            return 0.7;
        } else {
            return 1.0;
        }
    }

    @Override
    public String name() {
        return "RapidChangeRule";
    }

    @Override
    public String explain(Transaction transaction, double score) {
        int offers = transaction.getNumberOfOffers();
        return String.format(
                "Carrier has posted %d offers in the current window. Unusually high offer counts " +
                "may indicate rapid price probing, market dumping to squeeze competitors, or " +
                "automated flooding — all patterns associated with predatory or manipulative " +
                "pricing behavior.",
                offers
        );
    }
}
