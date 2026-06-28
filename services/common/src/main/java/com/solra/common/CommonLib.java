package com.solra.common;

/**
 * Common shared library - no Spring Boot application entry.
 * Provides shared DTOs, utilities, security config, and observability
 * for all Solra microservices.
 */
public final class CommonLib {
    private CommonLib() {}

    public static final String VERSION = "0.1.0-SNAPSHOT";
}
