package com.solra.auth.application.dto;

/**
 * Result of real-name verification check (AUTH-004).
 */
public record RealNameVerificationResultDTO(
    String userId,
    boolean verified,
    boolean isMinor,
    int age,
    String status,
    String restrictionMessage
) {
    public static RealNameVerificationResultDTO unverified(String userId) {
        return new RealNameVerificationResultDTO(userId, false, false, 0, "UNVERIFIED",
                "Please complete real-name verification to access all features");
    }

    public static RealNameVerificationResultDTO minorRestricted(String userId, int age) {
        return new RealNameVerificationResultDTO(userId, true, true, age, "MINOR_RESTRICTED",
                "Minors under 18 are subject to usage time and content restrictions");
    }

    public static RealNameVerificationResultDTO adult(String userId, int age) {
        return new RealNameVerificationResultDTO(userId, true, false, age, "ADULT",
                "Real-name verified. Full access granted.");
    }
}
