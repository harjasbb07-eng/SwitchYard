package com.switchyard.gateway;

/**
 * Mock external Razorpay UPI SDK that collects VPA payments in INR.
 */
public class RazorpayUpiSDK {
    public boolean collectVpaPayment(String customerId, double amountInRupees) {
        return amountInRupees > 0;
    }
}