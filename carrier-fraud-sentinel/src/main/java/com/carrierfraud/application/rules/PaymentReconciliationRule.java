package com.carrierfraud.application.rules;

import com.carrierfraud.domain.StrategyRule;
import com.carrierfraud.domain.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RULE 1: Payment Reconciliation Rule
 *
 * THRESHOLD-BASED SCORING:
 * - 0-10% unpaid  = 0.0 (CLEAN: acceptable market variation)
 * - 10-30% unpaid = 0.3 (LOW: concerning, monitor)
 * - 30-50% unpaid = 0.6 (MEDIUM: problematic, investigate)
 * - 50-80% unpaid = 0.9 (HIGH: critical pattern)
 * - 80%+ unpaid   = 1.0 (CRITICAL: fraud confirmed)
 *
 * BUSINESS CONTEXT (From 6 years at Alpega/WTRANSNET):
 * Below 10%: Normal market variation (late payments, disputes, errors)
 * 10-30%: Carrier may have cashflow problems (monitor)
 * 30-50%: Systematic issue (investigate, contact carrier)
 * 50-80%: Fraud pattern emerging (restrict carrier, escalate)
 * 80%+: Confirmed fraud (suspend, route to LEGAL)
 *
 * All thresholds configurable via application.properties.
 */
@Component
public class PaymentReconciliationRule implements StrategyRule {

    @Value("${fraud.rule.payment.threshold.clean:0.10}")
    private double thresholdClean;

    @Value("${fraud.rule.payment.threshold.low:0.30}")
    private double thresholdLow;

    @Value("${fraud.rule.payment.threshold.medium:0.50}")
    private double thresholdMedium;

    @Value("${fraud.rule.payment.threshold.high:0.80}")
    private double thresholdHigh;

    @Override
    public double evaluate(Transaction transaction) {
        int totalCases = transaction.getFailedPayments() + transaction.getSucceededPayments();

        if (totalCases == 0) {
            return 0.0;
        }

        double unpaidRate = 1.0 - transaction.getPaymentSuccessRate();

        if (unpaidRate <= thresholdClean) {
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
