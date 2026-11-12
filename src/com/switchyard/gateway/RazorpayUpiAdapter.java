package com.switchyard.gateway;

import com.switchyard.exception.GatewayTimeoutException;
import com.switchyard.model.PaymentMethod;
import com.switchyard.model.Transaction;

/**
 * Adapter that connects RazorpayUpiSDK to the PaymentGateway target interface.
 */
public class RazorpayUpiAdapter implements PaymentGateway {
    private final RazorpayUpiSDK sdk;
    private double feePercentage;
    private double successRate;
    private boolean enabled;

    public RazorpayUpiAdapter(RazorpayUpiSDK sdk, double feePercentage, double successRate, boolean enabled) {
        this.sdk = sdk;
        this.feePercentage = feePercentage;
        this.successRate = successRate;
        this.enabled = enabled;
    }

    @Override
    public boolean processPayment(Transaction tx) throws GatewayTimeoutException {
        String customerId = (tx.getCustomer() != null && tx.getCustomer().getCustomerId() != null)
                ? tx.getCustomer().getCustomerId()
                : "GUEST";
        boolean paid = sdk.collectVpaPayment(customerId, tx.getAmount());
        tx.addAuditLog("GATEWAY_EXEC", "RazorpayUpiSDK.collectVpaPayment(" + customerId + ", " + tx.getAmount() + ") -> " + paid);
        return paid;
    }

    @Override
    public String getGatewayName() {
        return "Razorpay";
    }

    @Override
    public boolean supportsMethod(PaymentMethod method) {
        return method == PaymentMethod.UPI;
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