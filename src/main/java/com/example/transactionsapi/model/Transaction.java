package com.example.transactionsapi.model;

import org.web3j.abi.datatypes.Address;
import org.web3j.tx.Contract;

public class Transaction {
    private String type;
    private String amount;
    private String bytecode;
    private Address sender;
    private Address receiver;
    private Contract smartContract;
    private String smartContractParams;
    private boolean isPrivate;
    private String[] zkProofParams;

    public Transaction() {
    }

    public Transaction(String type, String amount) {
        this.type = type;
        this.amount = amount;
        this.bytecode = "Default bytecode";
        this.sender = null;
        this.smartContract = null;
        this.smartContractParams = "";
        this.isPrivate = false;
    }

    public Transaction(String type, String amount, String bytecode) {
        this.type = type;
        this.amount = amount;
        this.bytecode = bytecode;
        this.sender = null;
        this.smartContract = null;
        this.smartContractParams = "";
        this.isPrivate = false;
    }

    public Transaction(String type, String amount, String bytecode, String smartContractParams, Address sender, Address receiver) {
        this(type, amount, bytecode, smartContractParams, sender, receiver, false);
    }

    public Transaction(String type, String amount, String bytecode, String smartContractParams, Address sender, Address receiver, boolean isPrivate) {
        this.type = type;
        this.amount = amount;
        this.bytecode = bytecode;
        this.sender = sender;
        this.receiver = receiver;
        this.smartContract = null;
        this.smartContractParams = smartContractParams;
        this.isPrivate = isPrivate;
    }


    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
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

    public Address getReceiver() {
        return receiver;
    }

    public void setReceiver(Address receiver) {
        this.receiver = receiver;
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

    public boolean isPrivate() {
        return this.isPrivate;
    }

    public void setIsPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public String serialize() {
        return String.format("Transaction: {type: %s, amount: %s, bytecode: %s, sender: %s, receiver: %s, smartContractParams: %s, isPrivate: %b}", this.type, this.amount, this.bytecode, this.sender, this.receiver, this.smartContractParams, this.isPrivate);
    }
}
