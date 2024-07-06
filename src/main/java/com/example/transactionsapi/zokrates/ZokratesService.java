package com.example.transactionsapi.zokrates;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;

@Service
public class ZokratesService {

    private static final byte[] DELIMITER = "UNIQUE_DELIMITER_SEQUENCE".getBytes();

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
            executeDockerCommand(copyProofCommand);
            executeDockerCommand(copyProvingKeyCommand);

            // Read the proof file
            Path proofPath = Paths.get("proof.json");
            byte[] proofBytes = Files.readAllBytes(proofPath);
            outputStream.write(proofBytes);
            outputStream.write(DELIMITER);
            // Read the proving key file
            Path provingKeyPath = Paths.get("proving.key");
            byte[] provingKeyBytes = Files.readAllBytes(provingKeyPath);
            outputStream.write(provingKeyBytes);

            // Clean up: Delete the copied files from the host
            // Files.delete(proofPath);
            // Files.delete(provingKeyPath);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
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

    public boolean verifyProof(byte[] zkpData) {
        // check the type of the provingKey 
        if (proof == null || provingKey == null) {
            throw new IllegalArgumentException("Proof and provingKey cannot be null.");
        }
        boolean verified = false;
        File proofFile = new File("proof.json");
        File provingKeyFile = new File("proving.key");
        try (BufferedWriter proofWriter = new BufferedWriter(new FileWriter(proofFile));
             BufferedWriter provingKeyWriter = new BufferedWriter(new FileWriter(provingKeyFile))) {
            // Write the proof to its file
            proofWriter.write(proof.toString());
            // Write the proving key to its file
            provingKeyWriter.write(provingKey.toString());
    
            // Copy both files to the Docker container
            String[] copyProofCommand = {"docker", "cp", "proof.json", "zokrates_container:/home/zokrates/"};
            String[] copyProvingKeyCommand = {"docker", "cp", "proving.key", "zokrates_container:/home/zokrates/"};
            executeDockerCommand(copyProofCommand);
            executeDockerCommand(copyProvingKeyCommand);
    
            String verifyCmd = "cd /home/zokrates && zokrates verify -p proof.json -v proving.key";
            ProcessBuilder processBuilder = new ProcessBuilder("docker", "exec", "zokrates_container", "/bin/bash", "-c", verifyCmd);
            Process process = processBuilder.start();
            process.waitFor();
            
            // Read the output from the command
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("Proof verified successfully")) {
                    verified = true;
                    break;
                } else if (line.contains("Proof verification failed")) {
                    verified = false;
                    break;
                }
            }

            // Delete the files from the Docker container
            String[] deleteProofCommand = {"docker", "exec", "zokrates_container", "/bin/bash", "-c", "rm /home/zokrates/proof.json"};
            String[] deleteProvingKeyCommand = {"docker", "exec", "zokrates_container", "/bin/bash", "-c", "rm /home/zokrates/proving.key"};
            executeDockerCommand(deleteProofCommand);
            executeDockerCommand(deleteProvingKeyCommand);        
            return verified;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Delete the files from the host machine
            proofFile.delete();
            provingKeyFile.delete();
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
        // Find the delimiter in the combinedData
        int delimiterIndex = findDelimiterIndex(combinedData, DELIMITER);
        if (delimiterIndex == -1) {
            throw new IllegalArgumentException("Delimiter not found in the combined data");
        }

        // Split the combinedData into proofBytes and provingKeyBytes
        byte[] proofBytes = new byte[delimiterIndex];
        System.arraycopy(combinedData, 0, proofBytes, 0, delimiterIndex);
        byte[] provingKeyBytes = new byte[combinedData.length - delimiterIndex - DELIMITER.length];
        System.arraycopy(combinedData, delimiterIndex + DELIMITER.length, provingKeyBytes, 0, provingKeyBytes.length);

        // Write the separated bytes to their respective files
        Files.write(Paths.get("proof.json"), proofBytes);
        Files.write(Paths.get("proving.key"), provingKeyBytes);
        
        // Now you can pass these files to Docker for verification
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