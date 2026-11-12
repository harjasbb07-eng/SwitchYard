package com.switchyard.fraud;

/**
 * Factory class that builds and links the default Chain of Responsibility for fraud evaluation.
 */
public class FraudChainFactory {

    public static FraudRule createDefaultFraudChain() {
        FraudRule head = new AmountLimitFraudRule(100000.0);
        FraudRule locationRule = new BlacklistedLocationFraudRule();
        FraudRule velocityRule = new VelocityCheckFraudRule();

        head.setNext(locationRule).setNext(velocityRule);
        return head;
    }
}