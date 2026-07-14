package com.carrierfraud.domain;

public interface StrategyRule {
    double evaluate(Transaction transaction);

    String name();

    default String explain(Transaction transaction, double score) {
        return "Rule triggered with score " + String.format("%.2f", score);
    }
}