package com.solra.spc.domain.service;

import com.solra.spc.domain.model.*;
import com.solra.spc.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SpaceDomainService covering SPC-001/SPC-002/SPC-010.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpaceDomainService")
class SpaceDomainServiceTest {

    @Mock
    private SpaceRepository spaceRepo;
    @Mock
    private UserActionRepository actionRepo;
    @Mock
    private StreamingLoader streamingLoader;
    @Mock
    private RecommendationEngine recommendationEngine;

    private SpaceDomainService spaceDomainService;

    private static final String TEST_SPACE_ID = "spc-001";
    private static final String TEST_USER_ID = "user-001";
    private static final String TEST_CREATOR_ID = "creator-001";

    @BeforeEach
    void setUp() {
        spaceDomainService = new SpaceDomainService(
                spaceRepo, actionRepo, streamingLoader, recommendationEngine
        );
    }

    // ============================================================
    // SPC-001 + SPC-010: getSpace
    // ============================================================
    @Nested
    @DisplayName("getSpace (SPC-001/SPC-010)")
    class GetSpaceTests {

        @Test
        @DisplayName("should return published space and increment views")
        void shouldReturnPublishedSpace() {
            Space space = createPublishedSpace();
            when(spaceRepo.findById(TEST_SPACE_ID)).thenReturn(Optional.of(space));

            Space result = spaceDomainService.getSpace(TEST_SPACE_ID);

            assertThat(result).isNotNull();
            assertThat(result.getStats().getViewCount()).isEqualTo(1); // incremented
            verify(spaceRepo).save(any(Space.class));
        }

        @Test
        @DisplayName("should throw when space not found")
        void shouldThrowWhenSpaceNotFound() {
            when(spaceRepo.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> spaceDomainService.getSpace("nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Space not found");
        }

        @Test
        @DisplayName("should throw when space is not published")
        void shouldThrowWhenNotPublished() {
            Space space = new Space(TEST_SPACE_ID, TEST_CREATOR_ID);
            space.setStatus(SpaceStatus.DRAFT);
            when(spaceRepo.findById(TEST_SPACE_ID)).thenReturn(Optional.of(space));

            assertThatThrownBy(() -> spaceDomainService.getSpace(TEST_SPACE_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not published");
        }
    }

    // ============================================================
    // loadInitialChunks
    // ============================================================
    @Nested
    @DisplayName("loadInitialChunks")
    class LoadInitialChunksTests {

        @Test
        @DisplayName("should delegate to streaming loader")
        void shouldDelegateToStreamingLoader() {
            AssetChunk chunk = new AssetChunk();
            chunk.setAssetId("init");
            when(streamingLoader.getInitialChunks(TEST_SPACE_ID)).thenReturn(List.of(chunk));

            List<AssetChunk> result = spaceDomainService.loadInitialChunks(TEST_SPACE_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAssetId()).isEqualTo("init");
        }
    }

    // ============================================================
    // getPreviewCard / batchGetPreviewCards
    // ============================================================
    @Nested
    @DisplayName("preview cards")
    class PreviewCardTests {

        @Test
        @DisplayName("should return preview card for existing space")
        void shouldReturnPreviewCard() {
            Space space = createPublishedSpace();
            when(spaceRepo.findById(TEST_SPACE_ID)).thenReturn(Optional.of(space));

            Optional<PreviewCard> result = spaceDomainService.getPreviewCard(TEST_SPACE_ID);

            assertThat(result).isPresent();
            assertThat(result.get().getSpaceId()).isEqualTo(TEST_SPACE_ID);
        }

        @Test
        @DisplayName("should return empty for non-existing space")
        void shouldReturnEmptyForNonExisting() {
            when(spaceRepo.findById("nonexistent")).thenReturn(Optional.empty());

            Optional<PreviewCard> result = spaceDomainService.getPreviewCard("nonexistent");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should batch get preview cards")
        void shouldBatchGetPreviewCards() {
            Space space1 = createPublishedSpace();
            Space space2 = createPublishedSpace();
            space2.setSpaceId("spc-002");
            when(spaceRepo.findByIds(List.of("spc-001", "spc-002")))
                    .thenReturn(List.of(space1, space2));

            List<PreviewCard> result = spaceDomainService.batchGetPreviewCards(List.of("spc-001", "spc-002"));

            assertThat(result).hasSize(2);
        }
    }

    // ============================================================
    // listRecommendations (SPC-002)
    // ============================================================
    @Nested
    @DisplayName("listRecommendations (SPC-002)")
    class ListRecommendationsTests {

        @Test
        @DisplayName("should route to personalized recommend by default")
        void shouldRouteToPersonalizedRecommend() {
            when(recommendationEngine.recommend(eq(TEST_USER_ID), eq(10), isNull()))
                    .thenReturn(List.of());

            List<Recommendation> result = spaceDomainService.listRecommendations(
                    TEST_USER_ID, "default", 0, 10, null);

            verify(recommendationEngine).recommend(TEST_USER_ID, 10, null);
        }

        @Test
        @DisplayName("should route to popular mode")
        void shouldRouteToPopular() {
            when(recommendationEngine.popular(10, null)).thenReturn(List.of());

            spaceDomainService.listRecommendations(TEST_USER_ID, "popular", 0, 10, null);

            verify(recommendationEngine).popular(10, null);
        }

        @Test
        @DisplayName("should route to newest mode")
        void shouldRouteToNewest() {
            when(recommendationEngine.newest(10, null)).thenReturn(List.of());

            spaceDomainService.listRecommendations(TEST_USER_ID, "newest", 0, 10, null);

            verify(recommendationEngine).newest(10, null);
        }

        @Test
        @DisplayName("should route to trending mode")
        void shouldRouteToTrending() {
            when(recommendationEngine.trending(10, null)).thenReturn(List.of());

            spaceDomainService.listRecommendations(TEST_USER_ID, "trending", 0, 10, null);

            verify(recommendationEngine).trending(10, null);
        }
    }

    // ============================================================
    // reportUserAction
    // ============================================================
    @Nested
    @DisplayName("reportUserAction")
    class ReportUserActionTests {

        @Test
        @DisplayName("should save action and report to recommendation engine")
        void shouldSaveAndReportAction() {
            UserAction action = new UserAction();
            action.setUserId(TEST_USER_ID);
            action.setSpaceId(TEST_SPACE_ID);
            action.setActionType(UserActionType.VIEW);

            spaceDomainService.reportUserAction(action);

            verify(actionRepo).save(action);
            verify(recommendationEngine).reportAction(action);
        }

        @Test
        @DisplayName("should increment view count for VIEW action")
        void shouldIncrementViewCount() {
            UserAction action = new UserAction();
            action.setUserId(TEST_USER_ID);
            action.setSpaceId(TEST_SPACE_ID);
            action.setActionType(UserActionType.VIEW);

            spaceDomainService.reportUserAction(action);

            verify(spaceRepo).incrementViewCount(TEST_SPACE_ID);
        }

        @Test
        @DisplayName("should increment like count for LIKE action")
        void shouldIncrementLikeCount() {
            UserAction action = new UserAction();
            action.setUserId(TEST_USER_ID);
            action.setSpaceId(TEST_SPACE_ID);
            action.setActionType(UserActionType.LIKE);

            spaceDomainService.reportUserAction(action);

            verify(spaceRepo).incrementLikeCount(TEST_SPACE_ID);
        }

        @Test
        @DisplayName("should increment share count for SHARE action")
        void shouldIncrementShareCount() {
            UserAction action = new UserAction();
            action.setUserId(TEST_USER_ID);
            action.setSpaceId(TEST_SPACE_ID);
            action.setActionType(UserActionType.SHARE);

            spaceDomainService.reportUserAction(action);

            verify(spaceRepo).incrementShareCount(TEST_SPACE_ID);
        }

        @Test
        @DisplayName("should increment visitors for ENTER action")
        void shouldIncrementVisitorsForEnter() {
            Space space = createPublishedSpace();
            UserAction action = new UserAction();
            action.setUserId(TEST_USER_ID);
            action.setSpaceId(TEST_SPACE_ID);
            action.setActionType(UserActionType.ENTER);
            when(spaceRepo.findById(TEST_SPACE_ID)).thenReturn(Optional.of(space));

            spaceDomainService.reportUserAction(action);

            verify(spaceRepo).save(argThat(s -> s.getStats().getVisitorCount() == 1));
        }

        @Test
        @DisplayName("should not throw when stats update fails")
        void shouldNotThrowOnStatsFailure() {
            UserAction action = new UserAction();
            action.setUserId(TEST_USER_ID);
            action.setSpaceId(TEST_SPACE_ID);
            action.setActionType(UserActionType.VIEW);
            doThrow(new RuntimeException("DB error")).when(spaceRepo).incrementViewCount(TEST_SPACE_ID);

            // Should not throw — exception is caught and logged
            assertThatCode(() -> spaceDomainService.reportUserAction(action)).doesNotThrowAnyException();
        }
    }

    // ============================================================
    // listSpaces
    // ============================================================
    @Nested
    @DisplayName("listSpaces")
    class ListSpacesTests {

        @Test
        @DisplayName("should delegate to repository")
        void shouldDelegateToRepository() {
            when(spaceRepo.findPublished(0, 10, null, "newest")).thenReturn(List.of());

            List<Space> result = spaceDomainService.listSpaces(0, 10, null, "newest");

            assertThat(result).isEmpty();
            verify(spaceRepo).findPublished(0, 10, null, "newest");
        }
    }

    // ---- helper ----
    private Space createPublishedSpace() {
        Space space = new Space(TEST_SPACE_ID, TEST_CREATOR_ID);
        space.setStatus(SpaceStatus.PUBLISHED);
        SpaceMeta meta = new SpaceMeta();
        meta.setTitle("Test Space");
        meta.setThumbnailUrl("https://example.com/thumb.jpg");
        space.setMeta(meta);
        space.setTags(List.of("test", "mock"));
        return space;
    }
}
