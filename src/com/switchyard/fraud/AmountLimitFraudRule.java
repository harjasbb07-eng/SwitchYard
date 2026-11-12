package com.switchyard.fraud;

import com.switchyard.exception.FraudDetectedException;
import com.switchyard.model.Transaction;

/**
 * Concrete handler in the Fraud Chain that blocks transactions exceeding the max INR limit.
 */
public class AmountLimitFraudRule extends FraudRule {
    private final double maxAllowedAmount;

    public AmountLimitFraudRule() {
        this(100000.0);
    }

    public AmountLimitFraudRule(double maxAllowedAmount) {
        this.maxAllowedAmount = maxAllowedAmount;
    }

    @Override
    protected void check(Transaction tx) throws FraudDetectedException {
        if (tx.getAmount() > maxAllowedAmount) {
            String msg = "Amount INR " + tx.getAmount() + " exceeds max limit of INR " + maxAllowedAmount;
            tx.addAuditLog("FRAUD_CHECK", "Blocked by AmountLimitFraudRule: " + msg);
            throw new FraudDetectedException(msg, "AmountLimitFraudRule");
        }
        tx.addAuditLog("FRAUD_CHECK", "Passed AmountLimitFraudRule");
    }
}