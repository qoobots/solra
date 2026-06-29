package com.solra.common.security;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Common security utilities for all Solra services.
 */
public final class SecurityUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private SecurityUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Generate a cryptographically secure random token.
     */
    public static String generateSecureToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generate a numeric verification code (e.g., for SMS).
     */
    public static String generateVerificationCode(int digits) {
        StringBuilder code = new StringBuilder(digits);
        for (int i = 0; i < digits; i++) {
            code.append(SECURE_RANDOM.nextInt(10));
        }
        return code.toString();
    }

    /**
     * Generate a unique session ID.
     */
    public static String generateSessionId() {
        return generateSecureToken(32);
    }

    /**
     * Generate a unique user ID.
     */
    public static String generateUserId() {
        return "usr_" + generateSecureToken(16);
    }

    /**
     * Hash-sensitive data for storage (using SHA-256 + salt).
     */
    public static String hashSensitive(String data, String salt) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] hash = md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Mask a phone number for display: 138****5678.
     */
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * Validate a Chinese phone number format.
     */
    public static boolean isValidChinesePhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    /**
     * Validate an email address format.
     */
    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,}$");
    }
}
