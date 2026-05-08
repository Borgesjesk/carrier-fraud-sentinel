package com.carrierfraud.application.rules;

import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;
import org.springframework.stereotype.Component;

@Component
public class FailedPaymentsRule implements StrategyRule {

    @Override
    public double evaluate(Transaction transaction) {
        int failedPayments = transaction.getFailedPayments();
        if (failedPayments == 0) return 0.0;
        if (failedPayments <= 1) return 0.4;
        if (failedPayments <= 3) return 0.6;
        if (failedPayments <= 5) return 0.8;
        return 1.0;
    }

    @Override
    public String name() {
        return  "FailedPaymentsRule";
    }
}

