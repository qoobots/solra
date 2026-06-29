package com.solra.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Login session entity — tracks an authenticated user session.
 */
public class LoginSession {

    private String sessionId;
    private String userId;
    private LoginMethod loginMethod;
    private String deviceInfo;
    private String ipAddress;
    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;
    private Instant createdAt;

    private LoginSession() {}

    public static LoginSession create(String userId, LoginMethod method,
                                       String deviceInfo, String ipAddress,
                                       String accessToken, String refreshToken,
                                       long expiresInSeconds) {
        LoginSession session = new LoginSession();
        session.sessionId = UUID.randomUUID().toString();
        session.userId = userId;
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

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    // -- Getters --
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public LoginMethod getLoginMethod() { return loginMethod; }
    public String getDeviceInfo() { return deviceInfo; }
    public String getIpAddress() { return ipAddress; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
