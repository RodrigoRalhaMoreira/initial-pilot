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

    public static BigInteger[] getHashParts(String publicAddress) throws NoSuchAlgorithmException {
        publicAddress = formatStringTo128Chars(publicAddress);
        byte[] x = hexStringToByteArray(publicAddress);
        System.out.println(Arrays.toString(x));
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(x);
        System.out.println("Hash: " + hash);
        byte[] i1 = Arrays.copyOfRange(hash, 0, 16);
        byte[] i2 = Arrays.copyOfRange(hash, 16, 32);

        BigInteger val1 = new BigInteger(1, i1);
        BigInteger val2 = new BigInteger(1, i2);

        return new BigInteger[]{val1, val2};
    }

    /*
     * For the input data, we split into four 128-bit values, and which gives us a data input of up to 512 bits. The values of the input will be a, b, c and d, and where d is the end part of the data buffer. With many other cryptosystems, we use a big-endian format, and where the last byte as the first byte of the data.
     */
    public static BigInteger[] splitAndConvert(String publicAddress) {
        publicAddress = formatStringTo128Chars(publicAddress);
        BigInteger[] parts = new BigInteger[4];
        for (int i = 0; i < 4; i++) {
            String part = publicAddress.substring(i * 32, (i + 1) * 32);
            parts[i] = new BigInteger(part, 16);
        }

        return parts;
    }

    public static String hashTransactionAmount(String receiverPublicAddress, String transactionAmount) throws Exception {
        // Hash the receiver's public address to create a 256 bit key
        byte[] key = (receiverPublicAddress).getBytes("UTF-8");
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // use only first 128 bit

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
        key = Arrays.copyOf(key, 16); // use only first 128 bit
    
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

    
}