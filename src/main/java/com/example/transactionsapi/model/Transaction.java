package com.example.transactionsapi.model;

public class Transaction {
    private String type;
    private double amount;

    public Transaction() {
        // Needed for JSON deserialization
    }

    public Transaction(String type, double amount) {
        this.type = type;
        this.amount = amount;
    }

    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
