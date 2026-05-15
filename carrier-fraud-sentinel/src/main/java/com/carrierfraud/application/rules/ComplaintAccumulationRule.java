package com.carrierfraud.application.rules;

import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RULE 3: Complaint Accumulation Rule
 *
 * THRESHOLD-BASED SCORING:
 * - 0-10 incidents  = 0.0 (CLEAN: normal operational disputes)
 * - 10-20 incidents = 0.3 (LOW: pattern emerging)
 * - 20-30 incidents = 0.6 (MEDIUM: systematic issue)
 * - 30-50 incidents = 0.9 (HIGH: carrier causing damage)
 * - 50+ incidents   = 1.0 (CRITICAL: immediate suspension)
 *
 * BUSINESS CONTEXT (From Alpega/WTRANSNET):
 * Complaints filed by transportistas with CMR + ALBARAN + evidence.
 * Types: OPEN_CASE, ACCIDENT, COMMERCIAL_DISPUTE, INSURANCE, REVIEWING.
 *
 * MVP: Uses reportedIncidents as proxy count.
 * FUTURE: Full List<Complaint> with time-window calculations:
 *   - 5+ open cases simultaneously = ALERT
 *   - 10+ mediated cases/week = ALERT
 *   - 20+ cases/month = ALERT
 *   - 2+ accidents/month = ALERT
 *   - 3+ unresolved disputes = CRITICAL
 */
@Component
public class ComplaintAccumulationRule implements StrategyRule {

    @Value("${fraud.rule.complaint.threshold.clean:10}")
    private int thresholdClean;

    @Value("${fraud.rule.complaint.threshold.low:20}")
    private int thresholdLow;

    @Value("${fraud.rule.complaint.threshold.medium:30}")
    private int thresholdMedium;

    @Value("${fraud.rule.complaint.threshold.high:50}")
    private int thresholdHigh;

    @Override
    public double evaluate(Transaction transaction) {
        int incidentCount = transaction.getReportedIncidents();

        if (incidentCount <= thresholdClean) {
            return 0.0;
        } else if (incidentCount <= thresholdLow) {
            return 0.3;
        } else if (incidentCount <= thresholdMedium) {
            return 0.6;
        } else if (incidentCount <= thresholdHigh) {
            return 0.9;
        } else {
            return 1.0;
        }
    }

    @Override
    public String name() {
        return "ComplaintAccumulationRule";
    }
}
