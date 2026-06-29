package com.solra.auth.domain.model;

import java.time.Instant;

/**
 * Value object — third-party OAuth account binding info.
 */
public class OAuthAccount {

    public enum Provider {
        WECHAT, APPLE, GOOGLE, FACEBOOK
    }

    private Provider provider;
    private String providerUserId;
    private String displayName;
    private String email;
    private String avatarUrl;
    private String encryptedAccessToken;
    private Instant expiresAt;
    private Instant boundAt;

    private OAuthAccount() {}

    public static OAuthAccount bind(Provider provider, String providerUserId,
                                     String displayName, String email, String avatarUrl,
                                     String encryptedAccessToken, Instant expiresAt) {
        OAuthAccount account = new OAuthAccount();
        account.provider = provider;
        account.providerUserId = providerUserId;
        account.displayName = displayName;
        account.email = email;
        account.avatarUrl = avatarUrl;
        account.encryptedAccessToken = encryptedAccessToken;
        account.expiresAt = expiresAt;
        account.boundAt = Instant.now();
        return account;
    }

    public Provider getProvider() { return provider; }
    public String getProviderUserId() { return providerUserId; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getEncryptedAccessToken() { return encryptedAccessToken; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getBoundAt() { return boundAt; }
}
