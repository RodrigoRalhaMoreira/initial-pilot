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
            // Start the Docker container
            String[] startCommand = {"docker", "run", "-d", "--name", "zokrates_container", "zokrates/zokrates", "tail", "-f", "/dev/null"};
            new ProcessBuilder(startCommand).start().waitFor();

            // Copy the Zokrates file into the running Docker container
            String[] copyCommand = {"docker", "cp", circuitFilePath, "zokrates_container:/home/zokrates/example.zok"};
            new ProcessBuilder(copyCommand).start().waitFor();

            // Execute the Zokrates compile command inside the Docker container
            String[] compileCommand = {"docker", "exec", "zokrates_container", "/bin/bash", "-c", "zokrates compile -i example.zok"};
            Process process = new ProcessBuilder(compileCommand).start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Zokrates compilation successful");
            } else {
                System.out.println("Zokrates compilation failed");
            }

            // Stop and remove the Docker container
            String[] stopCommand = {"docker", "rm", "-f", "zokrates_container"};
            new ProcessBuilder(stopCommand).start().waitFor();
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
