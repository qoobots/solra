package com.solra.avt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Solra AVT Service - Avatar Conversation Orchestration.
 *
 * Responsibilities:
 * - Multi-turn conversation management
 * - Conversation history storage and retrieval
 * - Avatar memory management (episodic/semantic/preference)
 * - Emotion state tracking and update
 * - LLM routing (delegating to llm-router AI service)
 * - Avatar personality data management
 */
@SpringBootApplication
public class AvtServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AvtServiceApplication.class, args);
    }
}
