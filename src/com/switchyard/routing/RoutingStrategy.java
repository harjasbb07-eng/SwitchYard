package com.switchyard.routing;

import com.switchyard.exception.NoGatewayAvailableException;
import com.switchyard.gateway.PaymentGateway;
import com.switchyard.model.Transaction;
import java.util.List;

public interface RoutingStrategy {
    PaymentGateway selectGateway(List<PaymentGateway> gateways, Transaction tx) throws NoGatewayAvailableException;
    String getStrategyName();
}