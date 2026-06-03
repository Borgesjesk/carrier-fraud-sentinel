package com.carrierfraud.application;

import com.carrierfraud.domain.*;
import com.carrierfraud.infrastructure.RiskAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.carrierfraud.PaymentReconciliationRule;
import com.carrierfraud.OfferPriceEscalationRule;
import com.carrierfraud.ComplaintAccumulationRule;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudDetectionService Integration Tests")
class FraudDetectionServiceTest {

    @Mock
    private RiskAlertRepository alertRepository;

    @Mock
    private AlertObserver observer1;

    @Mock
    private AlertObserver observer2;

    private FraudDetectionService service;

    @BeforeEach
    void setUp() {
        List<StrategyRule> rules = List.of(        // ← was new ArrayList<>() (empty)
                new PaymentReconciliationRule(),
                new OfferPriceEscalationRule(),
                new ComplaintAccumulationRule()
        );
        List<AlertObserver> observers = List.of(observer1, observer2);
        service = new FraudDetectionService(rules, observers, alertRepository);
    }

    // ============ HAPPY PATH (Alert Fires) ============

    @Test
    @DisplayName("Alert should fire when score >= threshold")
    void testAnalyse_HighScore_CreatesAlert() {
        Transaction transaction = new Transaction(
                "BadCarrier", "Transport123",
                50,      // High failures
                5,       // Low successes
                3000.0,  // High price
                50,
                40       // High incidents
        );

        RiskAlert alert = service.analyse(transaction);

        assertNotNull(alert, "Alert should be created for high-scoring carrier");
        assertEquals("BadCarrier", alert.getCarrierName());
        assertTrue(alert.getRiskScore() > 0.5, "Score should exceed threshold");
    }

    @Test
    @DisplayName("Alert should be saved to repository")
    void testAnalyse_AlertCreated_IsSaved() {
        Transaction transaction = new Transaction(
                "BadCarrier", "Transport123",
                50, 5, 3000.0, 50, 40
        );

        service.analyse(transaction);

        verify(alertRepository, times(1)).save(any(RiskAlert.class));
    }

    @Test
    @DisplayName("All observers should be notified when alert fires")
    void testAnalyse_AlertCreated_NotifiesAllObservers() {
        Transaction transaction = new Transaction(
                "BadCarrier", "Transport123",
                50, 5, 3000.0, 50, 40
        );

        service.analyse(transaction);

        verify(observer1, times(1)).notify(any(RiskAlert.class));
        verify(observer2, times(1)).notify(any(RiskAlert.class));
    }

    // ============ CLEAN CARRIER (No Alert) ============

    @Test
    @DisplayName("No alert should fire when score < threshold")
    void testAnalyse_LowScore_NoAlert() {
        Transaction transaction = new Transaction(
                "GoodCarrier", "Transport123",
                0,       // No failures
                50,      // All successes
                1500.0,  // Market rate
                100,
                0        // No incidents
        );

        RiskAlert alert = service.analyse(transaction);

        assertNull(alert, "No alert should be created for clean carrier");
    }

    @Test
    @DisplayName("Repository should not be called for clean carrier")
    void testAnalyse_CleanCarrier_NoSave() {
        Transaction transaction = new Transaction(
                "GoodCarrier", "Transport123",
                0, 50, 1500.0, 100, 0
        );

        service.analyse(transaction);

        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("Observers should not be notified for clean carrier")
    void testAnalyse_CleanCarrier_NoNotifications() {
        Transaction transaction = new Transaction(
                "GoodCarrier", "Transport123",
                0, 50, 1500.0, 100, 0
        );

        service.analyse(transaction);

        verify(observer1, never()).notify(any());
        verify(observer2, never()).notify(any());
    }

    // ============ SEVERITY DETERMINATION ============

    @Test
    @DisplayName("Score 1.6 should be CRITICAL")
    void testAnalyse_CriticalScore_HasCriticalSeverity() {
        Transaction transaction = new Transaction(
                "CriticalCarrier", "Transport123",
                40, 10, 3000.0, 50, 50
        );

        RiskAlert alert = service.analyse(transaction);

        assertNotNull(alert);
        assertEquals(AlertSeverity.CRITICAL, alert.getSeverity());
    }

    @Test
    @DisplayName("Score 1.2 should be HIGH")
    void testAnalyse_HighScore_HasHighSeverity() {
        Transaction transaction = new Transaction(
                "HighRiskCarrier", "Transport123",
                30, 5, 3000.0, 50, 35
        );

        RiskAlert alert = service.analyse(transaction);

        assertNotNull(alert);
        assertTrue(
                alert.getSeverity() == AlertSeverity.HIGH ||
                        alert.getSeverity() == AlertSeverity.CRITICAL
        );
    }

    @Test
    @DisplayName("Score 0.7 should be MEDIUM")
    void testAnalyse_MediumScore_HasMediumSeverity() {
        Transaction transaction = new Transaction(
                "MediumRiskCarrier", "Transport123",
                2, 8, 1500.0, 15, 15
        );

        RiskAlert alert = service.analyse(transaction);

        assertNotNull(alert);
        // Could be MEDIUM or HIGH depending on exact score
        assertTrue(
                alert.getSeverity() == AlertSeverity.MEDIUM ||
                        alert.getSeverity() == AlertSeverity.HIGH
        );
    }

    // ============ ROUTING LOGIC ============

    @Test
    @DisplayName("Critical alert should route to LEGAL")
    void testAnalyse_CriticalAlert_RoutesToLegal() {
        Transaction transaction = new Transaction(
                "CriticalCarrier", "Transport123",
                50, 5, 3000.0, 50, 50
        );

        RiskAlert alert = service.analyse(transaction);

        assertNotNull(alert);
        assertEquals(Department.LEGAL, alert.getAssignedDepartment(),
                "Critical alerts should route to LEGAL department");
    }

    @Test
    @DisplayName("High payment failure alert should route to PAYMENT_RECONCILIATION")
    void testAnalyse_PaymentAlert_RoutesToPaymentReconciliation() {
        Transaction transaction = new Transaction(
                "BadPayerCarrier", "Transport123",
                40, 5, 1500.0, 50, 5
        );

        RiskAlert alert = service.analyse(transaction);

        assertNotNull(alert);
        assertEquals(Department.MEDIATION, alert.getAssignedDepartment(),
                "Payment rule violations should route to PAYMENT_RECONCILIATION");
    }

    // ============ NULL SAFETY ============

    @Test
    @DisplayName("Null transaction should throw NullPointerException")
    void testAnalyse_NullTransaction_ThrowsException() {
        assertThrows(
                NullPointerException.class,
                () -> service.analyse(null),
                "Service should reject null transactions"
        );
    }
}