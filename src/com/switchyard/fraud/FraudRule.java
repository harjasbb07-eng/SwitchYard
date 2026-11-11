package com.switchyard.fraud;

import com.switchyard.exception.FraudDetectedException;
import com.switchyard.model.Transaction;

public abstract class FraudRule {
    protected FraudRule nextRule;

    public FraudRule setNext(FraudRule next) {
        this.nextRule = next;
        return next;
    }

    // Template Method Pattern: executes current check and delegates down the chain
    public final void evaluate(Transaction tx) throws FraudDetectedException {
        check(tx);
        if (nextRule != null) {
            nextRule.evaluate(tx);
        }
    }

    protected abstract void check(Transaction tx) throws FraudDetectedException;
}