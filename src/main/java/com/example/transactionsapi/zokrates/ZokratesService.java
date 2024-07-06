package com.example.transactionsapi.zokrates;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;

@Service
public class ZokratesService {

    private static final byte[] DELIMITER = "UNIQUE_DELIMITER_SEQUENCE".getBytes();
    private static final byte[] DELIMITER2 = "UNIQUE_DELIMITER_SEQUENCE2".getBytes();

    

    public ZokratesService() {
        startZokratesService();
    }

    public void stopZokratesService() {
        try {
            // Stop the Docker container
            String[] stopCommand = {"docker", "stop", "zokrates_container"};
            executeDockerCommand(stopCommand);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void startZokratesService() {
        try {
            if (dockerContainerExists("zokrates_container")) {
                // If the Docker container exists, start it
                String[] startExistingCommand = {"docker", "start", "zokrates_container"};
                executeDockerCommand(startExistingCommand);
            } else {
                // If the Docker container does not exist, create and start it
                String[] startCommand = {"docker", "run", "-d", "--name", "zokrates_container", "zokrates/zokrates", "tail", "-f", "/dev/null"};
                executeDockerCommand(startCommand);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Method to compile the circuit
    public void compileCircuit(String circuitFilePath) {
        try {
            // Copy the Zokrates file into the running Docker container
            String[] copyCommand = {"docker", "cp", circuitFilePath, "zokrates_container:/home/zokrates/signature_proof.zok"};
            executeDockerCommand(copyCommand);
    
            // Execute the Zokrates compile command inside the Docker container
            String[] compileCommand = {"docker", "exec", "zokrates_container", "/bin/bash", "-c", "zokrates compile -i signature_proof.zok"};
            executeDockerCommand(compileCommand);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setup() {
        try {
            // Execute the command inside the Docker container
            String[] dockerCommand = {"docker", "exec", "zokrates_container", "/bin/bash", "-c", "zokrates setup"};
            executeDockerCommand(dockerCommand);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void computeWitness(String[] inputs) {
        try {
            // Prepare the Zokrates command
            String command = "zokrates compute-witness -a " + String.join(" ", inputs);
    
            // Execute the command inside the Docker container
            String[] dockerCommand = {"docker", "exec", "zokrates_container", "/bin/bash", "-c", command};
            executeDockerCommand(dockerCommand);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    // Method to generate proof
    public void oldGenerateProof() {
        try {
            // Execute the command inside the Docker container
            String[] dockerCommand = {"docker", "exec", "zokrates_container", "/bin/bash", "-c", "zokrates generate-proof"};
            executeDockerCommand(dockerCommand);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public byte[] generateProof() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            // Execute the command inside the Docker container to generate the proof
            String[] dockerCommand = {"docker", "exec", "zokrates_container", "/bin/bash", "-c", "zokrates generate-proof"};
            executeDockerCommand(dockerCommand);

            // Copy the proof and proving key files from the Docker container to the host
            String[] copyProofCommand = {"docker", "cp", "zokrates_container:/home/zokrates/proof.json", "."};
            String[] copyProvingKeyCommand = {"docker", "cp", "zokrates_container:/home/zokrates/proving.key", "."};
            String[] copyVerificationKeyCommand = {"docker", "cp", "zokrates_container:/home/zokrates/verification.key", "."};
            
            executeDockerCommand(copyProofCommand);
            executeDockerCommand(copyProvingKeyCommand);
            executeDockerCommand(copyVerificationKeyCommand);

            // Read the proof file
            Path proofPath = Paths.get("proof.json");
            byte[] proofBytes = Files.readAllBytes(proofPath);
            outputStream.write(proofBytes);
            outputStream.write(DELIMITER);
            // Read the proving key file
            Path provingKeyPath = Paths.get("proving.key");
            byte[] provingKeyBytes = Files.readAllBytes(provingKeyPath);
            outputStream.write(provingKeyBytes);
            outputStream.write(DELIMITER2);
            // Read the verification key file
            Path verificationKeyPath = Paths.get("verification.key");
            byte[] verificationKeyBytes = Files.readAllBytes(verificationKeyPath);
            outputStream.write(verificationKeyBytes);
            
            Files.delete(proofPath);
            Files.delete(provingKeyPath);
            Files.delete(verificationKeyPath);

            // Delete the files from the Docker container
            String[] deleteProofCommand = {"docker", "exec", "zokrates_container", "/bin/bash", "-c", "rm /home/zokrates/proof.json"};
            String[] deleteProvingKeyCommand = {"docker", "exec", "zokrates_container", "/bin/bash", "-c", "rm /home/zokrates/proving.key"};
            String[] deleteVerificationKeyCommand = {"docker", "exec", "zokrates_container", "/bin/bash", "-c", "rm /home/zokrates/verification.key"};
            executeDockerCommand(deleteProofCommand);
            executeDockerCommand(deleteProvingKeyCommand);
            executeDockerCommand(deleteVerificationKeyCommand);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Proof and proving key generated successfully, and returned as byte array");
        return outputStream.toByteArray();
    }

    public void verifyProof() {
        try {
            // Execute the command inside the Docker container
            String[] dockerCommand = {"docker", "exec", "zokrates_container", "/bin/bash", "-c", "zokrates verify"};
            executeDockerCommand(dockerCommand);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean verifyProof(byte[] zkpData) throws IOException {
        // Assuming separateAndCreateFiles function handles file creation and checks
        separateAndCreateFiles(zkpData);

        System.out.println("WILL START PROOF VERIFICATION");
        boolean verified = false;
        try {
            // Copy both files to the Docker container
            String[] copyProofCommand = {"docker", "cp", "proof.json", "zokrates_container:/home/zokrates/"};
            String[] copyProvingKeyCommand = {"docker", "cp", "proving.key", "zokrates_container:/home/zokrates/"};
            String[] copyVerificationKeyCommand = {"docker", "cp", "verification.key", "zokrates_container:/home/zokrates/"};
            executeDockerCommand(copyProofCommand);
            executeDockerCommand(copyProvingKeyCommand);
            executeDockerCommand(copyVerificationKeyCommand);
            
    
            // Execute verification command in Docker container
            String[] dockerCommand = {"docker", "exec", "zokrates_container", "/bin/bash", "-c", "zokrates verify"};
            Process process = new ProcessBuilder(dockerCommand).start();
    
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                if (line.contains("PASSED")) {
                    verified = true;
                }
            }
        
            int exitCode = process.waitFor();
            System.out.println("Docker command execution " + (exitCode == 0 ? "successful" : "failed") + ". (Command: " + String.join(" ", dockerCommand) + ")");
    
            // Delete the files from the Docker container
            //String[] deleteProofCommand = {"docker", "exec", "zokrates_container", "/bin/bash", "-c", "rm /home/zokrates/proof.json"};
            //String[] deleteProvingKeyCommand = {"docker", "exec", "zokrates_container", "/bin/bash", "-c", "rm /home/zokrates/proving.key"};
            //executeDockerCommand(deleteProofCommand);
            //executeDockerCommand(deleteProvingKeyCommand);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return verified;
    }

    // helper functions

    private void executeDockerCommand(String[] command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).start();
    
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    
        int exitCode = process.waitFor();
        System.out.println("Docker command execution " + (exitCode == 0 ? "successful" : "failed") + ". (Command: " + String.join(" ", command) + ")");
    }

    private boolean dockerContainerExists(String containerName) throws IOException, InterruptedException {
        String[] checkCommand = {"docker", "ps", "-a", "-q", "--filter", "name=" + containerName};
        Process checkProcess = new ProcessBuilder(checkCommand).start();
        BufferedReader checkReader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
        return checkReader.readLine() != null;
    }

    public void separateAndCreateFiles(byte[] combinedData) throws IOException {
        // Find the indexes of the two delimiters in the combinedData
        int delimiterIndex1 = findDelimiterIndex(combinedData, DELIMITER);
        int delimiterIndex2 = findDelimiterIndex(combinedData, DELIMITER2);
    
        if (delimiterIndex1 == -1 || delimiterIndex2 == -1 || delimiterIndex1 >= delimiterIndex2) {
            throw new IllegalArgumentException("Delimiters not found or in wrong order in the combined data");
        }
    
        // Split the combinedData into proofBytes, provingKeyBytes, and additionalDataBytes
        byte[] proofBytes = new byte[delimiterIndex1];
        System.arraycopy(combinedData, 0, proofBytes, 0, delimiterIndex1);
    
        byte[] provingKeyBytes = new byte[delimiterIndex2 - delimiterIndex1 - DELIMITER.length];
        System.arraycopy(combinedData, delimiterIndex1 + DELIMITER.length, provingKeyBytes, 0, provingKeyBytes.length);
    
        byte[] verificationKeyBytes = new byte[combinedData.length - delimiterIndex2 - DELIMITER2.length];
        System.arraycopy(combinedData, delimiterIndex2 + DELIMITER2.length, verificationKeyBytes, 0, verificationKeyBytes.length);
    
        // Write the separated bytes to their respective files
        Files.write(Paths.get("proof.json"), proofBytes);
        Files.write(Paths.get("proving.key"), provingKeyBytes);
        Files.write(Paths.get("verification.key"), verificationKeyBytes);
    
        System.out.println("Files proof.json, proving.key, and additionalData.txt created successfully");
        // Now you can pass these files to Docker for verification or further processing
    }

    private int findDelimiterIndex(byte[] data, byte[] delimiter) {
        for (int i = 0; i < data.length - delimiter.length + 1; ++i) {
            boolean found = true;
            for (int j = 0; j < delimiter.length; ++j) {
                if (data[i + j] != delimiter[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1; // Delimiter not found
    }
}