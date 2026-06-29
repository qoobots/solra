package com.solra.auth.domain.service;

import com.solra.auth.domain.model.OAuthAccount;
import com.solra.auth.domain.model.UserAccount;
import java.util.Optional;

/**
 * OAuthAuthenticationService — AUTH-002 第三方登录领域服务接口。
 * 支持微信、Apple、Google、Facebook 四种 OAuth Provider。
 */
public interface OAuthAuthenticationService {

    /**
     * 通过 OAuth Provider 回调数据登录或注册。
     *
     * @param provider      OAuth 提供者
     * @param providerUserId 第三方平台用户唯一ID
     * @param displayName   用户昵称
     * @param email         邮箱
     * @param avatarUrl     头像URL
     * @param accessToken   access token
     * @param expiresAt      token过期时间
     * @return 登录或注册后的用户账号
     */
    UserAccount loginByOAuth(String provider, String providerUserId, String displayName,
                             String email, String avatarUrl, String accessToken, String expiresAt);

    /** 绑定 OAuth 账号到已有用户 */
    UserAccount bindOAuth(String userId, String provider, String providerUserId,
                          String displayName, String email, String avatarUrl,
                          String accessToken, String expiresAt);

    /** 解绑 OAuth 账号 */
    UserAccount unbindOAuth(String userId, String provider);

    /** 按 provider + providerUserId 查找用户 */
    Optional<UserAccount> findByOAuth(String provider, String providerUserId);
}
