package com.carrierfraud.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TransactionValidatorTest verifies business rule validation.
 *
 * TESTING STRATEGY:
 * - Happy path: Valid business combinations
 * - Unhappy paths: All 5 business rules violated
 *
 * BUSINESS RULES TESTED:
 * 1. Payment History Ratio: Can't have 80%+ failures and 0 successes
 * 2. Incident Ratio: Can't have more incidents than offers
 * 3. Offer Price Consistency: Price must be in market range
 * 4. New Carrier Pattern: Brand new with incidents but no offers = corruption
 * 5. Suspended Carrier Pattern: 30+ failures AND 30+ incidents = danger zone
 */
@DisplayName("TransactionValidator Business Rule Tests")
class TransactionValidatorTest {

    private final TransactionValidator validator = new TransactionValidator();

    // ============ HAPPY PATHS (Valid Business Data) ============

    @Test
    @DisplayName("Valid carrier data should pass all validations")
    void testValidate_ValidData_DoesNotThrow() {
        Transaction transaction = new Transaction(
            "CarrierA",
            "Transport123",
            5,       // 5 failures
            10,      // 10 successes (67% success rate, OK)
            1500.0,
            50,
            2
        );

        // Should not throw
        assertDoesNotThrow(() -> validator.validate(transaction));
    }

    @Test
    @DisplayName("New carrier with no history should pass validation")
    void testValidate_NewCarrier_DoesNotThrow() {
        Transaction transaction = new Transaction(
            "NewCarrier",
            "Transport123",
            0,       // No failures
            0,       // No successes (new)
            1500.0,
            0,       // No offers yet
            0        // No incidents
        );

        assertDoesNotThrow(() -> validator.validate(transaction));
    }

    @Test
    @DisplayName("Carrier with all successes should pass validation")
    void testValidate_AllSuccesses_DoesNotThrow() {
        Transaction transaction = new Transaction(
            "GoodCarrier",
            "Transport123",
            0,       // 0 failures
            50,      // 50 successes (100% success rate)
            1500.0,
            100,
            0        // No incidents
        );

        assertDoesNotThrow(() -> validator.validate(transaction));
    }

    // ============ RULE 1: Payment History Ratio ============

    @Test
    @DisplayName("RULE 1: 100 failures + 0 successes should throw BusinessRuleException")
    void testValidate_Rule1_HighFailureRate_ThrowsException() {
        Transaction transaction = new Transaction(
            "BadCarrier",
            "Transport123",
            100,     // 100 failures
            0,       // 0 successes (IMPOSSIBLE: 100% failure rate)
            1500.0,
            100,
            5
        );

        assertThrows(
            BusinessRuleException.class,
            () -> validator.validate(transaction),
            "Should catch impossible payment pattern"
        );
    }

    @Test
    @DisplayName("RULE 1: 50 failures + 1 success should pass (67% fail rate)")
    void testValidate_Rule1_MediumFailureRate_DoesNotThrow() {
        Transaction transaction = new Transaction(
            "AverageCarrier",
            "Transport123",
            50,      // 50 failures
            1,       // 1 success (98% failure rate, high but possible)
            1500.0,
            100,
            10
        );

        // Should not throw (above 80% threshold but acceptable with minimum success)
        assertDoesNotThrow(() -> validator.validate(transaction));
    }

    @Test
    @DisplayName("RULE 1: 50 attempts, 49 failures should throw (no learning pattern)")
    void testValidate_Rule1_NoLearningPattern_ThrowsException() {
        Transaction transaction = new Transaction(
            "BadCarrier",
            "Transport123",
            49,      // 49 failures
            1,       // 1 success (out of 50 = 98% fail rate)
            1500.0,
            50,      // 50+ attempts = should show learning
            15
        );

        assertThrows(
            BusinessRuleException.class,
            () -> validator.validate(transaction),
            "After 50+ attempts, carrier should show improvement"
        );
    }

    // ============ RULE 2: Incident Ratio ============

    @Test
    @DisplayName("RULE 2: More incidents than offers should throw BusinessRuleException")
    void testValidate_Rule2_IncidentsExceedOffers_ThrowsException() {
        Transaction transaction = new Transaction(
            "BadCarrier",
            "Transport123",
            5, 10,
            1500.0,
            5,       // 5 offers
            100      // 100 incidents (IMPOSSIBLE: more incidents than offers)
        );

        assertThrows(
            BusinessRuleException.class,
            () -> validator.validate(transaction),
            "Can't have more incidents than offers"
        );
    }

    @Test
    @DisplayName("RULE 2: Incidents equal to offers should pass")
    void testValidate_Rule2_IncidentsEqualOffers_DoesNotThrow() {
        Transaction transaction = new Transaction(
            "BadCarrier",
            "Transport123",
            5, 10,
            1500.0,
            10,      // 10 offers
            10       // 10 incidents (100% incident rate, concerning but possible)
        );

        // High incident rate but not mathematically impossible
        assertDoesNotThrow(() -> validator.validate(transaction));
    }

    // ============ RULE 3: Offer Price Consistency ============

    @Test
    @DisplayName("RULE 3: Suspiciously low offer price should throw BusinessRuleException")
    void testValidate_Rule3_PriceTooLow_ThrowsException() {
        Transaction transaction = new Transaction(
            "BadCarrier",
            "Transport123",
            5, 10,
            10.0,    // €10 per offer (IMPOSSIBLE: below market minimum of €50)
            100,
            5
        );

        assertThrows(
            BusinessRuleException.class,
            () -> validator.validate(transaction),
            "Price below market minimum"
        );
    }

    @Test
    @DisplayName("RULE 3: Market-rate offer price should pass")
    void testValidate_Rule3_PriceInRange_DoesNotThrow() {
        Transaction transaction = new Transaction(
            "GoodCarrier",
            "Transport123",
            5, 10,
            1500.0,  // €1500 per offer (market rate)
            100,
            2
        );

        assertDoesNotThrow(() -> validator.validate(transaction));
    }

    @Test
    @DisplayName("RULE 3: Premium offer price should pass")
    void testValidate_Rule3_PremiumPrice_DoesNotThrow() {
        Transaction transaction = new Transaction(
            "PremiumCarrier",
            "Transport123",
            2, 20,
            50000.0, // €50,000 per offer (emergency shipment, valid)
            50,
            1
        );

        assertDoesNotThrow(() -> validator.validate(transaction));
    }

    // ============ RULE 4: New Carrier Grace Period ============

    @Test
    @DisplayName("RULE 4: Brand new carrier with incidents but no offers should throw")
    void testValidate_Rule4_PhantomCarrier_ThrowsException() {
        Transaction transaction = new Transaction(
            "PhantomCarrier",
            "Transport123",
            0,       // 0 failed payments (no history)
            0,       // 0 succeeded payments (no history)
            1500.0,
            0,       // 0 offers (brand new)
            5        // 5 incidents (DATA CORRUPTION: incidents without offers)
        );

        assertThrows(
            BusinessRuleException.class,
            () -> validator.validate(transaction),
            "New carrier can't have incidents without offers"
        );
    }

    @Test
    @DisplayName("RULE 4: New carrier with no incidents should pass")
    void testValidate_Rule4_NewCarrierClean_DoesNotThrow() {
        Transaction transaction = new Transaction(
            "NewCarrier",
            "Transport123",
            0, 0,    // No payment history yet
            1500.0,
            0,       // No offers yet
            0        // No incidents
        );

        assertDoesNotThrow(() -> validator.validate(transaction));
    }

    // ============ RULE 5: Suspended Carrier Pattern ============

    @Test
    @DisplayName("RULE 5: 30+ failures AND 30+ incidents should throw (danger zone)")
    void testValidate_Rule5_DangerZone_ThrowsException() {
        Transaction transaction = new Transaction(
            "SuspendedCarrier",
            "Transport123",
            30,      // 30+ failures
            10,
            1500.0,
            100,
            30       // 30+ incidents (DANGER ZONE)
        );

        assertThrows(
            BusinessRuleException.class,
            () -> validator.validate(transaction),
            "High failures + high incidents = should be suspended"
        );
    }

    @Test
    @DisplayName("RULE 5: 30+ failures but low incidents should pass")
    void testValidate_Rule5_OnlyFailures_DoesNotThrow() {
        Transaction transaction = new Transaction(
            "UnluckyCarrier",
            "Transport123",
            30,      // 30+ failures
            5,
            1500.0,
            100,
            2        // Low incidents (not danger zone)
        );

        assertDoesNotThrow(() -> validator.validate(transaction));
    }

    @Test
    @DisplayName("RULE 5: 30+ incidents but low failures should pass")
    void testValidate_Rule5_OnlyIncidents_DoesNotThrow() {
        Transaction transaction = new Transaction(
            "RiskyCarrier",
            "Transport123",
            5,
            20,
            1500.0,
            100,
            30       // 30+ incidents
        );

        assertDoesNotThrow(() -> validator.validate(transaction));
    }
}
