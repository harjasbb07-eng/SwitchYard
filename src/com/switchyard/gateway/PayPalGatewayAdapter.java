package com.switchyard.gateway;

import com.switchyard.exception.GatewayTimeoutException;
import com.switchyard.model.PaymentMethod;
import com.switchyard.model.Transaction;

/**
 * Adapter that connects PayPalWalletSDK to the PaymentGateway target interface.
 */
public class PayPalGatewayAdapter implements PaymentGateway {
    private final PayPalWalletSDK sdk;
    private double feePercentage;
    private double successRate;
    private boolean enabled;

    public PayPalGatewayAdapter(PayPalWalletSDK sdk, double feePercentage, double successRate, boolean enabled) {
        this.sdk = sdk;
        this.feePercentage = feePercentage;
        this.successRate = successRate;
        this.enabled = enabled;
    }

    @Override
    public boolean processPayment(Transaction tx) throws GatewayTimeoutException {
        String email = (tx.getCustomer() != null && tx.getCustomer().getEmail() != null)
                ? tx.getCustomer().getEmail()
                : "customer@switchyard.com";
        int statusCode = sdk.sendWalletTransfer(email, tx.getAmount());
        tx.addAuditLog("GATEWAY_EXEC", "PayPalWalletSDK.sendWalletTransfer(" + email + ", " + tx.getAmount() + ") -> Status " + statusCode);

        if (statusCode == 504) {
            throw new GatewayTimeoutException("PayPal gateway timed out with HTTP 504 for tx " + tx.getTxId());
        }
        return statusCode == 200;
    }

    @Override
    public String getGatewayName() {
        return "PayPal";
    }

    @Override
    public boolean supportsMethod(PaymentMethod method) {
        return method == PaymentMethod.WALLET || method == PaymentMethod.CREDIT_CARD;
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