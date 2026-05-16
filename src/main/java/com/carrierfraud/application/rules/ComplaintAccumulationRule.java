package com.carrierfraud.application.rules;

import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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