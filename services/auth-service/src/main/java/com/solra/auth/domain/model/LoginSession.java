package com.solra.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Login session entity — tracks an authenticated user session.
 */
public class LoginSession {

    private String sessionId;
    private String userId;
    private String deviceId;           // AUTH-005: associated device fingerprint
    private LoginMethod loginMethod;
    private String deviceInfo;
    private String ipAddress;
    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;
    private Instant createdAt;

    private LoginSession() {}

    /**
     * Create a login session without deviceId (backward-compatible).
     */
    public static LoginSession create(String userId, LoginMethod method,
                                       String deviceInfo, String ipAddress,
                                       String accessToken, String refreshToken,
                                       long expiresInSeconds) {
        return create(userId, method, deviceInfo, ipAddress,
                accessToken, refreshToken, expiresInSeconds, null);
    }

    /**
     * AUTH-005: Create a login session with device association.
     */
    public static LoginSession create(String userId, LoginMethod method,
                                       String deviceInfo, String ipAddress,
                                       String accessToken, String refreshToken,
                                       long expiresInSeconds, String deviceId) {
        LoginSession session = new LoginSession();
        session.sessionId = UUID.randomUUID().toString();
        session.userId = userId;
        session.deviceId = deviceId;
        session.loginMethod = method;
        session.deviceInfo = deviceInfo;
        session.ipAddress = ipAddress;
        session.accessToken = accessToken;
        session.refreshToken = refreshToken;
        Instant now = Instant.now();
        session.createdAt = now;
        session.expiresAt = now.plusSeconds(expiresInSeconds);
        return session;
    }

    /**
     * AUTH-005: Refresh session tokens (rotate access + refresh tokens).
     */
    public void refreshTokens(String newAccessToken, String newRefreshToken, long expiresInSeconds) {
        this.accessToken = newAccessToken;
        this.refreshToken = newRefreshToken;
        this.expiresAt = Instant.now().plusSeconds(expiresInSeconds);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    // -- Getters --
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public String getDeviceId() { return deviceId; }          // AUTH-005
    public LoginMethod getLoginMethod() { return loginMethod; }
    public String getDeviceInfo() { return deviceInfo; }
    public String getIpAddress() { return ipAddress; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
