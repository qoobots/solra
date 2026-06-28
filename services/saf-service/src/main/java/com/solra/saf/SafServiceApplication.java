package com.solra.saf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Solra SAF Service - Content Safety & Moderation.
 *
 * Responsibilities:
 * - AI-powered text/image/audio content moderation
 * - Manual review workflow orchestration
 * - Safety score computation
 * - User appeal processing
 * - Real-time content filtering for avatar conversations
 */
@SpringBootApplication
public class SafServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SafServiceApplication.class, args);
    }
}
