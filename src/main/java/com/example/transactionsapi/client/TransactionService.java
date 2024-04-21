package com.example.transactionsapi.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;

import com.example.transactionsapi.contracts.HelloWorld;
import com.example.transactionsapi.model.Transaction;

@Service
public class TransactionService {
    

    public TransactionService() {
    }

    public ResponseEntity<String> executeContract(Transaction transaction) {
        // Add your contract execution logic here
        // Update the message
        Contract contract = transaction.getSmartContract();
        HelloWorld helloWorld = (HelloWorld) contract;  // Cast Contract to HelloWorld
        
        try{
            TransactionReceipt receipt = helloWorld.update("Changed variable in the smart contract!").send();
            if (!receipt.isStatusOK()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating contract: Transaction failed with status " + receipt.getStatus());
            }
        
            // Read the updated message
            String updatedMessage = helloWorld.message().send();
        
            return ResponseEntity.ok("Contract deployed and message updated. Updated message: " + updatedMessage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating contract: " + e.getMessage());
        }
    }
}
