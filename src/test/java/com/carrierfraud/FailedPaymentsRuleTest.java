package com.carrierfraud;

import com.carrierfraud.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FailedPaymentsRuleTest {

    private FailedPaymentsRule failedPaymentsRule;

    @BeforeEach
    void setUp() {
        failedPaymentsRule = new FailedPaymentsRule();
    }

    @Test
    void shouldReturnZeroWhenNoFailedPayments() {

        Transaction transaction1 = new Transaction("000AAA", "PT", 0, 60, 1000.0, 150, "Forgot to pay at the right date", 1, 1, false);

        assertEquals(0.0, failedPaymentsRule.evaluate(transaction1));
    }

    @Test
    void shouldReturnMaxScoreWhenFailedPaymentsAboveFive() {

        Transaction transaction2 = new Transaction("123ABC", "ES", 8, 60, 2000.0, 400, "There was no problem at all", 2, 2, true);

        assertEquals(1.0, failedPaymentsRule.evaluate(transaction2));
    }

    @Test
    void shouldReturnLowScoreWhenOneFailedPayment() {

        Transaction transaction = new Transaction("111BBB", "IT", 1, 60, 700.0, 100, "None", 2, 2, true);

        assertEquals(0.4, failedPaymentsRule.evaluate(transaction));
    }

    @Test
    void shouldReturnMediumScoreWhenThreeFailedPayments() {

        Transaction transaction = new Transaction("222CCC", "ZH", 3, 60, 600.0, 200, "None", 2, 2, true);

        assertEquals(0.6, failedPaymentsRule.evaluate(transaction));
    }

    @Test
    void shouldReturnHighScoreWhenFiveFailedPayments() {

        Transaction transaction = new Transaction("333DDD", "BR", 5, 60, 500.0, 50, "None", 2, 2, true);

        assertEquals(0.8, failedPaymentsRule.evaluate(transaction));
    }
}
