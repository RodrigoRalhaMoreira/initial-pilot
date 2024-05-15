package com.example.transactionsapi.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class PublicAddressUtil {

    public static String getHash(String publicAddress) throws NoSuchAlgorithmException {
        byte[] x = new BigInteger(publicAddress, 16).toByteArray();

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(x);
        return bytesToHex(hash);
    }

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

    public static BigInteger[] splitAndConvert(String publicAddress) {
        publicAddress = formatStringTo128Chars(publicAddress);
        BigInteger[] parts = new BigInteger[4];
        for (int i = 0; i < 4; i++) {
            String part = publicAddress.substring(i * 32, (i + 1) * 32);
            parts[i] = new BigInteger(part, 16);
        }

        return parts;
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

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