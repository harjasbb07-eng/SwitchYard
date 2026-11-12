package com.switchyard.gateway;

/**
 * Mock external PayPal Wallet SDK that returns HTTP status codes (504 on amount == 9999).
 */
public class PayPalWalletSDK {
    public int sendWalletTransfer(String email, double amount) {
        if (Double.compare(amount, 9999.0) == 0) {
            return 504;
        }
        return 200;
    }
}