package com.example.transactionsapi.controller;
import com.example.transactionsapi.contracts.HelloWorld;
import com.example.transactionsapi.model.Transaction;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;
import java.io.FileWriter;
import java.io.IOException;


@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final String CONTRACT_ADDRESS = "your_contract_address";  // replace with your contract's address
    private static final BigInteger GAS_PRICE = BigInteger.valueOf(20_000_000_000L);
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(4_300_000);
    private Web3j web3j;
    private HelloWorld helloWorld;

    @Autowired
    public TransactionController() {
        this.web3j = Web3j.build(new HttpService("http://localhost:8545"));  // Ganache RPC server
        ContractGasProvider gasProvider = new StaticGasProvider(GAS_PRICE, GAS_LIMIT);
        //this.helloWorld = HelloWorld.load(CONTRACT_ADDRESS, web3j, credentials, gasProvider);;
    }

    @PostMapping
    public ResponseEntity<String> createTransaction(@RequestBody Transaction transaction) {
        try {
            saveTransactionToFile(transaction);
            return new ResponseEntity<>("Transaction saved successfully", HttpStatus.CREATED);
        } catch (IOException e) {
            return new ResponseEntity<>("Error saving transaction", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private void saveTransactionToFile(Transaction transaction) throws IOException {
        try (FileWriter writer = new FileWriter("transactions.txt", true)) {
            writer.write(transaction.getType() + ":" + transaction.getAmount() + "\n");
        }
    }

    @PostMapping("/contract")
    public ResponseEntity<String> executeContract(@RequestBody String message) {
        try {
            TransactionReceipt receipt = helloWorld.update(message).send();
            return new ResponseEntity<>(receipt.getTransactionHash(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error executing contract", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/deploy")
    public ResponseEntity<String> deployContract() {
        String privateKey = "0x8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63";  // replace with your private key
        Credentials credentials = Credentials.create(privateKey);

        // Connect to local Ethereum node
        Web3j web3j = Web3j.build(new HttpService("http://localhost:8545"));  // replace with your local node URL if different

        try {
            String initialValue = "Hello, World!";  // replace with your actual initial value
            HelloWorld contract = HelloWorld.deploy(
                web3j,
                credentials,
                new StaticGasProvider(GAS_PRICE, GAS_LIMIT),
                initialValue  // constructor argument
            ).send();

            String contractAddress = contract.getContractAddress();
            return ResponseEntity.ok(contractAddress);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deploying contract: " + e.getMessage());
        }
    }
}
