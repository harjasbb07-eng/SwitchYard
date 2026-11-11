package com.switchyard.state;

import com.switchyard.exception.InvalidTransactionStateException;
import com.switchyard.model.Transaction;

public class FraudCheckedState implements TransactionState {

    @Override
    public void next(Transaction tx) throws InvalidTransactionStateException {
        tx.setState(new RoutedState());
        tx.addAuditLog("ROUTED", "Payment gateway selected (" + tx.getSelectedGateway() + ") and moved to ROUTED state.");
    }

    @Override
    public void fail(Transaction tx, String reason) {
        tx.setState(new FailedState());
        tx.addAuditLog("FAILED", "Failed from FRAUD_CHECKED state: " + reason);
    }

    @Override
    public String getStateName() {
        return "FRAUD_CHECKED";
    }
}