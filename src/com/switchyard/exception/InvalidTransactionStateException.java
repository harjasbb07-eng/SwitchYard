package com.switchyard.exception;

public class InvalidTransactionStateException extends PaymentException {
    public InvalidTransactionStateException(String message) {
        super(message);
    }
}