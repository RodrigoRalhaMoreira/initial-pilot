package com.example.transactionsapi.model;

import org.web3j.abi.datatypes.Address;
import org.web3j.tx.Contract;

public class Transaction {
    private String type;
    private double amount;
    private String bytecode;
    private Address sender;
    private Contract smartContract;
    private String smartContractParams;

    public Transaction() {
    }

    public Transaction(String type, double amount) {
        this.type = type;
        this.amount = amount;
        this.bytecode = "Default bytecode";
        this.sender = null;
        this.smartContract = null;
        this.smartContractParams = "";
    }

    public Transaction(String type, double amount, String bytecode) {
        this.type = type;
        this.amount = amount;
        this.bytecode = bytecode;
        this.sender = null;
        this.smartContract = null;
        this.smartContractParams = "";
    }

    public Transaction(String type, double amount, String bytecode, String smartContractParams, Address sender) {
        this.type = type;
        this.amount = amount;
        this.bytecode = bytecode;
        this.sender = sender;
        this.smartContract = null;
        this.smartContractParams = smartContractParams;
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

    public Address getSender() {
        return sender;
    }

    public void setSender(Address sender) {
        this.sender = sender;
    }

    public Contract getSmartContract() {
        return smartContract;
    }

    public void setSmartContract(Contract smartContract) {
        this.smartContract = smartContract;
    }

    public String getSmartContractParams() {
        return smartContractParams;
    }

    public void setSmartContractParams(String smartContractParams) {
        this.smartContractParams = smartContractParams;
    }
}
