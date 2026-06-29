package com.solra.auth.infrastructure.security;

import com.solra.auth.domain.service.AuthDomainService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt implementation of password encoding.
 */
@Component
public class BCryptPasswordEncoderAdapter implements AuthDomainService.PasswordEncoder {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
