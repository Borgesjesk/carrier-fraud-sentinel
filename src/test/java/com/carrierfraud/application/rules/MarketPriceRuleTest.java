package com.carrierfraud.application.rules;

import com.carrierfraud.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketPriceRuleTest {

    private MarketPriceRule marketPriceRule;

    @BeforeEach
    void setUp() {
        marketPriceRule = new MarketPriceRule(1000.0);
    }

    @Test
    void shouldReturnZeroWhenOfferBelowMarketPrice() {

        Transaction transaction = new Transaction("000AAA", "PT", 0, 60, 900.0, 150, "None", 1, 1, true);

        assertEquals(0.0, marketPriceRule.evaluate(transaction));
    }

    @Test
    void shouldReturnLowScoreWhenOfferSlightAboveMarketPrice() {

        Transaction transaction = new Transaction("000AAA", "PT", 0, 60, 1100.0, 150, "None", 1, 1, true);

        assertEquals(0.3, marketPriceRule.evaluate(transaction));

    }

    @Test
    void shouldReturnMediumScoreWhenOfferModeratelyAboveMarketPrice() {

        Transaction transaction = new Transaction("000AAA", "PT", 0, 60, 1300.0, 150, "None", 1, 1, true);

        assertEquals(0.6, marketPriceRule.evaluate(transaction));
    }

    @Test
    void shouldReturnMaxScoreWhenOfferFarAboveMarketPrice() {

        Transaction transaction = new Transaction("000AAA", "PT", 0, 60, 1600.0, 150, "None", 1, 1, true);

        assertEquals(1.0, marketPriceRule.evaluate(transaction));
    }
}
