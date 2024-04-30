package com.example.transactionsapi.zokrates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.stereotype.Service;

@Service
public class ZokratesService {
    
    // Method to compile the circuit
    public void compileCircuit(String circuitFilePath) {
        try {
            // Command to run Zokrates compiler inside Docker container
            String[] command = {"docker", "run", "--rm", "-v", circuitFilePath + ":/zokrates/code", "zokrates/zokrates", "compile", "-i", "/zokrates/code"};

            // Execute the command
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            // Read output from Zokrates compiler
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Zokrates compilation successful");
            } else {
                System.out.println("Zokrates compilation failed");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Method to generate proof
    public String generateProof(String[] inputs) {
        // Use Zokrates command line or API to generate proof
        // Replace this with your implementation
        return "proof";
    }

    // Method to verify proof
    public boolean verifyProof(String proof, String[] publicInputs) {
        // Use Zokrates command line or API to verify proof
        // Replace this with your implementation
        return true;
    }
}
