package com.solra.auth.application.dto;

/**
 * Command for user registration (AUTH-001).
 */
public record RegisterCommand(
    String username,
    String password,
    String email,
    String phone,
    String displayName,
    String inviteCode
) {
    public boolean isPhoneRegistration() {
        return phone != null && !phone.isBlank();
    }

    public boolean isUsernameRegistration() {
        return username != null && !username.isBlank();
    }
}
