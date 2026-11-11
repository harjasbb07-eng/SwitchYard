package com.switchyard.model;

import com.switchyard.state.TransactionState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Transaction {
    private final String txId;
    private final double amount;
    private final PaymentMethod paymentMethod;
    private final String location;

    // Aggregation: Customer and Merchant have independent lifecycles
    private final Customer customer;
    private final Merchant merchant;

    // Composition: AuditEntry list is strictly owned by this Transaction instance
    private final List<AuditEntry> auditTrail = new ArrayList<>();

    private TransactionState currentState;

    // Result & Dashboard Analytics Tracking Fields
    private String selectedGateway = "NONE";
    private double appliedFee = 0.0;
    private String fraudResult = "PENDING";
    private long executionTimeMs = 0;

    public Transaction(String txId, double amount, PaymentMethod paymentMethod, String location,
                       Customer customer, Merchant merchant) {
        this.txId = txId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.location = location;
        this.customer = customer;
        this.merchant = merchant;
        addAuditLog("CREATED", "Transaction object created for amount INR " + amount);
    }

    public void addAuditLog(String stateName, String message) {
        this.auditTrail.add(new AuditEntry(System.currentTimeMillis(), stateName, message));
    }

    public String getTxId() {
        return txId;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getLocation() {
        return location;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public List<AuditEntry> getAuditTrail() {
        return Collections.unmodifiableList(auditTrail);
    }

    public TransactionState getCurrentState() {
        return currentState;
    }

    public void setState(TransactionState currentState) {
        this.currentState = currentState;
    }

    public String getSelectedGateway() {
        return selectedGateway;
    }

    public void setSelectedGateway(String selectedGateway) {
        this.selectedGateway = selectedGateway;
    }

    public double getAppliedFee() {
        return appliedFee;
    }

    public void setAppliedFee(double appliedFee) {
        this.appliedFee = appliedFee;
    }

    public String getFraudResult() {
        return fraudResult;
    }

    public void setFraudResult(String fraudResult) {
        this.fraudResult = fraudResult;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}