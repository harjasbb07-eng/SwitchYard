package com.switchyard.exception;

public class FraudDetectedException extends PaymentException {
    private final String ruleTriggered;

    public FraudDetectedException(String message, String ruleTriggered) {
        super(message);
        this.ruleTriggered = ruleTriggered;
    }

    public String getRuleTriggered() {
        return ruleTriggered;
    }
}