package com.solra.auth.application.dto;

/**
 * AUTH-002: OAuth 第三方登录命令。
 */
public record OAuthLoginCommand(
        String provider,        // WECHAT, APPLE, GOOGLE, FACEBOOK
        String providerUserId,  // 第三方平台用户ID
        String displayName,
        String email,
        String avatarUrl,
        String accessToken,
        String expiresAt,       // ISO 8601
        String ipAddress,
        String deviceInfo
) {}

/**
 * AUTH-002: OAuth 绑定/解绑命令。
 */
public record OAuthBindCommand(
        String userId,
        String provider,
        String providerUserId,
        String displayName,
        String email,
        String avatarUrl,
        String accessToken,
        String expiresAt
) {}

public record OAuthUnbindCommand(
        String userId,
        String provider
) {}
