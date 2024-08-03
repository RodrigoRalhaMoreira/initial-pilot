package com.example.transactionsapi.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class PublicAddressUtil {

    /*
     In Socrates we can only hold 128-bit values, and so our 256-bit hash is split into two parts.
     */

    public static BigInteger[] getHashParts(String publicAddress, boolean isHyFlexChainAddress) throws NoSuchAlgorithmException {
        if (isHyFlexChainAddress) {
            return getHashPartsHyFlexChainAddress(publicAddress);
        }
        publicAddress = formatStringTo128Chars(publicAddress);
        byte[] x = hexStringToByteArray(publicAddress);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(x);
        byte[] i1 = Arrays.copyOfRange(hash, 0, 16);
        byte[] i2 = Arrays.copyOfRange(hash, 16, 32);

        BigInteger val1 = new BigInteger(1, i1);
        BigInteger val2 = new BigInteger(1, i2);

        return new BigInteger[]{val1, val2};
    }
    public static BigInteger[] getHashPartsHyFlexChainAddress(String publicAddress) throws NoSuchAlgorithmException {
        // Ensure the input string is 184 characters long
        publicAddress = formatStringTo184Chars(publicAddress);

        // Split the public address into four 46-character parts
        String part1 = publicAddress.substring(0, 46);
        String part2 = publicAddress.substring(46, 92);
        String part3 = publicAddress.substring(92, 138);
        String part4 = publicAddress.substring(138, 184);

        // Concatenate the four parts into a single byte array
        byte[] x = concatenateByteArrays(
            hexStringToByteArray(part1),
            hexStringToByteArray(part2),
            hexStringToByteArray(part3),
            hexStringToByteArray(part4)
        );

        // Compute the SHA-256 hash
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(x);

        // Split the hash into two 128-bit parts
        byte[] i1 = Arrays.copyOfRange(hash, 0, 16);
        byte[] i2 = Arrays.copyOfRange(hash, 16, 32);

        // Convert each part to a BigInteger
        BigInteger val1 = new BigInteger(1, i1);
        BigInteger val2 = new BigInteger(1, i2);

        return new BigInteger[]{val1, val2};
    }

    /*
     * For the input data, we split into four 128-bit values, and which gives us a data input of up to 512 bits. The values of the input will be a, b, c and d, and where d is the end part of the data buffer. With many other cryptosystems, we use a big-endian format, and where the last byte as the first byte of the data.
     */
    public static BigInteger[] splitAndConvert(String publicAddress, boolean isHyFlexChainAddress) {
        if (isHyFlexChainAddress) {
            return splitAndConvertHyFlexChainAddress(publicAddress);
        }
        publicAddress = formatStringTo128Chars(publicAddress);
        BigInteger[] parts = new BigInteger[4];
        for (int i = 0; i < 4; i++) {
            String part = publicAddress.substring(i * 32, (i + 1) * 32);
            parts[i] = new BigInteger(part, 16);
        }

        return parts;
    }

    public static BigInteger[] splitAndConvertHyFlexChainAddress(String publicAddress) {
        // Ensure the input string is 184 characters long
        publicAddress = formatStringTo184Chars(publicAddress);
        
        int partLength = 46;
        BigInteger[] parts = new BigInteger[4];
        
        for (int i = 0; i < 4; i++) {
            int start = i * partLength;
            int end = Math.min(start + partLength, publicAddress.length());
            String part = publicAddress.substring(start, end);
            parts[i] = new BigInteger(part, 16);
        }

        return parts;
    }


    public static String hashTransactionAmount(String receiverPublicAddress, String transactionAmount) throws Exception {
        // Hash the receiver's public address to create a 256 bit key
        byte[] key = (receiverPublicAddress).getBytes("UTF-8");
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        key = sha.digest(key);

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        // Encrypt the transaction amount
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] encrypted = cipher.doFinal(transactionAmount.getBytes("UTF-8"));

        // Convert to hexadecimal
        StringBuilder sb = new StringBuilder();
        for (byte b : encrypted) {
            sb.append(String.format("%02X", b));
        }

        return sb.toString();
    }
    public static String decryptTransactionAmount(String receiverPublicAddress, String encryptedTransactionAmount) throws Exception {
        // Hash the receiver's public address to create a 256 bit key
        byte[] key = (receiverPublicAddress).getBytes("UTF-8");
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        key = sha.digest(key);
    
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
    
        // Decrypt the transaction amount
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] decrypted = cipher.doFinal(hexStringToByteArray(encryptedTransactionAmount));
    
        return new String(decrypted, "UTF-8");
    }

    // helpers

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static byte[] concatenateByteArrays(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }
        byte[] result = new byte[totalLength];
        int currentIndex = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, currentIndex, array.length);
            currentIndex += array.length;
        }
        return result;
    }


    
    /**
     * 128 hex characters * 4 bits/hex character = 528 bits.
     * So we will have 4 128-bit values where the public key always stays within the last 2 values and the others are just 0.
     */
    private static String formatStringTo128Chars(String publicAddress) {
        if (publicAddress.startsWith("0x")) {
            publicAddress = publicAddress.substring(2);
        }
        while (publicAddress.length() < 128) {
            publicAddress = "0" + publicAddress;
        }
        return publicAddress;
    }

    // HyFlexChain address example: 0x013059301306072a8648ce3d020106082a8648ce3d0301070342000473e7366e6d854fdea1633eacc168fe2b4f4bc8b4174bc7fff33f0ef17faa319f81e64adc8fdbe9c75899e09b008579e30858bbd2da136ee58e7b2af65b81c79b

    // Helper method to format the string to 184 characters
    private static String formatStringTo184Chars(String input) {
        // Remove the '0x' prefix if present
        if (input.startsWith("0x")) {
            input = input.substring(2);
        }
        
        // Pad the string with leading zeros to make it 184 characters long
        return String.format("%184s", input).replace(' ', '0');
}


    
}