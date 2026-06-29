package com.solra.auth.domain.repository;

import com.solra.auth.domain.model.LoginSession;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for LoginSession entity.
 * AUTH-005: Extended with device-aware query methods.
 */
public interface LoginSessionRepository {
    Optional<LoginSession> findById(String sessionId);
    Optional<LoginSession> findByRefreshToken(String refreshToken);
    List<LoginSession> findByUserId(String userId);
    List<LoginSession> findByUserIdAndDeviceId(String userId, String deviceId);  // AUTH-005
    LoginSession save(LoginSession session);
    void deleteByUserId(String userId);
    void deleteByUserIdAndDeviceId(String userId, String deviceId);  // AUTH-005: revoke single device
    void deleteById(String sessionId);
    int countByUserId(String userId);  // AUTH-005: count active sessions
}
