package com.solra.auth.domain.event;

import com.solra.auth.domain.model.LoginMethod;
import java.time.Instant;

/**
 * Domain event — emitted when a new user is registered (AUTH-001).
 */
public record UserRegisteredEvent(
    String userId,
    String phone,
    String username,
    LoginMethod method,
    Instant registeredAt
) {}

/**
 * Domain event — emitted when a user logs in.
 */
public record UserLoggedInEvent(
    String userId,
    String sessionId,
    LoginMethod method,
    String ipAddress,
    Instant loginAt
) {}

/**
 * Domain event — emitted when real-name verification is approved (AUTH-004).
 */
public record RealNameVerifiedEvent(
    String userId,
    boolean isMinor,
    Instant verifiedAt
) {}
