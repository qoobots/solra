package com.solra.auth.infrastructure.oauth;

import com.solra.auth.domain.model.OAuthAccount;
import com.solra.auth.domain.model.UserAccount;
import com.solra.auth.domain.repository.UserAccountRepository;
import com.solra.auth.domain.service.OAuthAuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * DefaultOAuthAuthenticationService — AUTH-002 第三方登录默认实现。
 * 支持微信/Apple/Google/Facebook 登录 → 自动注册/绑定。
 */
@Component
public class DefaultOAuthAuthenticationService implements OAuthAuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultOAuthAuthenticationService.class);

    private final UserAccountRepository accountRepo;

    public DefaultOAuthAuthenticationService(UserAccountRepository accountRepo) {
        this.accountRepo = accountRepo;
    }

    @Override
    public UserAccount loginByOAuth(String provider, String providerUserId, String displayName,
                                     String email, String avatarUrl, String accessToken, String expiresAt) {
        OAuthAccount.Provider priv = OAuthAccount.Provider.valueOf(provider.toUpperCase());

        // 1. 查找是否已有绑定此 OAuth 账号的用户
        Optional<UserAccount> existing = accountRepo.findByOAuthProviderAndId(provider, providerUserId);
        if (existing.isPresent()) {
            UserAccount account = existing.get();
            // 更新 token 信息
            OAuthAccount oauth = OAuthAccount.bind(priv, providerUserId, displayName, email, avatarUrl,
                    accessToken, expiresAt != null ? Instant.parse(expiresAt) : null);
            account.bindOAuth(oauth);
            accountRepo.save(account);
            log.info("AUTH-002 OAuth login (existing): user={} provider={}", account.getUserId(), provider);
            return account;
        }

        // 2. 新用户：创建账号并绑定 OAuth
        String username = (provider.toLowerCase() + "_" + providerUserId).substring(0, 32);
        UserAccount account = UserAccount.registerByUsername(username, "OAUTH_" + accessToken.hashCode(), displayName);
        if (email != null) account.updateProfile(displayName, email, avatarUrl);

        OAuthAccount oauth = OAuthAccount.bind(priv, providerUserId, displayName, email, avatarUrl,
                accessToken, expiresAt != null ? Instant.parse(expiresAt) : null);
        account.bindOAuth(oauth);

        UserAccount saved = accountRepo.save(account);
        log.info("AUTH-002 OAuth login (new user): user={} provider={} providerUserId={}",
                saved.getUserId(), provider, providerUserId);
        return saved;
    }

    @Override
    public UserAccount bindOAuth(String userId, String provider, String providerUserId,
                                  String displayName, String email, String avatarUrl,
                                  String accessToken, String expiresAt) {
        UserAccount account = accountRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        OAuthAccount.Provider priv = OAuthAccount.Provider.valueOf(provider.toUpperCase());
        OAuthAccount oauth = OAuthAccount.bind(priv, providerUserId, displayName, email, avatarUrl,
                accessToken, expiresAt != null ? Instant.parse(expiresAt) : null);
        account.bindOAuth(oauth);

        UserAccount saved = accountRepo.save(account);
        log.info("AUTH-002 OAuth bound: user={} provider={}", userId, provider);
        return saved;
    }

    @Override
    public UserAccount unbindOAuth(String userId, String provider) {
        UserAccount account = accountRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        account.unbindOAuth(provider);
        UserAccount saved = accountRepo.save(account);
        log.info("AUTH-002 OAuth unbound: user={} provider={}", userId, provider);
        return saved;
    }

    @Override
    public Optional<UserAccount> findByOAuth(String provider, String providerUserId) {
        return accountRepo.findByOAuthProviderAndId(provider, providerUserId);
    }
}
