package com.example.transactionsapi.controller;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import com.example.transactionsapi.contracts.HelloWorld;
import com.example.transactionsapi.model.Transaction;


@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(6_000_000);
    private static final BigInteger GAS_PRICE = BigInteger.valueOf(20_000_000_000L);
    private static final String privateKey = "0x8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63";

    private final Web3j web3j;
    private final Credentials credentials;
    private final StaticGasProvider gasProvider;


    @Autowired
    public TransactionController() {
        this.web3j = Web3j.build(new HttpService());
        this.credentials = Credentials.create(privateKey);  
        this.gasProvider = new StaticGasProvider(GAS_PRICE, GAS_LIMIT);  
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
        try {
            String initialValue = "Hello, World!";
            HelloWorld contract = HelloWorld.deploy(
                this.web3j,
                this.credentials,
                this.gasProvider,
                initialValue  // constructor argument
            ).send();
    
            // Update the message
            TransactionReceipt receipt = contract.update("Updated message").send();
            if (!receipt.isStatusOK()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating contract: Transaction failed with status " + receipt.getStatus());
            }
    
            // Read the updated message
            String updatedMessage = contract.message().send();
    
            return ResponseEntity.ok("Contract deployed and message updated. Updated message: " + updatedMessage);
    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deploying contract: " + e.getMessage());
        }
    }

    @PostMapping("/contract-execution")
    public ResponseEntity<String> executeContract(@RequestBody Transaction transaction) {
        try {
            // Get the account address
            String accountAddress = this.credentials.getAddress();

            System.out.println("Account address: " + accountAddress);
            // Get the next available nonce
            EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                accountAddress,
                DefaultBlockParameterName.LATEST
            ).sendAsync().get();
        
            BigInteger nonce = ethGetTransactionCount.getTransactionCount();
            
            Utf8String constructorParam = new Utf8String("Hello, World!");

            // Encode the constructor parameters
            String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.asList(constructorParam));
            // Create and sign a raw transaction
            RawTransaction rawTransaction = RawTransaction.createContractTransaction(
                nonce,
                GAS_PRICE,
                GAS_LIMIT,
                BigInteger.ZERO,  // value sent with the transaction
                transaction.getBytecode() + encodedConstructor
            );
            // Sign the raw transaction
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, this.credentials);
            String hexValue = Numeric.toHexString(signedMessage);
    
            // Send the transaction
            EthSendTransaction ethSendTransaction = this.web3j.ethSendRawTransaction(hexValue).send();
    
            // Check if the transaction was successful
            if (ethSendTransaction.hasError()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing contract: " + ethSendTransaction.getError().getMessage());
            }
    
            // Get the transaction hash
            String transactionHash = ethSendTransaction.getTransactionHash();
    
            // Wait for the transaction to be mined
            Optional<TransactionReceipt> transactionReceiptOptional = Optional.empty();
            for (int i = 0; i < 10; i++) {
                EthGetTransactionReceipt ethGetTransactionReceipt = this.web3j.ethGetTransactionReceipt(transactionHash).send();
                transactionReceiptOptional = ethGetTransactionReceipt.getTransactionReceipt();

                if (transactionReceiptOptional.isPresent()) {
                    break;
                }

                // Wait for a while before trying again
                Thread.sleep(1000);
            }
            // Print the transaction receipt
            System.out.println(transactionReceiptOptional.toString());
            if (!transactionReceiptOptional.isPresent()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing contract: Transaction receipt not generated");
            }

            TransactionReceipt transactionReceipt = transactionReceiptOptional.get();

            String contractAddress = transactionReceipt.getContractAddress();
            System.out.println("Contract address: " + contractAddress);

            // Load the contract
            HelloWorld contract = HelloWorld.load(
                contractAddress, 
                this.web3j, 
                this.credentials, 
                this.gasProvider
            );

            System.out.println("Contract loaded");
            String message = contract.message().send();
            System.out.println(message);
            System.out.println("Contract message read");
            // Update the message
            TransactionReceipt receipt = contract.update("Changed variable in the smart contract!").send();
            if (!receipt.isStatusOK()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating contract: Transaction failed with status " + receipt.getStatus());
            }
    
            // Read the updated message
            String updatedMessage = contract.message().send();
    
            return ResponseEntity.ok("Contract deployed and message updated. Updated message: " + updatedMessage);
    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing contract: " + e.getMessage());
        }
    }

}
