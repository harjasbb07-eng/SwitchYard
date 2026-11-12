package com.switchyard.service;

import com.switchyard.exception.FraudDetectedException;
import com.switchyard.exception.GatewayTimeoutException;
import com.switchyard.exception.InvalidTransactionStateException;
import com.switchyard.exception.NoGatewayAvailableException;
import com.switchyard.fraud.FraudRule;
import com.switchyard.gateway.PaymentGateway;
import com.switchyard.model.AuditEntry;
import com.switchyard.model.Transaction;
import com.switchyard.routing.RoutingStrategy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaymentOrchestrator {
    private FraudRule fraudChainHead;
    private RoutingStrategy routingStrategy;
    private final List<PaymentGateway> gateways;
    private final List<Transaction> transactionHistory = new ArrayList<>();

    // Dependency Inversion Principle (DIP): depends on abstractions, not concrete implementations
    public PaymentOrchestrator(FraudRule fraudChainHead, RoutingStrategy routingStrategy,
                               List<PaymentGateway> gateways) {
        this.fraudChainHead = fraudChainHead;
        this.routingStrategy = routingStrategy;
        this.gateways = new ArrayList<>(gateways);
    }

    public synchronized void setRoutingStrategy(RoutingStrategy routingStrategy) {
        this.routingStrategy = routingStrategy;
    }

    public synchronized RoutingStrategy getRoutingStrategy() {
        return routingStrategy;
    }

    public synchronized void setFraudChainHead(FraudRule fraudChainHead) {
        this.fraudChainHead = fraudChainHead;
    }

    public synchronized List<PaymentGateway> getGateways() {
        return Collections.unmodifiableList(gateways);
    }

    public synchronized List<Transaction> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }

    // Allows the live Dashboard UI sliders or ConsoleRunner to dynamically update gateway parameters
    public synchronized boolean updateGatewayConfig(String gatewayName, double feePercentage,
                                                    double successRate, boolean enabled) {
        for (PaymentGateway gateway : gateways) {
            if (gateway.getGatewayName().equalsIgnoreCase(gatewayName)) {
                gateway.setFeePercentage(feePercentage);
                gateway.setSuccessRate(successRate);
                gateway.setEnabled(enabled);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean executeTransaction(Transaction tx) {
        long startTime = System.currentTimeMillis();
        tx.addAuditLog(tx.getCurrentState().getStateName(),
                "Starting orchestration with strategy: " + routingStrategy.getStrategyName());

        try {
            // Step 1: Run Fraud Rule Chain (Chain of Responsibility)
            if (fraudChainHead != null) {
                fraudChainHead.evaluate(tx);
            }
            tx.setFraudResult("PASSED");
            tx.getCurrentState().next(tx); // Moves INITIATED -> FRAUD_CHECKED

            // Step 2: Select Optimal Gateway (Strategy Pattern)
            PaymentGateway chosenGateway = routingStrategy.selectGateway(gateways, tx);
            tx.setSelectedGateway(chosenGateway.getGatewayName());
            tx.setAppliedFee(chosenGateway.getFeePercentage());
            tx.addAuditLog(tx.getCurrentState().getStateName(),
                    String.format("Strategy [%s] selected gateway [%s] (Fee: %.2f%%, Success Rate: %.2f%%)",
                            routingStrategy.getStrategyName(),
                            chosenGateway.getGatewayName(),
                            chosenGateway.getFeePercentage(),
                            chosenGateway.getSuccessRate()));
            tx.getCurrentState().next(tx); // Moves FRAUD_CHECKED -> ROUTED

            // Step 3: Process Payment via Gateway Adapter (Adapter Pattern)
            boolean paymentSuccess = chosenGateway.processPayment(tx);
            if (paymentSuccess) {
                tx.getCurrentState().next(tx); // Moves ROUTED -> SETTLED
            } else {
                tx.getCurrentState().fail(tx, "Gateway " + chosenGateway.getGatewayName() + " declined the payment.");
            }

        } catch (FraudDetectedException e) {
            tx.setFraudResult("BLOCKED: " + e.getRuleTriggered());
            tx.getCurrentState().fail(tx, "Fraud Alert [" + e.getRuleTriggered() + "]: " + e.getMessage());
        } catch (NoGatewayAvailableException e) {
            tx.getCurrentState().fail(tx, "Routing Error: " + e.getMessage());
        } catch (GatewayTimeoutException e) {
            tx.getCurrentState().fail(tx, "Gateway Timeout: " + e.getMessage());
        } catch (InvalidTransactionStateException e) {
            tx.getCurrentState().fail(tx, "State Machine Violation: " + e.getMessage());
        } finally {
            long elapsed = Math.max(1, System.currentTimeMillis() - startTime);
            tx.setExecutionTimeMs(elapsed);
            transactionHistory.add(tx);
        }

        return "SETTLED".equals(tx.getCurrentState().getStateName());
    }

    public void printTransactionReceipt(Transaction tx) {
        System.out.println("================================================================================");
        System.out.printf(" TRANSACTION RECEIPT : %s%n", tx.getTxId());
        System.out.println("================================================================================");
        System.out.printf(" Customer         : %s (%s)%n", tx.getCustomer().getName(), tx.getCustomer().getCustomerId());
        System.out.printf(" Merchant         : %s (%s)%n", tx.getMerchant().getName(), tx.getMerchant().getMerchantId());
        System.out.printf(" Amount & Method  : INR %.2f via %s%n", tx.getAmount(), tx.getPaymentMethod());
        System.out.printf(" Location         : %s%n", tx.getLocation());
        System.out.printf(" Fraud Screening  : %s%n", tx.getFraudResult());
        System.out.printf(" Selected Gateway : %s (Fee: %.2f%%)%n", tx.getSelectedGateway(), tx.getAppliedFee());
        System.out.printf(" Final Status     : %s (Time: %d ms)%n", tx.getCurrentState().getStateName(), tx.getExecutionTimeMs());
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(" Audit Trail:");
        for (AuditEntry entry : tx.getAuditTrail()) {
            System.out.println("   " + entry);
        }
        System.out.println("================================================================================\n");
    }
}