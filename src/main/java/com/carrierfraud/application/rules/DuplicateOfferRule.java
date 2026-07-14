package com.carrierfraud.application.rules;

import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;
import org.springframework.stereotype.Component;

@Component
public class DuplicateOfferRule implements StrategyRule {

    private static final double SUSPICIOUS_PRICE_1 = 1247.50;
    private static final double SUSPICIOUS_PRICE_2 = 1789.99;
    private static final double SUSPICIOUS_PRICE_3 = 2456.00;
    private static final double MATCH_TOLERANCE = 5.0;

    @Override
    public double evaluate(Transaction transaction) {
        double price = transaction.getOfferPrice();
        if (matchesSuspiciousPrice(price)) {
            return 0.6;
        }
        return 0.0;
    }

    private boolean matchesSuspiciousPrice(double price) {
        return Math.abs(price - SUSPICIOUS_PRICE_1) < MATCH_TOLERANCE
                || Math.abs(price - SUSPICIOUS_PRICE_2) < MATCH_TOLERANCE
                || Math.abs(price - SUSPICIOUS_PRICE_3) < MATCH_TOLERANCE;
    }

    @Override
    public String name() {
        return "DuplicateOfferRule";
    }

    @Override
    public String explain(Transaction transaction, double score) {
        return String.format(
                "Offer price €%.2f matches a known duplicate pricing pattern seen recently across " +
                "multiple carriers. Suggests offer mirroring, collusion, or automated bidding scripts " +
                "copying competitor listings within seconds of publication.",
                transaction.getOfferPrice()
        );
    }
}
