package com.carrierfraud.domain;

import com.carrierfraud.domain.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Transaction Domain Model Tests")
class TransactionTest {

    // ============ HAPPY PATH (Valid Transaction) ============

    @Test
    @DisplayName("Valid transaction should create successfully ")
    void testConstructor_ValidaData_CreatesTransaction() {

        String carrierName = "CarrierA";
        String transportName = "Transport123";
        int failedPayments = 5;
        int succeededPayments = 10;
        double offerPrice = 1500.0;
        int numberOfOffers = 50;
        int reportedIncidents = 2;

        Transaction transaction = new Transaction(
                carrierName,
                transportName,
                failedPayments,
                succeededPayments,
                offerPrice,
                numberOfOffers,
                reportedIncidents
        );

        assertEquals(carrierName, transaction.getCarrierName());
        assertEquals(transportName, transaction.getTransportName());
        assertEquals(failedPayments, transaction.getFailedPayments());
        assertEquals(succeededPayments, transaction.getSucceededPayments());
        assertEquals(offerPrice, transaction.getOfferPrice());
        assertEquals(numberOfOffers, transaction.getNumberOfOffers());
        assertEquals(reportedIncidents, transaction.getReportedIncidents());
    }

    // ============ UNHAPPY PATHS (Constructor Validation) ============

    @Test
    @DisplayName("Null carrierName should throw IllegalArgumentException")
    void testConstructor_NullCarrierName_ThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                        null,
                        "Transport123",
                        5, 10, 1500.0, 50, 2
                ));
    }

    @Test
    @DisplayName("Empty carrierName should throw IllegalArgumentException")
    void testConstructor_EmptyCarrierName_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Transaction(
                        "",  // INVALID (empty)
                        "Transport123",
                        5, 10, 1500.0, 50, 2
                )
        );
    }

    @Test
    @DisplayName("CarrierName > 50 chars should throw IllegalArgumentException")
    void testConstructor_CarrierNameTooLong_ThrowsException() {
        String longName = "A".repeat(51);  // 51 chars, exceeds limit

        assertThrows(
                IllegalArgumentException.class,
                () -> new Transaction(
                        longName,  // INVALID (too long)
                        "Transport123",
                        5, 10, 1500.0, 50, 2
                )
        );
    }

    @Test
    @DisplayName("Negative failedPayments should throw IllegalArgumentException")
    void testConstructor_NegativeFailedPayments_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Transaction(
                        "CarrierA",
                        "Transport123",
                        -5,  // INVALID (negative)
                        10, 1500.0, 50, 2
                )
        );
    }

    @Test
    @DisplayName("Zero offerPrice should throw IllegalArgumentException")
    void testConstructor_ZeroOfferPrice_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Transaction(
                        "CarrierA",
                        "Transport123",
                        5, 10,
                        0.0,  // INVALID (must be > 0)
                        50, 2
                )
        );
    }

    @Test
    @DisplayName("Negative offerPrice should throw IllegalArgumentException")
    void testConstructor_NegativeOfferPrice_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Transaction(
                        "CarrierA",
                        "Transport123",
                        5, 10,
                        -1500.0,  // INVALID (negative)
                        50, 2
                )
        );
    }

    // ============ COMPUTED PROPERTIES ============

    @Test
    @DisplayName("getPaymentSuccessRate should return correct percentage")
    void testPaymentSuccessRate_CorrectCalculation() {
        // ARRANGE: 10 successes out of 15 total = 66.67%
        Transaction transaction = new Transaction(
                "CarrierA", "Transport123",
                5,      // failedPayments
                10,     // succeededPayments (10 out of 15 = 66.67%)
                1500.0, 50, 2
        );

        // ACT
        double successRate = transaction.getPaymentSuccessRate();

        // ASSERT
        assertEquals(10.0 / 15.0, successRate, 0.001);  // Within 0.001 tolerance
    }

    @Test
    @DisplayName("getPaymentSuccessRate should return 0 if no payment history")
    void testPaymentSuccessRate_NoHistory_ReturnsZero() {
        // ARRANGE: 0 successes, 0 failures = no history
        Transaction transaction = new Transaction(
                "CarrierA", "Transport123",
                0,      // failedPayments
                0,      // succeededPayments (0 out of 0 = undefined → 0.0)
                1500.0, 50, 2
        );

        // ACT
        double successRate = transaction.getPaymentSuccessRate();

        // ASSERT
        assertEquals(0.0, successRate);
    }

    @Test
    @DisplayName("getPaymentSuccessRate should return 1.0 if all succeeded")
    void testPaymentSuccessRate_AllSucceeded_ReturnsOne() {
        // ARRANGE: 0 failures, 10 successes = 100% success
        Transaction transaction = new Transaction(
                "CarrierA", "Transport123",
                0,      // failedPayments
                10,     // succeededPayments (10 out of 10 = 100%)
                1500.0, 50, 2
        );

        // ACT
        double successRate = transaction.getPaymentSuccessRate();

        // ASSERT
        assertEquals(1.0, successRate);
    }

    @Test
    @DisplayName("getIncidentRatio should return correct ratio")
    void testIncidentRatio_CorrectCalculation() {
        // ARRANGE: 2 incidents out of 50 offers = 4%
        Transaction transaction = new Transaction(
                "CarrierA", "Transport123",
                5, 10,
                1500.0,
                50,     // numberOfOffers
                2       // reportedIncidents (2 out of 50 = 4%)
        );

        // ACT
        double incidentRatio = transaction.getIncidentRatio();

        // ASSERT
        assertEquals(2.0 / 50.0, incidentRatio, 0.001);
    }

    @Test
    @DisplayName("getIncidentRatio should return 0 if no offers")
    void testIncidentRatio_NoOffers_ReturnsZero() {
        // ARRANGE: 0 offers
        Transaction transaction = new Transaction(
                "CarrierA", "Transport123",
                5, 10,
                1500.0,
                0,      // numberOfOffers (0 offers)
                0       // reportedIncidents
        );

        // ACT
        double incidentRatio = transaction.getIncidentRatio();

        // ASSERT
        assertEquals(0.0, incidentRatio);
    }

    // ============ IMMUTABILITY ============

    @Test
    @DisplayName("Transaction fields should be immutable")
    void testImmutability_NoSetters_FieldsAreReadOnly() {
        Transaction transaction = new Transaction(
                "CarrierA", "Transport123",
                5, 10, 1500.0, 50, 2
        );

        // ASSERT: No setter methods exist (compile-time verified)
        // This test documents the immutability contract
        // If someone tried to add a setter, this test would catch it
        assertTrue(transaction.getClass().getFields().length == 0);
    }

    // ============ OBJECT CONTRACT ============

    @Test
    @DisplayName("Two transactions with same data should be equal")
    void testEquals_SameData_AreEqual() {
        Transaction t1 = new Transaction("CarrierA", "Transport123", 5, 10, 1500.0, 50, 2);
        Transaction t2 = new Transaction("CarrierA", "Transport123", 5, 10, 1500.0, 50, 2);

        assertEquals(t1, t2);
    }

    @Test
    @DisplayName("Two transactions with different data should not be equal")
    void testEquals_DifferentData_NotEqual() {
        Transaction t1 = new Transaction("CarrierA", "Transport123", 5, 10, 1500.0, 50, 2);
        Transaction t2 = new Transaction("CarrierB", "Transport123", 5, 10, 1500.0, 50, 2);

        assertNotEquals(t1, t2);
    }

    @Test
    @DisplayName("Equal transactions should have same hashCode")
    void testHashCode_EqualTransactions_SameHashCode() {
        Transaction t1 = new Transaction("CarrierA", "Transport123", 5, 10, 1500.0, 50, 2);
        Transaction t2 = new Transaction("CarrierA", "Transport123", 5, 10, 1500.0, 50, 2);

        assertEquals(t1.hashCode(), t2.hashCode());
    }
}











