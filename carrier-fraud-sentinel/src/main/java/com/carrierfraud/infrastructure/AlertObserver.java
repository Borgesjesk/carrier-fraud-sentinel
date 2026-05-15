package com.carrierfraud.infrastructure;

import com.carrierfraud.domain.RiskAlert;

/**
 * AlertObserver defines the notification contract.
 *
 * OBSERVER PATTERN:
 * - When alert fires, notify all observers
 * - Decouples alert creation from notifications
 * - Easy to add new notification channels
 *
 * IMPLEMENTATIONS:
 * 1. ConsoleAlertObserver - prints to stdout (development)
 * 2. AuditLogAlertObserver - writes to audit log (compliance)
 * 3. EmailAlertObserver - sends email notification (production)
 * 4. SlackAlertObserver - posts to Slack (ops team)
 * 5. DatabaseHistoryObserver - saves to audit trail
 *
 * HOW IT WORKS:
 * 1. FraudDetectionService creates RiskAlert
 * 2. Loops through all observers:
 *    for (AlertObserver obs : observers) {
 *        obs.notify(alert);
 *    }
 * 3. Each observer handles notification independently
 *
 * BENEFIT:
 * - Add new observer without changing service
 * - Observers run independently (one failure doesn't break others)
 * - Clear separation of concerns
 */
public interface AlertObserver {

    /**
     * Notify this observer that an alert has fired.
     *
     * @param alert the alert that fired
     */
    void notify(RiskAlert alert);
}
