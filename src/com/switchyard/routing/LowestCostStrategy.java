package com.switchyard.routing;

import com.switchyard.exception.NoGatewayAvailableException;
import com.switchyard.gateway.PaymentGateway;
import com.switchyard.model.Transaction;
import java.util.Comparator;
import java.util.List;

public class LowestCostStrategy implements RoutingStrategy {

    @Override
    public PaymentGateway selectGateway(List<PaymentGateway> gateways, Transaction tx)
            throws NoGatewayAvailableException {
        if (gateways == null || gateways.isEmpty()) {
            throw new NoGatewayAvailableException("No payment gateways registered in orchestrator.");
        }

        return gateways.stream()
                .filter(g -> g.isEnabled() && g.supportsMethod(tx.getPaymentMethod()))
                .min(Comparator.comparingDouble(PaymentGateway::getFeePercentage)
                        .thenComparing(Comparator.comparingDouble(PaymentGateway::getSuccessRate).reversed()))
                .orElseThrow(() -> new NoGatewayAvailableException(
                        "No active payment gateway available supporting method: " + tx.getPaymentMethod()));
    }

    @Override
    public String getStrategyName() {
        return "LOWEST_COST";
    }
}