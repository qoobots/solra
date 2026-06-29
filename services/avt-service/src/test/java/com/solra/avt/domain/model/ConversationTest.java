package com.solra.avt.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Conversation aggregate root.
 */
@DisplayName("Conversation")
class ConversationTest {

    @Nested
    @DisplayName("state transitions")
    class StateTransitions {

        @Test
        @DisplayName("should start as ACTIVE")
        void shouldStartActive() {
            Conversation conv = new Conversation("c1", "u1", "s1", "a1");
            assertThat(conv.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        }

        @Test
        @DisplayName("should pause from ACTIVE")
        void shouldPauseFromActive() {
            Conversation conv = new Conversation("c1", "u1", "s1", "a1");
            conv.pause();
            assertThat(conv.getStatus()).isEqualTo(ConversationStatus.PAUSED);
        }

        @Test
        @DisplayName("should resume from PAUSED")
        void shouldResumeFromPaused() {
            Conversation conv = new Conversation("c1", "u1", "s1", "a1");
            conv.pause();
            conv.resume();
            assertThat(conv.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        }

        @Test
        @DisplayName("should not pause from ENDED")
        void shouldNotPauseFromEnded() {
            Conversation conv = new Conversation("c1", "u1", "s1", "a1");
            conv.end();
            conv.pause();
            assertThat(conv.getStatus()).isEqualTo(ConversationStatus.ENDED);
        }

        @Test
        @DisplayName("should not resume from ACTIVE")
        void shouldNotResumeFromActive() {
            Conversation conv = new Conversation("c1", "u1", "s1", "a1");
            conv.resume();
            assertThat(conv.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        }

        @Test
        @DisplayName("should end from any state")
        void shouldEndFromAnyState() {
            Conversation conv = new Conversation("c1", "u1", "s1", "a1");
            conv.pause();
            conv.end();
            assertThat(conv.getStatus()).isEqualTo(ConversationStatus.ENDED);
        }

        @Test
        @DisplayName("touch should update timestamp")
        void touchShouldUpdateTimestamp() throws InterruptedException {
            Conversation conv = new Conversation("c1", "u1", "s1", "a1");
            var before = conv.getUpdatedAt();
            Thread.sleep(1);
            conv.touch();
            assertThat(conv.getUpdatedAt()).isAfter(before);
        }
    }
}
