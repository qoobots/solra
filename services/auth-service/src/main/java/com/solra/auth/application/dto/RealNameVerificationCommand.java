package com.solra.auth.application.dto;

import java.time.LocalDate;

/**
 * Command for submitting real-name verification (AUTH-004).
 */
public record RealNameVerificationCommand(
    String userId,
    String realName,
    String idNumber,
    LocalDate birthDate
) {}
