package com.solra.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Shared JWT token provider for all Solra services.
 * Issues and validates access tokens and refresh tokens.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${solra.jwt.secret:SolraDefaultSecretKeyChangeInProduction!!}") String secret,
            @Value("${solra.jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs,
            @Value("${solra.jwt.refresh-token-expiration-ms:2592000000}") long refreshTokenExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * Generate an access token for the given user.
     */
    public String generateAccessToken(String userId, List<String> roles, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

        JwtBuilder builder = Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(expiry)
                .claim("roles", roles)
                .claim("type", "access");

        if (extraClaims != null) {
            extraClaims.forEach(builder::claim);
        }

        return builder.signWith(signingKey).compact();
    }

    /**
     * Generate a refresh token for the given user.
     */
    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(expiry)
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString())
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validate and parse a token. Returns claims if valid, empty if invalid.
     */
    public Optional<Jws<Claims>> validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Extract user ID from a validated token.
     */
    public String getUserIdFromToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Check if a token has expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
}
