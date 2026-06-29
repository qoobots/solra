package com.solra.auth.infrastructure.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Mock SMS provider for development environment.
 * In production, replace with actual cloud SMS provider.
 */
@Service
public class MockSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(MockSmsProvider.class);

    @Override
    public boolean sendVerificationCode(String phone, String code) {
        log.info("[MOCK SMS] Sending verification code {} to phone {}", code,
                com.solra.common.security.SecurityUtils.maskPhoneNumber(phone));
        return true;
    }
}
