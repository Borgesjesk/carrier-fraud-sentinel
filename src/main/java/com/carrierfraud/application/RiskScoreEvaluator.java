package com.carrierfraud.application;

public class RiskScoreEvaluator {

    public static String evaluate(double riskScore) {
        if (riskScore < 0.0 || riskScore > 10.0) {
            return "Invalid Risk Score";
        } else if (riskScore <= 0.5) {
            return " CLEAN.";
        } else if (riskScore <= 1.0) {
            return " LOW.";
        } else if (riskScore <= 2.0) {
            return " MEDIUM.";
        }
        return " CRITICAL.";
    }
}




