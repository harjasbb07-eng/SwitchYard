package com.switchyard.state;

import com.switchyard.exception.InvalidTransactionStateException;
import com.switchyard.model.Transaction;

public class InitiatedState implements TransactionState {

    @Override
    public void next(Transaction tx) throws InvalidTransactionStateException {
        tx.setState(new FraudCheckedState());
        tx.addAuditLog("FRAUD_CHECKED", "Transaction passed all fraud checks and moved to FRAUD_CHECKED state.");
    }

    @Override
    public void fail(Transaction tx, String reason) {
        tx.setState(new FailedState());
        tx.addAuditLog("FAILED", "Failed from INITIATED state: " + reason);
    }

    @Override
    public String getStateName() {
        return "INITIATED";
    }
}