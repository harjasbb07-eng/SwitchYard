package com.switchyard.state;

import com.switchyard.exception.InvalidTransactionStateException;
import com.switchyard.model.Transaction;

public class FailedState implements TransactionState {

    @Override
    public void next(Transaction tx) throws InvalidTransactionStateException {
        throw new InvalidTransactionStateException(
                "Transaction " + tx.getTxId() + " is in FAILED state and cannot proceed.");
    }

    @Override
    public void fail(Transaction tx, String reason) {
        tx.addAuditLog("FAILED", "Additional failure note: " + reason);
    }

    @Override
    public String getStateName() {
        return "FAILED";
    }
}