package com.switchyard.gateway;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class that instantiates default configurable payment gateway adapters.
 */
public class GatewayFactory {

    public static List<PaymentGateway> createDefaultGateways() {
        List<PaymentGateway> gateways = new ArrayList<>();
        gateways.add(new StripeGatewayAdapter(new StripeExternalSDK(), 2.0, 99.2, true));
        gateways.add(new RazorpayUpiAdapter(new RazorpayUpiSDK(), 0.5, 96.5, true));
        gateways.add(new PayPalGatewayAdapter(new PayPalWalletSDK(), 1.5, 97.8, true));
        return gateways;
    }
}