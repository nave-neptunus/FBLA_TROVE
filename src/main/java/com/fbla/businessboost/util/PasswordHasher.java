package com.fbla.businessboost.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Handles secure password hashing using bcrypt.
 *
 * <p>
 * <strong>Why bcrypt instead of SHA-256?</strong><br>
 * SHA-256 is a fast cryptographic hash, which makes it vulnerable to
 * brute-force and dictionary attacks — an attacker can try billions of
 * passwords per second with a GPU. Bcrypt is intentionally slow (tunable
 * via the work factor) and incorporates a salt automatically, making
 * offline brute-force attacks computationally impractical.
 * </p>
 *
 * <p>
 * Work factor 12 is the current recommended default: it takes ~300ms per
 * hash on modern hardware, which is imperceptible to users but devastating
 * for attackers.
 * </p>
 *
 * @author FBLA Development Team
 * @version 2.0
 */
public class PasswordHasher {

    /** BCrypt work factor (cost). Higher = slower hash, more secure. */
    private static final int WORK_FACTOR = 12;

    /**
     * Hashes a plaintext password using bcrypt with a freshly generated salt.
     *
     * <p>
     * Each call produces a different hash for the same password because a new
     * random salt is generated each time. This is by design — use
     * {@link #verifyPassword(String, String)} to check passwords.
     * </p>
     *
     * @param plaintext The raw password to hash (must not be null)
     * @return A bcrypt hash string (60 characters), or null if hashing fails
     */
    public static String hashPassword(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        try {
            return BCrypt.hashpw(plaintext, BCrypt.gensalt(WORK_FACTOR));
        } catch (Exception e) {
            System.err.println("Error hashing password: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verifies a plaintext password against a stored bcrypt hash.
     *
     * <p>
     * This must be used instead of re-hashing and comparing strings, because
     * bcrypt hashes embed the salt and can only be verified by BCrypt itself.
     * </p>
     *
     * @param plaintext  The raw password to verify
     * @param storedHash The bcrypt hash from the database
     * @return true if the password matches, false otherwise
     */
    public static boolean verifyPassword(String plaintext, String storedHash) {
        if (plaintext == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plaintext, storedHash);
        } catch (Exception e) {
            System.err.println("Error verifying password: " + e.getMessage());
            return false;
        }
    }

    /** Private constructor — utility class, do not instantiate. */
    private PasswordHasher() {
        throw new UnsupportedOperationException("Utility class");
    }
}
