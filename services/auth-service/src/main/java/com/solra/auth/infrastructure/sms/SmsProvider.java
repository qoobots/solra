package com.solra.auth.infrastructure.sms;

/**
 * Interface for SMS verification code sending (AUTH-001).
 * Implementation will use cloud SMS provider (e.g., Alibaba Cloud SMS).
 */
public interface SmsProvider {
    /**
     * Send a verification code to the specified phone number.
     * @param phone  the target phone number
     * @param code   the verification code
     * @return true if sent successfully
     */
    boolean sendVerificationCode(String phone, String code);
}
