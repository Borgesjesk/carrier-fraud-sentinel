package com.carrierfraud.application.rules;

import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;
import org.springframework.stereotype.Component;

@Component
public class ComplaintAccumulationRule implements StrategyRule {

    private static final int THRESHOLD_CLEAN = 10;
    private static final int THRESHOLD_LOW = 20;
    private static final int THRESHOLD_MEDIUM = 30;
    private static final int THRESHOLD_HIGH = 50;

    @Override
    public double evaluate(Transaction transaction) {
        int incidentCount = transaction.getReportedIncidents();

        if (incidentCount <= THRESHOLD_CLEAN) {
            return 0.0;
        } else if (incidentCount <= THRESHOLD_LOW) {
            return 0.3;
        } else if (incidentCount <= THRESHOLD_MEDIUM) {
            return 0.6;
        } else if (incidentCount <= THRESHOLD_HIGH) {
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