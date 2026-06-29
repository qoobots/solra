package com.solra.auth.infrastructure.persistence;

import com.solra.auth.domain.model.LoginSession;
import com.solra.auth.domain.repository.LoginSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AUTH-005: In-memory implementation of LoginSessionRepository.
 * Production should use Redis or PostgreSQL.
 */
@Component
public class InMemoryLoginSessionRepository implements LoginSessionRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryLoginSessionRepository.class);

    private final Map<String, LoginSession> sessionsById = new ConcurrentHashMap<>();

    @Override
    public Optional<LoginSession> findById(String sessionId) {
        return Optional.ofNullable(sessionsById.get(sessionId))
                .filter(s -> !s.isExpired());
    }

    @Override
    public Optional<LoginSession> findByRefreshToken(String refreshToken) {
        return sessionsById.values().stream()
                .filter(s -> refreshToken.equals(s.getRefreshToken()))
                .filter(s -> !s.isExpired())
                .findFirst();
    }

    @Override
    public List<LoginSession> findByUserId(String userId) {
        return sessionsById.values().stream()
                .filter(s -> userId.equals(s.getUserId()))
                .filter(s -> !s.isExpired())
                .collect(Collectors.toList());
    }

    @Override
    public List<LoginSession> findByUserIdAndDeviceId(String userId, String deviceId) {
        return sessionsById.values().stream()
                .filter(s -> userId.equals(s.getUserId()) && deviceId.equals(s.getDeviceId()))
                .filter(s -> !s.isExpired())
                .collect(Collectors.toList());
    }

    @Override
    public LoginSession save(LoginSession session) {
        sessionsById.put(session.getSessionId(), session);
        log.debug("LoginSession saved: {} for user={} device={}", session.getSessionId(),
                session.getUserId(), session.getDeviceId());
        return session;
    }

    @Override
    public void deleteByUserId(String userId) {
        sessionsById.values().removeIf(s -> userId.equals(s.getUserId()));
        log.debug("All sessions deleted for user={}", userId);
    }

    @Override
    public void deleteByUserIdAndDeviceId(String userId, String deviceId) {
        sessionsById.values().removeIf(s ->
                userId.equals(s.getUserId()) && deviceId.equals(s.getDeviceId()));
        log.debug("Sessions deleted for user={} device={}", userId, deviceId);
    }

    @Override
    public void deleteById(String sessionId) {
        sessionsById.remove(sessionId);
        log.debug("Session deleted: {}", sessionId);
    }

    @Override
    public int countByUserId(String userId) {
        return (int) sessionsById.values().stream()
                .filter(s -> userId.equals(s.getUserId()))
                .filter(s -> !s.isExpired())
                .count();
    }
}
