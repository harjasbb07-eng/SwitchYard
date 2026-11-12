package com.switchyard.state;

import com.switchyard.exception.InvalidTransactionStateException;
import com.switchyard.model.Transaction;

public class RoutedState implements TransactionState {

    @Override
    public void next(Transaction tx) throws InvalidTransactionStateException {
        tx.setState(new SettledState());
        tx.addAuditLog("SETTLED", "Payment processed and settled successfully via " + tx.getSelectedGateway() + ".");
    }

    @Override
    public void fail(Transaction tx, String reason) {
        tx.setState(new FailedState());
        tx.addAuditLog("FAILED", "Failed from ROUTED state: " + reason);
    }

    @Override
    public String getStateName() {
        return "ROUTED";
    }
}