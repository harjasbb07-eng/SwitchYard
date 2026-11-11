package com.switchyard.exception;

public class GatewayTimeoutException extends PaymentException {
    public GatewayTimeoutException(String message) {
        super(message);
    }
}