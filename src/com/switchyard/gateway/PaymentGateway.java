package com.switchyard.gateway;

import com.switchyard.exception.GatewayTimeoutException;
import com.switchyard.model.PaymentMethod;
import com.switchyard.model.Transaction;

public interface PaymentGateway {
    boolean processPayment(Transaction tx) throws GatewayTimeoutException;
    String getGatewayName();
    boolean supportsMethod(PaymentMethod method);
    double getFeePercentage();
    void setFeePercentage(double feePercentage);
    double getSuccessRate();
    void setSuccessRate(double successRate);
    boolean isEnabled();
    void setEnabled(boolean enabled);
}