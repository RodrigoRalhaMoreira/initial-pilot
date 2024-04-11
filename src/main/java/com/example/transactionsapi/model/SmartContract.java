package com.example.transactionsapi.model;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.Code;

public class SmartContract {
    private final Address contractAddress;
    private final Code code;

    public SmartContract(Address contractAddress, Code code) {
        this.contractAddress = contractAddress;
        this.code = code;
    }

    public Address getContractAddress() {
        return this.contractAddress;
    }

    public Code getCode() {
        return this.code;
    }
}
