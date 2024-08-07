package com.example.transactionsapi.controller;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Type;
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

import com.example.transactionsapi.client.TransactionService;
import com.example.transactionsapi.contracts.HelloWorld;
import com.example.transactionsapi.model.Transaction;
import com.example.transactionsapi.utils.PublicAddressUtil;


@RestController
@RequestMapping("/api")
public class TransactionController {

    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(6_000_000);
    private static final BigInteger GAS_PRICE = BigInteger.valueOf(20_000_000_000L);
    private static final String privateKey = "0x8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63";

    private final Web3j web3j;
    private final Credentials credentials;
    private final StaticGasProvider gasProvider;
    private final TransactionService transactionService;
    private final ZokratesController zokratesController;


    @Autowired
    public TransactionController(TransactionService transactionService, ZokratesController zokratesController) {
        this.web3j = Web3j.build(new HttpService());
        this.credentials = Credentials.create(privateKey);  
        this.gasProvider = new StaticGasProvider(GAS_PRICE, GAS_LIMIT);  
        this.transactionService = transactionService;
        this.zokratesController = zokratesController;
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

            String contractAddress = contract.getContractAddress();
            return ResponseEntity.ok(contractAddress);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deploying contract: " + e.getMessage());
        }
    }

 
    @PostMapping("/contract-execution")
    public ResponseEntity<String> executeContract(@RequestBody Transaction transaction) {
        
        if (!validateTransactionFields(transaction)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid transaction fields.");
        }
        
        try {
            // Get the next available nonce
            EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                transaction.getSender(),
                DefaultBlockParameterName.LATEST
            ).sendAsync().get();
        
            BigInteger nonce = ethGetTransactionCount.getTransactionCount();
            
            // Split the parameters string into an array of parameters
            String[] params = transaction.getSmartContractParams().split(",");

            // Convert the array of parameters to a list of Utf8String
            List<Type> constructorParams = Arrays.stream(params)
                .map(Utf8String::new)
                .collect(Collectors.toList());

            // Encode the constructor parameters
            String encodedConstructor = FunctionEncoder.encodeConstructor(constructorParams);

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

            TransactionReceipt transactionReceipt = transactionReceiptOptional.get();

            String contractAddress = transactionReceipt.getContractAddress();

            // Load the contract
            HelloWorld contract = HelloWorld.load(
                contractAddress, 
                this.web3j, 
                this.credentials, 
                this.gasProvider
            );
            
            transaction.setSmartContract(contract);
            return transactionService.executeContract(transaction);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing contract: " + e.getMessage());
        }        
    }


    @PostMapping("/private-transaction")
    public ResponseEntity<String> privateTransaction(@RequestBody Transaction transaction) throws Exception {


        processTransaction(transaction.getSender(), transaction.getReceiver());

        String encryptedAmount = PublicAddressUtil.hashTransactionAmount(transaction.getReceiver(), transaction.getAmount());
        transaction.setAmount(encryptedAmount);

        return ResponseEntity.ok("Private transaction executed successfully. " + transaction.serialize());
    }

    // helpers

    private boolean validateTransactionFields(Transaction transaction) {
        if (transaction.getType() == null || transaction.getType().isEmpty()) {
            return false;
        }
    
        if (Double.valueOf(transaction.getAmount()) <= 0.0) {
            return false;
        }
    
        if (transaction.getBytecode() == null || transaction.getBytecode().isEmpty()) {
            return false;
        }
    
        if (transaction.getSender() == null) {
            return false;
        }
    
        return true;
    }

    private void processTransaction(String senderPublicAddress, String receiverPublicAddress) throws NoSuchAlgorithmException {
        
        String[] senderInputs = prepareInputs(senderPublicAddress);
        String[] receiverInputs = prepareInputs(receiverPublicAddress);
        String[] witnessInputs = new String[senderInputs.length + receiverInputs.length];
        
        // Copy senderInputs into witnessInputs
        System.arraycopy(senderInputs, 0, witnessInputs, 0, senderInputs.length);
        
        // Copy receiverInputs into witnessInputs
        System.arraycopy(receiverInputs, 0, witnessInputs, senderInputs.length, receiverInputs.length);
        
        zokratesController.computeWitness(witnessInputs);
        zokratesController.generateProof();
        zokratesController.verifyProof();
    }

    // right now defaulting to hyflexchain addresses, flexibility for later
    private String[] prepareInputs(String address) throws NoSuchAlgorithmException {
        BigInteger[] splitHash = PublicAddressUtil.getHashParts(address);
        BigInteger[] publicKeyParts = PublicAddressUtil.splitAndConvert(address);
        
        // zk-snark generation and hide of sender and receiver
        String[] witnessInputs = new String[publicKeyParts.length + splitHash.length];
        
        for (int i = 0; i < publicKeyParts.length; i++) {
            witnessInputs[i] = publicKeyParts[i].toString();
        }
        for (int i = 0; i < splitHash.length; i++) {
            witnessInputs[i+publicKeyParts.length] = splitHash[i].toString();
        }
        
        for (int i = 0; i < witnessInputs.length; i++) {
            System.out.println(witnessInputs[i]);
        }
        return witnessInputs;
    }


}
