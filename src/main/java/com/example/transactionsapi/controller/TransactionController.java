package com.example.transactionsapi.controller;
import com.example.transactionsapi.model.Transaction;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileWriter;
import java.io.IOException;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

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
}
