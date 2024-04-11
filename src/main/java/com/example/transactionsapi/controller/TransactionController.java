package com.example.transactionsapi.controller;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import com.example.transactionsapi.contracts.HelloWorld;
import com.example.transactionsapi.model.Transaction;


@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(4_300_000);
    private static final BigInteger GAS_PRICE = BigInteger.valueOf(20_000_000_000L);
    private static final String privateKey = "0x8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63";

    private Web3j web3j;

    @Autowired
    public TransactionController() {
        this.web3j = Web3j.build(new HttpService("http://localhost:8545"));  // Ganache RPC server
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

    @PostMapping("/deploy")
    public ResponseEntity<String> deployContract() {
        // Connect to local Ethereum node
        try {
            String initialValue = "Hello, World!";  // replace with your actual initial value
            HelloWorld contract = HelloWorld.deploy(
                this.web3j,
                Credentials.create(privateKey),
                new StaticGasProvider(GAS_PRICE, GAS_LIMIT),
                initialValue  // constructor argument
            ).send();

            String contractAddress = contract.getContractAddress();
            return ResponseEntity.ok(contractAddress);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deploying contract: " + e.getMessage());
        }
    }

    @PostMapping("/contract-execution")
    public ResponseEntity<String> executeContract(@RequestBody Transaction transaction) {
        try {
            // Create credentials from private key
            Credentials credentials = Credentials.create(privateKey);

            // Get the account address
            String accountAddress = credentials.getAddress();
            // Get the next available nonce
            EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                accountAddress,
                DefaultBlockParameterName.LATEST
            ).sendAsync().get();
        
            BigInteger nonce = ethGetTransactionCount.getTransactionCount();

            // Create and sign a raw transaction
            RawTransaction rawTransaction = RawTransaction.createContractTransaction(
                nonce,
                GAS_PRICE,
                GAS_LIMIT,
                BigInteger.ZERO,  // value sent with the transaction
                transaction.getBytecode()
            );
            // Sign the raw transaction
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            // Send the transaction
            EthSendTransaction ethSendTransaction = this.web3j.ethSendRawTransaction(hexValue).send();
            
            // Get the transaction hash
            String transactionHash = ethSendTransaction.getTransactionHash();

            return ResponseEntity.ok(transactionHash);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing contract: " + e.getMessage());
        }
    }

}
