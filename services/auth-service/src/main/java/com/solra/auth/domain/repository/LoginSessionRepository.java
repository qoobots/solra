package com.solra.auth.domain.repository;

import com.solra.auth.domain.model.LoginSession;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for LoginSession entity.
 */
public interface LoginSessionRepository {
    Optional<LoginSession> findById(String sessionId);
    Optional<LoginSession> findByRefreshToken(String refreshToken);
    List<LoginSession> findByUserId(String userId);
    LoginSession save(LoginSession session);
    void deleteByUserId(String userId);
    void deleteById(String sessionId);
}
