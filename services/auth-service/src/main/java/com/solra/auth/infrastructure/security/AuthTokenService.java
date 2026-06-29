package com.solra.auth.infrastructure.security;

import com.solra.common.security.JwtTokenProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Auth-specific JWT token service wrapping the common JwtTokenProvider.
 */
@Component
public class AuthTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final int accessTokenExpirationSeconds;
    private final int refreshTokenExpirationSeconds;

    public AuthTokenService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.accessTokenExpirationSeconds = (int) (jwtTokenProvider.getAccessTokenExpirationMs() / 1000);
        this.refreshTokenExpirationSeconds = (int) (jwtTokenProvider.getRefreshTokenExpirationMs() / 1000);
    }

    public String generateAccessToken(String userId, List<String> roles) {
        return jwtTokenProvider.generateAccessToken(userId, roles, Map.of());
    }

    public String generateRefreshToken(String userId) {
        return jwtTokenProvider.generateRefreshToken(userId);
    }

    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token).isPresent();
    }

    public String getUserIdFromToken(String token) {
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    public int getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public int getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }
}
