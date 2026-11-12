package com.switchyard;

import com.switchyard.fraud.FraudChainFactory;
import com.switchyard.gateway.GatewayFactory;
import com.switchyard.model.Customer;
import com.switchyard.model.Merchant;
import com.switchyard.model.PaymentMethod;
import com.switchyard.model.Transaction;
import com.switchyard.routing.HighestSuccessRateStrategy;
import com.switchyard.routing.LowestCostStrategy;
import com.switchyard.service.PaymentOrchestrator;

public class ConsoleRunner {
    public static void main(String[] args) {
        System.out.println("Starting SwitchYard Console Runner...");

        // Initialize with default factories as requested in the handout
        PaymentOrchestrator orchestrator = new PaymentOrchestrator(
                FraudChainFactory.createDefaultFraudChain(),
                new LowestCostStrategy(),
                GatewayFactory.createDefaultGateways()
        );

        Merchant demoMerchant = new Merchant("M1", "Demo Merchant");

        // Scenario 1: UPI with LowestCostStrategy (Picks Razorpay @ 0.5%)
        System.out.println("\n--- Scenario 1: UPI / LowestCostStrategy ---");
        Customer c1 = new Customer("C1", "Alice", "alice@example.com");
        Transaction tx1 = new Transaction("TX-1001", 5000.0, PaymentMethod.UPI, "Mumbai", c1, demoMerchant);
        orchestrator.executeTransaction(tx1);
        orchestrator.printTransactionReceipt(tx1);

        // Scenario 2: Dynamic config change (Razorpay fee increased to 3.0%) -> Picks Stripe @ 2.0%
        System.out.println("\n--- Scenario 2: Dynamic Config (Razorpay 3.0%) / LowestCostStrategy ---");
        orchestrator.updateGatewayConfig("Razorpay", 3.0, 96.5, true);
        Customer c2 = new Customer("C2", "Bob", "bob@example.com");
        Transaction tx2 = new Transaction("TX-1002", 5000.0, PaymentMethod.UPI, "Mumbai", c2, demoMerchant);
        orchestrator.executeTransaction(tx2);
        orchestrator.printTransactionReceipt(tx2);

        // Scenario 3: Switch to HighestSuccessRateStrategy -> Picks Stripe (99.2%)
        System.out.println("\n--- Scenario 3: HighestSuccessRateStrategy ---");
        orchestrator.setRoutingStrategy(new HighestSuccessRateStrategy());
        Customer c3 = new Customer("C3", "Charlie", "charlie@example.com");
        Transaction tx3 = new Transaction("TX-1003", 5000.0, PaymentMethod.UPI, "Mumbai", c3, demoMerchant);
        orchestrator.executeTransaction(tx3);
        orchestrator.printTransactionReceipt(tx3);

        // Scenario 4: High amount blocked by AmountLimitFraudRule
        System.out.println("\n--- Scenario 4: Amount Limit Fraud ---");
        Customer c4 = new Customer("C4", "David", "david@example.com");
        Transaction tx4 = new Transaction("TX-1004", 250000.0, PaymentMethod.CREDIT_CARD, "Delhi", c4, demoMerchant);
        orchestrator.executeTransaction(tx4);
        orchestrator.printTransactionReceipt(tx4);

        // Scenario 5: Blacklisted location
        System.out.println("\n--- Scenario 5: Blacklisted Location Fraud ---");
        Customer c5 = new Customer("C5", "Eve", "eve@example.com");
        Transaction tx5 = new Transaction("TX-1005", 5000.0, PaymentMethod.WALLET, "SHADOW_PROXY", c5, demoMerchant);
        orchestrator.executeTransaction(tx5);
        orchestrator.printTransactionReceipt(tx5);

        // Scenario 6: Velocity abuse (4 rapid transactions)
        System.out.println("\n--- Scenario 6: Velocity Check Fraud (4 Transactions) ---");
        Customer cVelocity = new Customer("C-VELOCITY", "Frank", "frank@example.com");
        orchestrator.setRoutingStrategy(new LowestCostStrategy());
        
        for (int i = 1; i <= 4; i++) {
            System.out.println("\nExecuting Velocity Attempt " + i + "...");
            Transaction txV = new Transaction("TX-V" + i, 100.0, PaymentMethod.UPI, "Pune", cVelocity, demoMerchant);
            orchestrator.executeTransaction(txV);
            orchestrator.printTransactionReceipt(txV);
        }
    }
}