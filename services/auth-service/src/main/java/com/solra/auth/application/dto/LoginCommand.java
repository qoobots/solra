package com.solra.auth.application.dto;

import com.solra.auth.domain.model.LoginMethod;

/**
 * Command for user login (AUTH-001).
 */
public record LoginCommand(
    String credential,        // username / phone / email
    String password,
    LoginMethod method,
    String verificationCode,
    String deviceInfo,
    String ipAddress
) {}
