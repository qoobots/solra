package com.solra.spc.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Space aggregate root.
 */
@DisplayName("Space")
class SpaceTest {

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("should start as DRAFT")
        void shouldStartAsDraft() {
            Space space = new Space("spc-1", "creator-1");
            assertThat(space.getStatus()).isEqualTo(SpaceStatus.DRAFT);
        }

        @Test
        @DisplayName("should initialize stats")
        void shouldInitializeStats() {
            Space space = new Space("spc-1", "creator-1");
            assertThat(space.getStats()).isNotNull();
            assertThat(space.getStats().getViewCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should set timestamps")
        void shouldSetTimestamps() {
            Space space = new Space("spc-1", "creator-1");
            assertThat(space.getCreatedAt()).isNotNull();
            assertThat(space.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("state transitions")
    class StateTransitions {

        @Test
        @DisplayName("should publish from DRAFT")
        void shouldPublishFromDraft() {
            Space space = new Space("spc-1", "creator-1");
            space.publish();
            assertThat(space.getStatus()).isEqualTo(SpaceStatus.PUBLISHED);
        }

        @Test
        @DisplayName("should publish from REVIEWING")
        void shouldPublishFromReviewing() {
            Space space = new Space("spc-1", "creator-1");
            space.setStatus(SpaceStatus.REVIEWING);
            space.publish();
            assertThat(space.getStatus()).isEqualTo(SpaceStatus.PUBLISHED);
        }

        @Test
        @DisplayName("should not publish from ARCHIVED")
        void shouldNotPublishFromArchived() {
            Space space = new Space("spc-1", "creator-1");
            space.archive();
            space.publish();
            assertThat(space.getStatus()).isEqualTo(SpaceStatus.ARCHIVED);
        }

        @Test
        @DisplayName("should archive from any state")
        void shouldArchiveFromAnyState() {
            Space space = new Space("spc-1", "creator-1");
            space.publish();
            space.archive();
            assertThat(space.getStatus()).isEqualTo(SpaceStatus.ARCHIVED);
        }
    }

    @Nested
    @DisplayName("statistics increments")
    class StatisticsIncrements {

        @Test
        @DisplayName("should increment views")
        void shouldIncrementViews() {
            Space space = new Space("spc-1", "creator-1");
            space.incrementViews();
            assertThat(space.getStats().getViewCount()).isEqualTo(1);
            space.incrementViews();
            assertThat(space.getStats().getViewCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should increment likes")
        void shouldIncrementLikes() {
            Space space = new Space("spc-1", "creator-1");
            space.incrementLikes();
            assertThat(space.getStats().getLikeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should increment shares")
        void shouldIncrementShares() {
            Space space = new Space("spc-1", "creator-1");
            space.incrementShares();
            assertThat(space.getStats().getShareCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should increment visitors")
        void shouldIncrementVisitors() {
            Space space = new Space("spc-1", "creator-1");
            space.incrementVisitors();
            assertThat(space.getStats().getVisitorCount()).isEqualTo(1);
        }
    }
}
