package com.carrierfraud.application.rules;

import com.carrierfraud.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HighOfferCountRuleTest {

    private HighOfferCountRule highOfferCountRule;

    @BeforeEach
    void setUp() {
        highOfferCountRule = new HighOfferCountRule(100);
    }

    @Test
    void shouldReturnZeroScoreWhenOfferBelowOfferCountRule() {

        Transaction transaction = new Transaction("000AAA", "PT", 0, 60, 1600.0, 50, "None", 1, 1, true);

        assertEquals(0.0, highOfferCountRule.evaluate(transaction));
    }

    @Test
    void shouldReturnLowScoreWhenOfferSlightAboveOfferCountRule() {

        Transaction transaction = new Transaction("000AAA", "PT", 0, 60, 1600.0, 200, "None", 1, 1, true);

        assertEquals(0.3, highOfferCountRule.evaluate(transaction));
    }

    @Test
    void shouldReturnMediumScoreWhenOfferModeratelyAboveOfferCountRule() {

        Transaction transaction = new Transaction("000AAA", "PT", 0, 60, 1600.0, 300, "None", 1, 1, true);

        assertEquals(0.6, highOfferCountRule.evaluate(transaction));
    }

    @Test
    void shouldReturnMaxScoreWhenOfferFarAboveOfferCountRule() {

        Transaction transaction = new Transaction("000AAA", "PT", 0, 60, 1600.0, 500, "None", 1, 1, true);

        assertEquals(1.0, highOfferCountRule.evaluate(transaction));
    }
}

