package util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Educational, robust security utility implementing PBKDF2 with HMAC-SHA256
 * and cryptographically secure random salt generation.
 * Follows industry-standard key derivation recommendations (65,536 iterations,
 * 256-bit output) and constant-time password hash comparison.
 */
public final class SecurityUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SecurityUtil() {
        // Utility class
    }

    /**
     * Generates a cryptographically strong 16-byte random salt, Base64-encoded.
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes a password using PBKDF2WithHmacSHA256 with the given Base64-encoded salt.
     *
     * @param password   the plain-text password char array or string
     * @param saltBase64 the salt encoded in Base64
     * @return Base64-encoded password hash
     */
    public static String hashPassword(String password, String saltBase64) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        if (saltBase64 == null || saltBase64.isBlank()) {
            throw new IllegalArgumentException("Salt must not be blank");
        }

        byte[] salt = Base64.getDecoder().decode(saltBase64);
        char[] passwordChars = password.toCharArray();

        try {
            PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to hash password with PBKDF2: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies a plain-text password against a stored Base64 hash using constant-time comparison.
     *
     * @param password           the entered plain-text password
     * @param saltBase64         the stored salt Base64
     * @param expectedHashBase64 the stored hash Base64
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String saltBase64, String expectedHashBase64) {
        if (password == null || saltBase64 == null || expectedHashBase64 == null) {
            return false;
        }
        try {
            String calculatedHash = hashPassword(password, saltBase64);
            byte[] a = Base64.getDecoder().decode(calculatedHash);
            byte[] b = Base64.getDecoder().decode(expectedHashBase64);
            return MessageDigest.isEqual(a, b);
        } catch (Exception e) {
            return false;
        }
    }
}
