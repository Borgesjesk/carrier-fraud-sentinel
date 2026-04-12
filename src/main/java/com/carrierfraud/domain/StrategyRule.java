package com.carrierfraud.domain;

public interface StrategyRule {

    double evaluate(Transaction transaction);

    String name();

}
