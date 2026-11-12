package com.switchyard.fraud;

import com.switchyard.exception.FraudDetectedException;
import com.switchyard.model.Transaction;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Concrete handler in the Fraud Chain that blocks transactions from blacklisted locations.
 */
public class BlacklistedLocationFraudRule extends FraudRule {
    private final Set<String> blockedLocations = new HashSet<>(
            Arrays.asList("SHADOW_PROXY", "HIGH_RISK_ZONE", "DARKNET_NODE")
    );

    @Override
    protected void check(Transaction tx) throws FraudDetectedException {
        String location = tx.getLocation();
        if (location != null && blockedLocations.contains(location.trim().toUpperCase())) {
            String msg = "Transaction originating from blacklisted location: " + location;
            tx.addAuditLog("FRAUD_CHECK", "Blocked by BlacklistedLocationFraudRule: " + msg);
            throw new FraudDetectedException(msg, "BlacklistedLocationFraudRule");
        }
        tx.addAuditLog("FRAUD_CHECK", "Passed BlacklistedLocationFraudRule");
    }
}