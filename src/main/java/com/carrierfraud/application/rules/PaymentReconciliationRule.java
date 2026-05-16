package com.carrierfraud.application.rules;

import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentReconciliationRule implements StrategyRule {

    @Value("${fraude.rule.payment.threshold.clean:0.10}")
    private double thresholdClean;

    @Value("${fraud.rule.payment.threshold.low:0:30}")
    private double thresholdLow;

    @Value("${fraud.rule.payment.threshold.medium:0.50}")
    private double thresholdMedium;

    @Value("${fraude.rule.payment.threshold.high:0.80}")
    private double thresholdHigh;

    @Override
    public double evaluate(Transaction transaction) {
        int totalCases = transaction.getFailedPayments() + transaction.getSucceededPayments();

        if (totalCases == 0) {
            return 0.0;
        }

        double unpaidRate = 1.0 - transaction.getPaymentSuccessRate();

        if (unpaidRate <= thresholdLow) {
            return 0.0;
        } else if (unpaidRate <= thresholdLow) {
            return 0.3;
        } else if (unpaidRate <= thresholdMedium) {
            return 0.6;
        } else if (unpaidRate <= thresholdHigh) {
            return 0.9;
        } else {
            return 1.0;
        }
    }

    @Override
    public String name() {
        return "PaymentReconciliationRule";
    }
}