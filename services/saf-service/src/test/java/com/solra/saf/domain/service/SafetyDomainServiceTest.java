package com.solra.saf.domain.service;

import com.solra.saf.domain.model.*;
import com.solra.saf.domain.repository.ReviewCaseRepository;
import com.solra.common.exception.SolraException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SafetyDomainService 单元测试")
class SafetyDomainServiceTest {

    @Mock
    private ReviewCaseRepository reviewCaseRepository;

    @Mock
    private ContentFilter contentFilter;

    private SafetyDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new SafetyDomainService(reviewCaseRepository, List.of(contentFilter));
        lenient().when(reviewCaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(contentFilter.supports(any())).thenReturn(true);
    }

    // ========== SAF-001: submitReview ==========

    @Nested
    @DisplayName("SAF-001 submitReview — 内容提交审核")
    class SubmitReviewTests {

        @Test
        @DisplayName("AUTOMATIC 类型通过关键词过滤 → 案例为 APPROVED")
        void automaticTypeFilterPassed() {
            ContentTarget target = ContentTarget.text("ct1", "hello world");
            when(contentFilter.filter("hello world"))
                    .thenReturn(ContentFilter.FilterResult.pass(SafetyScore.safe("keyword-v1")));

            ReviewCase result = domainService.submitReview("user1", target, ReviewType.AUTOMATIC, ReviewPriority.NORMAL);

            assertNotNull(result);
            assertEquals("user1", result.getUserId());
            assertEquals(ReviewDecision.APPROVED, result.getDecision());
            assertEquals(ReviewStatus.COMPLETED, result.getStatus()); // confidence=1.0 >= 0.9
            verify(reviewCaseRepository).save(any());
        }

        @Test
        @DisplayName("AUTOMATIC 类型被关键词拒绝 → 案例为 REJECTED")
        void automaticTypeFilterRejected() {
            ContentTarget target = ContentTarget.text("ct2", "self harm content");
            PolicyViolation violation = PolicyViolation.detected("p1", "self-harm",
                    PolicyViolation.Category.SELF_HARM, PolicyViolation.Severity.CRITICAL,
                    "contains self-harm", "self harm");
            SafetyScore unsafeScore = SafetyScore.unsafe(0.0f, List.of(), "keyword-v1");
            when(contentFilter.filter("self harm content"))
                    .thenReturn(ContentFilter.FilterResult.reject(unsafeScore, List.of(violation)));

            ReviewCase result = domainService.submitReview("user1", target, ReviewType.AUTOMATIC, ReviewPriority.HIGH);

            assertEquals(ReviewDecision.REJECTED, result.getDecision());
            assertTrue(result.isRejected());
            assertEquals(ReviewStatus.COMPLETED, result.getStatus()); // confidence=1.0 >= 0.9
        }

        @Test
        @DisplayName("MANUAL 类型跳过自动过滤 → 案例为 PENDING")
        void manualTypeSkipsAutoFilter() {
            ContentTarget target = ContentTarget.text("ct3", "manual review content");

            ReviewCase result = domainService.submitReview("user2", target, ReviewType.MANUAL, ReviewPriority.NORMAL);

            assertEquals(ReviewStatus.PENDING, result.getStatus());
            assertNull(result.getDecision());
            verify(contentFilter, never()).filter(any());
        }

        @Test
        @DisplayName("HYBRID 类型执行自动过滤")
        void hybridTypeExecutesAutoFilter() {
            ContentTarget target = ContentTarget.text("ct4", "hybrid check");
            when(contentFilter.filter("hybrid check"))
                    .thenReturn(ContentFilter.FilterResult.pass(SafetyScore.safe("keyword-v1")));

            ReviewCase result = domainService.submitReview("user3", target, ReviewType.HYBRID, ReviewPriority.NORMAL);

            assertEquals(ReviewDecision.APPROVED, result.getDecision());
        }

        @Test
        @DisplayName("无匹配过滤器时 filterContent 返回 null → 案例保持 PENDING")
        void noMatchingFilterReturnsPending() {
            when(contentFilter.supports(any())).thenReturn(false);
            ContentTarget target = ContentTarget.avatarSpeech("ct5", "dialogue");

            ReviewCase result = domainService.submitReview("user4", target, ReviewType.AUTOMATIC, ReviewPriority.LOW);

            assertEquals(ReviewStatus.PENDING, result.getStatus());
        }
    }

    // ========== SAF-002: filterDialogue ==========

    @Nested
    @DisplayName("SAF-002 filterDialogue — 实时对话安全过滤")
    class FilterDialogueTests {

        @Test
        @DisplayName("安全对话通过过滤 → 返回 true")
        void safeDialoguePasses() {
            when(contentFilter.supports(ContentType.AVATAR_SPEECH)).thenReturn(true);
            when(contentFilter.filter("hello, how are you?"))
                    .thenReturn(ContentFilter.FilterResult.pass(SafetyScore.safe("keyword-v1")));

            boolean result = domainService.filterDialogue("user1", "hello, how are you?");

            assertTrue(result);
            // 不应该创建任何 ReviewCase（因为通过了）
            verify(reviewCaseRepository, never()).save(any());
        }

        @Test
        @DisplayName("危险对话被过滤 → 返回 false 并创建 URGENT 案例")
        void dangerousDialogueBlocked() {
            PolicyViolation violation = PolicyViolation.detected("p1", "hate-speech",
                    PolicyViolation.Category.HATE_SPEECH, PolicyViolation.Severity.MEDIUM,
                    "hate speech detected", "hate");
            SafetyScore unsafeScore = SafetyScore.unsafe(0.2f, List.of(), "keyword-v1");
            when(contentFilter.supports(ContentType.AVATAR_SPEECH)).thenReturn(true);
            when(contentFilter.filter("i hate you all"))
                    .thenReturn(ContentFilter.FilterResult.reject(unsafeScore, List.of(violation)));

            boolean result = domainService.filterDialogue("user2", "i hate you all");

            assertFalse(result);
            verify(reviewCaseRepository, atLeastOnce()).save(any());
        }

        @Test
        @DisplayName("无适用过滤器时直接放行 → 返回 true")
        void noFilterApplicablePasses() {
            when(contentFilter.supports(ContentType.AVATAR_SPEECH)).thenReturn(false);

            boolean result = domainService.filterDialogue("user3", "anything");

            assertTrue(result);
            verify(reviewCaseRepository, never()).save(any());
        }
    }

    // ========== SAF-001: queryReview ==========

    @Nested
    @DisplayName("queryReview — 查询审核结果")
    class QueryReviewTests {

        @Test
        @DisplayName("存在案例时返回案例")
        void existingCaseFound() {
            ReviewCase existing = ReviewCase.create("user1",
                    ContentTarget.text("ct1", "text"), ReviewType.AUTOMATIC, ReviewPriority.NORMAL);
            when(reviewCaseRepository.findById(existing.getCaseId())).thenReturn(Optional.of(existing));

            ReviewCase result = domainService.queryReview(existing.getCaseId());

            assertNotNull(result);
            assertEquals(existing.getCaseId(), result.getCaseId());
        }

        @Test
        @DisplayName("案例不存在时抛出 NotFoundException")
        void notFoundThrowsException() {
            when(reviewCaseRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThrows(SolraException.NotFoundException.class,
                    () -> domainService.queryReview("nonexistent"));
        }
    }

    // ========== getSafetyScore ==========

    @Nested
    @DisplayName("getSafetyScore — 获取安全评分")
    class GetSafetyScoreTests {

        @Test
        @DisplayName("内容通过过滤返回安全评分")
        void contentSafe() {
            SafetyScore safeScore = SafetyScore.safe("keyword-v1");
            when(contentFilter.filter("safe text"))
                    .thenReturn(ContentFilter.FilterResult.pass(safeScore));

            SafetyScore result = domainService.getSafetyScore("safe text", ContentType.TEXT);

            assertEquals(1.0f, result.getOverallScore(), 0.001);
            assertTrue(result.isSafe(0.5f));
        }

        @Test
        @DisplayName("无适用过滤器返回默认安全评分")
        void noFilterReturnsDefaultSafe() {
            when(contentFilter.supports(any())).thenReturn(false);

            SafetyScore result = domainService.getSafetyScore("anything", ContentType.IMAGE);

            assertEquals(1.0f, result.getOverallScore(), 0.001);
            assertEquals("no-filter", result.getModelVersion());
        }
    }

    // ========== batchReview ==========

    @Nested
    @DisplayName("batchReview — 批量审核")
    class BatchReviewTests {

        @Test
        @DisplayName("批量审核多个内容返回等量案例")
        void batchReviewReturnsAllCases() {
            ContentTarget t1 = ContentTarget.text("b1", "text1");
            ContentTarget t2 = ContentTarget.text("b2", "text2");
            when(contentFilter.filter(any()))
                    .thenReturn(ContentFilter.FilterResult.pass(SafetyScore.safe("keyword-v1")));

            List<ReviewCase> results = domainService.batchReview("user1", List.of(t1, t2),
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);

            assertEquals(2, results.size());
            verify(reviewCaseRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("空列表批量审核返回空列表")
        void emptyBatchReturnsEmpty() {
            List<ReviewCase> results = domainService.batchReview("user1", List.of(),
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);

            assertTrue(results.isEmpty());
        }
    }

    // ========== filterContent (private, tested indirectly) ==========

    @Nested
    @DisplayName("filterContent 边界情况（通过 submitReview 间接测试）")
    class FilterContentEdgeCases {

        @Test
        @DisplayName("null 内容不触发过滤")
        void nullContentSkipped() {
            ContentTarget target = ContentTarget.text("ct6", null);
            // content text is null — filterContent returns null internally
            when(contentFilter.filter(null)).thenThrow(new RuntimeException("should not be called"));

            ReviewCase result = domainService.submitReview("user1", target, ReviewType.AUTOMATIC, ReviewPriority.LOW);

            assertEquals(ReviewStatus.PENDING, result.getStatus());
        }
    }
}
