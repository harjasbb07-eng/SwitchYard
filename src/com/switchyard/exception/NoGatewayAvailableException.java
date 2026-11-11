package com.switchyard.exception;

public class NoGatewayAvailableException extends PaymentException {
    public NoGatewayAvailableException(String message) {
        super(message);
    }
}