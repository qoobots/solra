package com.solra.avt.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MemoryEntry entity.
 */
@DisplayName("MemoryEntry")
class MemoryEntryTest {

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("should clamp importance to 0..1")
        void shouldClampImportance() {
            MemoryEntry mem = new MemoryEntry("m1", "u1", MemoryType.EPISODIC, "content", 1.5f);
            assertThat(mem.getImportance()).isEqualTo(1.0f);

            mem = new MemoryEntry("m2", "u1", MemoryType.SEMANTIC, "content", -0.5f);
            assertThat(mem.getImportance()).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("should set createdAt and lastAccessed")
        void shouldSetTimestamps() {
            MemoryEntry mem = new MemoryEntry("m1", "u1", MemoryType.FACT, "content", 0.5f);
            assertThat(mem.getCreatedAt()).isNotNull();
            assertThat(mem.getLastAccessed()).isNotNull();
            assertThat(mem.getLastAccessed()).isEqualTo(mem.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("access")
    class Access {

        @Test
        @DisplayName("should update lastAccessed")
        void shouldUpdateLastAccessed() throws InterruptedException {
            MemoryEntry mem = new MemoryEntry("m1", "u1", MemoryType.EPISODIC, "content", 0.5f);
            var before = mem.getLastAccessed();
            Thread.sleep(1);
            mem.access();
            assertThat(mem.getLastAccessed()).isAfter(before);
        }
    }

    @Nested
    @DisplayName("isExpired")
    class IsExpired {

        @Test
        @DisplayName("should return false when no expiry set")
        void shouldReturnFalseForNoExpiry() {
            MemoryEntry mem = new MemoryEntry("m1", "u1", MemoryType.EPISODIC, "content", 0.5f);
            assertThat(mem.isExpired()).isFalse();
        }

        @Test
        @DisplayName("should return true when expired")
        void shouldReturnTrueWhenExpired() {
            MemoryEntry mem = new MemoryEntry("m1", "u1", MemoryType.EPISODIC, "content", 0.5f);
            mem.setExpiresAt(Instant.now().minusSeconds(60));
            assertThat(mem.isExpired()).isTrue();
        }

        @Test
        @DisplayName("should return false when not yet expired")
        void shouldReturnFalseWhenNotExpired() {
            MemoryEntry mem = new MemoryEntry("m1", "u1", MemoryType.EPISODIC, "content", 0.5f);
            mem.setExpiresAt(Instant.now().plusSeconds(3600));
            assertThat(mem.isExpired()).isFalse();
        }
    }
}
