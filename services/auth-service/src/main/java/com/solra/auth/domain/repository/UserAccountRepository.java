package com.solra.auth.domain.repository;

import com.solra.auth.domain.model.UserAccount;
import java.util.Optional;

/**
 * Repository interface for UserAccount aggregate.
 */
public interface UserAccountRepository {
    Optional<UserAccount> findById(String userId);
    Optional<UserAccount> findByPhone(String phone);
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByEmail(String email);
    /** AUTH-002: Find user by OAuth provider + providerUserId */
    Optional<UserAccount> findByOAuthProviderAndId(String provider, String providerUserId);
    boolean existsByPhone(String phone);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    UserAccount save(UserAccount account);
    void delete(String userId);
}
