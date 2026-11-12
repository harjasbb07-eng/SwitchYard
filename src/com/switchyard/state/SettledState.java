package com.switchyard.state;

import com.switchyard.exception.InvalidTransactionStateException;
import com.switchyard.model.Transaction;

public class SettledState implements TransactionState {

    @Override
    public void next(Transaction tx) throws InvalidTransactionStateException {
        throw new InvalidTransactionStateException(
                "Transaction " + tx.getTxId() + " is already SETTLED and cannot transition further.");
    }

    @Override
    public void fail(Transaction tx, String reason) {
        tx.addAuditLog("SETTLED", "Ignored fail attempt on already SETTLED transaction: " + reason);
    }

    @Override
    public String getStateName() {
        return "SETTLED";
    }
}