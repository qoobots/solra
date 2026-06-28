package com.solra.spc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Solra SPC Service - Space Consumption & Discovery.
 *
 * Responsibilities:
 * - Space metadata CRUD
 * - Personalized space recommendation orchestration
 * - Full-text space search (Elasticsearch)
 * - Preview card generation
 * - CDN distribution orchestration
 * - User interaction event collection (for recommendation feedback)
 */
@SpringBootApplication
public class SpcServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpcServiceApplication.class, args);
    }
}
