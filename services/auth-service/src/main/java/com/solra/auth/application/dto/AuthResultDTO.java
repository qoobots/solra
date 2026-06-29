package com.solra.auth.application.dto;

import java.util.List;

/**
 * Result of authentication — returned after successful login/register (AUTH-001).
 */
public record AuthResultDTO(
    String userId,
    String username,
    String displayName,
    String phone,
    String email,
    String avatarUrl,
    String accessToken,
    String refreshToken,
    long expiresInSeconds,
    List<String> roles
) {}
