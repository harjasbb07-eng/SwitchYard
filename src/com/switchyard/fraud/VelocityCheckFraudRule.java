package com.switchyard.fraud;

import com.switchyard.exception.FraudDetectedException;
import com.switchyard.model.Transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * Concrete handler in the Fraud Chain that blocks rapid repeated transactions by the same customer.
 */
public class VelocityCheckFraudRule extends FraudRule {
    private final Map<String, Integer> customerAttempts = new HashMap<>();
    private static final int MAX_ATTEMPTS = 3;

    @Override
    protected void check(Transaction tx) throws FraudDetectedException {
        String customerId = (tx.getCustomer() != null && tx.getCustomer().getCustomerId() != null)
                ? tx.getCustomer().getCustomerId()
                : "UNKNOWN_CUSTOMER";

        int count = customerAttempts.getOrDefault(customerId, 0) + 1;
        customerAttempts.put(customerId, count);

        if (count > MAX_ATTEMPTS) {
            String msg = "Customer " + customerId + " exceeded velocity limit (max 3 tx)";
            tx.addAuditLog("FRAUD_CHECK", "Blocked by VelocityCheckFraudRule: " + msg);
            throw new FraudDetectedException(msg, "VelocityCheckFraudRule");
        }
        tx.addAuditLog("FRAUD_CHECK", "Passed VelocityCheckFraudRule (Attempt " + count + "/" + MAX_ATTEMPTS + ")");
    }
}