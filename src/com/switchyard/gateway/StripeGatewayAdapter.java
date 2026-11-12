package com.switchyard.gateway;

import com.switchyard.exception.GatewayTimeoutException;
import com.switchyard.model.PaymentMethod;
import com.switchyard.model.Transaction;

/**
 * Adapter that connects StripeExternalSDK to the PaymentGateway target interface.
 */
public class StripeGatewayAdapter implements PaymentGateway {
    private final StripeExternalSDK sdk;
    private double feePercentage;
    private double successRate;
    private boolean enabled;

    public StripeGatewayAdapter(StripeExternalSDK sdk, double feePercentage, double successRate, boolean enabled) {
        this.sdk = sdk;
        this.feePercentage = feePercentage;
        this.successRate = successRate;
        this.enabled = enabled;
    }

    @Override
    public boolean processPayment(Transaction tx) throws GatewayTimeoutException {
        long amountInPaise = Math.round(tx.getAmount() * 100);
        String receipt = sdk.chargeInPaise(tx.getTxId(), amountInPaise);
        tx.addAuditLog("GATEWAY_EXEC", "StripeExternalSDK.chargeInPaise(" + tx.getTxId() + ", " + amountInPaise + ") -> " + receipt);
        return receipt != null && !receipt.isEmpty();
    }

    @Override
    public String getGatewayName() {
        return "Stripe";
    }

    @Override
    public boolean supportsMethod(PaymentMethod method) {
        return method == PaymentMethod.CREDIT_CARD || method == PaymentMethod.UPI;
    }

    @Override
    public double getFeePercentage() {
        return feePercentage;
    }

    @Override
    public void setFeePercentage(double feePercentage) {
        this.feePercentage = feePercentage;
    }

    @Override
    public double getSuccessRate() {
        return successRate;
    }

    @Override
    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}