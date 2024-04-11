package com.example.transactionsapi.model;

public class Transaction {
    private String type;
    private double amount;
    private String bytecode;

    public Transaction() {
        // Needed for JSON deserialization
    }

    public Transaction(String type, double amount) {
        this.type = type;
        this.amount = amount;
        this.bytecode = "Default bytecode";
    }

    public Transaction(String type, double amount, String bytecode) {
        this.type = type;
        this.amount = amount;
        this.bytecode = bytecode;
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

    public String getBytecode() {
        return bytecode;
    }

    public void setBytecode(String bytecode) {
        this.bytecode = bytecode;
    }
}
