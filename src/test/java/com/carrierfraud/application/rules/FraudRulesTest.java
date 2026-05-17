package com.carrierfraud.application.rules;

import com.carrierfraud.ComplaintAccumulationRule;
import com.carrierfraud.OfferPriceEscalationRule;
import com.carrierfraud.PaymentReconciliationRule;
import com.carrierfraud.domain.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Fraud Detection Rules - Threshold-Based Scoring")
class FraudRulesTest {

    private final PaymentReconciliationRule rule1 = new PaymentReconciliationRule();
    private final OfferPriceEscalationRule rule2 = new OfferPriceEscalationRule();
    private final ComplaintAccumulationRule rule3 = new ComplaintAccumulationRule();

    // ============ RULE 1: Payment Reconciliation ============

    @Test
    @DisplayName("Rule 1: 100% success rate = CLEAN (0.0)")
    void testRule1_AllPaid_ScoreZero() {
        Transaction t = new Transaction("GoodCarrier", "T1", 0, 50, 1500.0, 100, 0);
        assertEquals(0.0, rule1.evaluate(t));
    }

    @Test
    @DisplayName("Rule 1: 5% unpaid = CLEAN (0.0)")
    void testRule1_FivePercentUnpaid_ScoreZero() {
        Transaction t = new Transaction("OkCarrier", "T1", 1, 19, 1500.0, 50, 0);
        assertEquals(0.0, rule1.evaluate(t));
    }

    @Test
    @DisplayName("Rule 1: 20% unpaid = LOW (0.3)")
    void testRule1_TwentyPercentUnpaid_ScoreLow() {
        Transaction t = new Transaction("ConcerningCarrier", "T1", 2, 8, 1500.0, 50, 0);
        assertEquals(0.3, rule1.evaluate(t));
    }

    @Test
    @DisplayName("Rule 1: 40% unpaid = MEDIUM (0.6)")
    void testRule1_FortyPercentUnpaid_ScoreMedium() {
        Transaction t = new Transaction("ProblematicCarrier", "T1", 4, 6, 1500.0, 50, 0);
        assertEquals(0.6, rule1.evaluate(t));
    }

    @Test
    @DisplayName("Rule 1: 70% unpaid = HIGH (0.9)")
    void testRule1_SeventyPercentUnpaid_ScoreHigh() {
        Transaction t = new Transaction("HighRiskCarrier", "T1", 7, 3, 1500.0, 50, 0);
        assertEquals(0.9, rule1.evaluate(t));
    }

    @Test
    @DisplayName("Rule 1: 90% unpaid = CRITICAL (1.0)")
    void testRule1_NinetyPercentUnpaid_ScoreCritical() {
        Transaction t = new Transaction("FraudCarrier", "T1", 9, 1, 1500.0, 50, 0);
        assertEquals(1.0, rule1.evaluate(t));
    }

    @Test
    @DisplayName("Rule 1: No payment history = CLEAN (0.0)")
    void testRule1_NoHistory_ScoreZero() {
        Transaction t = new Transaction("NewCarrier", "T1", 0, 0, 1500.0, 0, 0);
        assertEquals(0.0, rule1.evaluate(t));
    }

    // ============ RULE 2: Price Escalation ============

    @Test
    @DisplayName("Rule 2: Market-rate price = CLEAN (0.0)")
    void testRule2_MarketRate_ScoreZero() {
        Transaction t = new Transaction("FairCarrier", "T1", 2, 8, 1500.0, 20, 1);
        assertEquals(0.0, rule2.evaluate(t));
    }

    @Test
    @DisplayName("Rule 2: Below market = CLEAN (0.0)")
    void testRule2_BelowMarket_ScoreZero() {
        Transaction t = new Transaction("CheapCarrier", "T1", 2, 8, 1200.0, 20, 1);
        assertEquals(0.0, rule2.evaluate(t));
    }

    @Test
    @DisplayName("Rule 2: 15% above baseline = LOW (0.2)")
    void testRule2_FifteenPercentAbove_ScoreLow() {
        Transaction t = new Transaction("SlightlyHigh", "T1", 2, 8, 1725.0, 20, 1);
        assertEquals(0.2, rule2.evaluate(t));
    }

    @Test
    @DisplayName("Rule 2: 35% above baseline = MEDIUM (0.5)")
    void testRule2_ThirtyFivePercentAbove_ScoreMedium() {
        Transaction t = new Transaction("SuspiciousPrice", "T1", 2, 8, 2025.0, 20, 1);
        assertEquals(0.5, rule2.evaluate(t));
    }

    @Test
    @DisplayName("Rule 2: 75% above baseline = HIGH (0.8)")
    void testRule2_SeventyFivePercentAbove_ScoreHigh() {
        Transaction t = new Transaction("HighPrice", "T1", 2, 8, 2625.0, 20, 1);
        assertEquals(0.8, rule2.evaluate(t));
    }

    @Test
    @DisplayName("Rule 2: 150% above baseline = CRITICAL (1.0)")
    void testRule2_OneHundredFiftyPercentAbove_ScoreCritical() {
        Transaction t = new Transaction("ExtremePrice", "T1", 2, 8, 3750.0, 20, 1);
        assertEquals(1.0, rule2.evaluate(t));
    }

    // ============ RULE 3: Complaint Accumulation ============

    @Test
    @DisplayName("Rule 3: 0 incidents = CLEAN (0.0)")
    void testRule3_NoIncidents_ScoreZero() {
        Transaction t = new Transaction("GoodCarrier", "T1", 2, 8, 1500.0, 50, 0);
        assertEquals(0.0, rule3.evaluate(t));
    }

    @Test
    @DisplayName("Rule 3: 5 incidents = CLEAN (0.0)")
    void testRule3_FiveIncidents_ScoreZero() {
        Transaction t = new Transaction("OkCarrier", "T1", 2, 8, 1500.0, 50, 5);
        assertEquals(0.0, rule3.evaluate(t));
    }

    @Test
    @DisplayName("Rule 3: 15 incidents = LOW (0.3)")
    void testRule3_FifteenIncidents_ScoreLow() {
        Transaction t = new Transaction("ConcerningCarrier", "T1", 2, 8, 1500.0, 50, 15);
        assertEquals(0.3, rule3.evaluate(t));
    }

    @Test
    @DisplayName("Rule 3: 25 incidents = MEDIUM (0.6)")
    void testRule3_TwentyFiveIncidents_ScoreMedium() {
        Transaction t = new Transaction("ProblematicCarrier", "T1", 2, 8, 1500.0, 50, 25);
        assertEquals(0.6, rule3.evaluate(t));
    }

    @Test
    @DisplayName("Rule 3: 40 incidents = HIGH (0.9)")
    void testRule3_FortyIncidents_ScoreHigh() {
        Transaction t = new Transaction("HighRiskCarrier", "T1", 2, 8, 1500.0, 50, 40);
        assertEquals(0.9, rule3.evaluate(t));
    }

    @Test
    @DisplayName("Rule 3: 55 incidents = CRITICAL (1.0)")
    void testRule3_FiftyFiveIncidents_ScoreCritical() {
        Transaction t = new Transaction("CriticalCarrier", "T1", 2, 8, 1500.0, 60, 55);
        assertEquals(1.0, rule3.evaluate(t));
    }

    // ============ COMBINED SCORING ============

    @Test
    @DisplayName("Clean carrier across all rules = 0.0")
    void testAllRules_CleanCarrier_ScoreZero() {
        Transaction t = new Transaction("ExcellentCarrier", "T1", 0, 50, 1500.0, 100, 0);
        double total = rule1.evaluate(t) + rule2.evaluate(t) + rule3.evaluate(t);
        assertEquals(0.0, total);
    }

    @Test
    @DisplayName("Fraudulent carrier triggers all rules = CRITICAL")
    void testAllRules_FraudulentCarrier_HighCombinedScore() {
        Transaction t = new Transaction("FraudCarrier", "T1", 9, 1, 3750.0, 60, 55);
        double total = rule1.evaluate(t) + rule2.evaluate(t) + rule3.evaluate(t);
        assertTrue(total >= 2.5, "Total score should be CRITICAL: " + total);
    }
}
