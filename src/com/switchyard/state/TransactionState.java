package com.switchyard.state;

import com.switchyard.exception.InvalidTransactionStateException;
import com.switchyard.model.Transaction;

public interface TransactionState {
    void next(Transaction tx) throws InvalidTransactionStateException;
    void fail(Transaction tx, String reason);
    String getStateName();
}