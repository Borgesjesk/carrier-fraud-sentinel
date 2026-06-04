package com.carrierfraud.application.rules;

import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;
import org.springframework.stereotype.Component;

@Component
public class PaymentReconciliationRule implements StrategyRule {

    private static final double THRESHOLD_CLEAN = 0.10;
    private static final double THRESHOLD_LOW = 0.30;
    private static final double THRESHOLD_MEDIUM = 0.50;
    private static final double THRESHOLD_HIGH = 0.80;

    @Override
    public double evaluate(Transaction transaction) {
        int totalCases = transaction.getFailedPayments() + transaction.getSucceededPayments();

        if (totalCases == 0) {
            return 0.0;
        }

        double unpaidRate = 1.0 - transaction.getPaymentSuccessRate();

        if (unpaidRate <= THRESHOLD_CLEAN) {
            return 0.0;
        } else if (unpaidRate <= THRESHOLD_LOW) {
            return 0.3;
        } else if (unpaidRate <= THRESHOLD_MEDIUM) {
            return 0.6;
        } else if (unpaidRate <= THRESHOLD_HIGH) {
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