package com.switchyard.gateway;

/**
 * Mock external Stripe SDK that processes charges in paise.
 */
public class StripeExternalSDK {
    public String chargeInPaise(String txId, long amountInPaise) {
        return "STRIPE_RECEIPT_" + txId + "_" + amountInPaise;
    }
}