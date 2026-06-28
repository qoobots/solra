package com.solra.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Solra Auth Service - Identity & Access Management.
 *
 * Responsibilities:
 * - User registration and login (password/OAuth/biometric)
 * - JWT token issuance, refresh, and validation
 * - RBAC permission management
 * - OAuth account linking (WeChat/Apple/Google)
 * - Session management
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
