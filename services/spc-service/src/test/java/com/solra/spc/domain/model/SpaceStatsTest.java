package com.solra.spc.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SpaceStats value object.
 */
@DisplayName("SpaceStats")
class SpaceStatsTest {

    @Nested
    @DisplayName("increment operations")
    class IncrementOperations {

        @Test
        @DisplayName("should start at zero")
        void shouldStartAtZero() {
            SpaceStats stats = new SpaceStats();
            assertThat(stats.getViewCount()).isEqualTo(0);
            assertThat(stats.getLikeCount()).isEqualTo(0);
            assertThat(stats.getShareCount()).isEqualTo(0);
            assertThat(stats.getVisitorCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should increment views correctly")
        void shouldIncrementViews() {
            SpaceStats stats = new SpaceStats();
            stats.incrementViews();
            stats.incrementViews();
            assertThat(stats.getViewCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should increment likes correctly")
        void shouldIncrementLikes() {
            SpaceStats stats = new SpaceStats();
            stats.incrementLikes();
            assertThat(stats.getLikeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should increment shares correctly")
        void shouldIncrementShares() {
            SpaceStats stats = new SpaceStats();
            stats.incrementShares();
            stats.incrementShares();
            stats.incrementShares();
            assertThat(stats.getShareCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should increment visitors correctly")
        void shouldIncrementVisitors() {
            SpaceStats stats = new SpaceStats();
            stats.incrementVisitors();
            assertThat(stats.getVisitorCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should increment conversations correctly")
        void shouldIncrementConversations() {
            SpaceStats stats = new SpaceStats();
            stats.incrementConversations();
            assertThat(stats.getConversationCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should be independent counters")
        void shouldBeIndependent() {
            SpaceStats stats = new SpaceStats();
            stats.incrementViews();
            stats.incrementLikes();
            stats.incrementShares();

            assertThat(stats.getViewCount()).isEqualTo(1);
            assertThat(stats.getLikeCount()).isEqualTo(1);
            assertThat(stats.getShareCount()).isEqualTo(1);
            assertThat(stats.getVisitorCount()).isEqualTo(0);
        }
    }
}
