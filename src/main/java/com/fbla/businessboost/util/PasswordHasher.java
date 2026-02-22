package com.fbla.businessboost.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for securely hashing user passwords.
 */
public class PasswordHasher {

    /**
     * Hashes a password string using SHA-256.
     * 
     * @param password The plaintext password to hash
     * @return The resulting SHA-256 hex string, or null if an error occurs
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Failed to hash password: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Converts a byte array into a hexadecimal string representation.
     */
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
}
