package com.solra.saf.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReviewCase 聚合根 单元测试")
class ReviewCaseTest {

    @Nested
    @DisplayName("create — 工厂方法")
    class CreateTests {

        @Test
        @DisplayName("创建案例初始状态为 PENDING")
        void createsWithPendingStatus() {
            ContentTarget target = ContentTarget.text("c1", "test content");
            ReviewCase c = ReviewCase.create("user1", target, ReviewType.AUTOMATIC, ReviewPriority.NORMAL);

            assertEquals(ReviewStatus.PENDING, c.getStatus());
            assertEquals("user1", c.getUserId());
            assertEquals(ReviewType.AUTOMATIC, c.getReviewType());
            assertEquals(ReviewPriority.NORMAL, c.getPriority());
            assertNotNull(c.getCaseId());
            assertNotNull(c.getSubmittedAt());
            assertNull(c.getDecision());
        }

        @Test
        @DisplayName("每次创建生成唯一 caseId")
        void generatesUniqueId() {
            ContentTarget target = ContentTarget.text("c2", "text");
            ReviewCase c1 = ReviewCase.create("u1", target, ReviewType.AUTOMATIC, ReviewPriority.LOW);
            ReviewCase c2 = ReviewCase.create("u2", target, ReviewType.AUTOMATIC, ReviewPriority.LOW);

            assertNotEquals(c1.getCaseId(), c2.getCaseId());
        }
    }

    @Nested
    @DisplayName("autoReview — 自动审核")
    class AutoReviewTests {

        @Test
        @DisplayName("高置信度 (≥0.9) → 状态变为 COMPLETED")
        void highConfidenceCompletes() {
            ReviewCase c = ReviewCase.create("u1", ContentTarget.text("c1", "text"),
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);
            c.autoReview(ReviewDecision.APPROVED, List.of(), 0.95f, "v1");

            assertEquals(ReviewStatus.COMPLETED, c.getStatus());
            assertEquals(ReviewDecision.APPROVED, c.getDecision());
            assertNotNull(c.getReviewedAt());
        }

        @Test
        @DisplayName("低置信度 (<0.9) → 状态变为 ESCALATED")
        void lowConfidenceEscalates() {
            ReviewCase c = ReviewCase.create("u1", ContentTarget.text("c1", "text"),
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);
            c.autoReview(ReviewDecision.FLAGGED, List.of(), 0.5f, "v1");

            assertEquals(ReviewStatus.ESCALATED, c.getStatus());
            assertEquals(ReviewDecision.FLAGGED, c.getDecision());
        }

        @Test
        @DisplayName("置信度 = 0.9 边界 → COMPLETED")
        void confidenceBoundaryComplete() {
            ReviewCase c = ReviewCase.create("u1", ContentTarget.text("c1", "text"),
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);
            c.autoReview(ReviewDecision.APPROVED, List.of(), 0.9f, "v1");

            assertEquals(ReviewStatus.COMPLETED, c.getStatus());
        }

        @Test
        @DisplayName("置信度 = 0.89 边界 → ESCALATED")
        void confidenceBoundaryEscalate() {
            ReviewCase c = ReviewCase.create("u1", ContentTarget.text("c1", "text"),
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);
            c.autoReview(ReviewDecision.REJECTED, List.of(), 0.89f, "v1");

            assertEquals(ReviewStatus.ESCALATED, c.getStatus());
        }

        @Test
        @DisplayName("null 违规列表转为空列表")
        void nullViolationsBecomesEmpty() {
            ReviewCase c = ReviewCase.create("u1", ContentTarget.text("c1", "text"),
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);
            c.autoReview(ReviewDecision.APPROVED, null, 1.0f, "v1");

            assertTrue(c.getViolations().isEmpty());
        }

        @Test
        @DisplayName("审核人 ID 格式为 auto:modelVersion")
        void reviewerIdFormat() {
            ReviewCase c = ReviewCase.create("u1", ContentTarget.text("c1", "text"),
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);
            c.autoReview(ReviewDecision.APPROVED, List.of(), 1.0f, "v2");

            assertEquals("auto:v2", c.getReviewerId());
        }
    }

    @Nested
    @DisplayName("manualReview — 人工审核")
    class ManualReviewTests {

        @Test
        @DisplayName("人工审核后状态为 COMPLETED")
        void manualReviewCompletes() {
            ReviewCase c = ReviewCase.create("u1", ContentTarget.text("c1", "text"),
                    ReviewType.MANUAL, ReviewPriority.HIGH);
            c.manualReview(ReviewDecision.APPROVED, "looks fine", "admin1");

            assertEquals(ReviewStatus.COMPLETED, c.getStatus());
            assertEquals(ReviewDecision.APPROVED, c.getDecision());
            assertEquals("manual:admin1", c.getReviewerId());
            assertEquals("looks fine", c.getMetadata().get("manual_review_reason"));
        }
    }

    @Nested
    @DisplayName("isPassed / isRejected — 结果判断")
    class ResultJudgmentTests {

        @Test
        @DisplayName("APPROVED 判断为通过")
        void approvedIsPassed() {
            ReviewCase c = ReviewCase.create("u1", ContentTarget.text("c1", "text"),
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);
            c.autoReview(ReviewDecision.APPROVED, List.of(), 1.0f, "v1");

            assertTrue(c.isPassed());
            assertFalse(c.isRejected());
        }

        @Test
        @DisplayName("REJECTED 判断为拒绝")
        void rejectedIsRejected() {
            ReviewCase c = ReviewCase.create("u1", ContentTarget.text("c1", "text"),
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);
            c.autoReview(ReviewDecision.REJECTED, List.of(), 1.0f, "v1");

            assertFalse(c.isPassed());
            assertTrue(c.isRejected());
        }

        @Test
        @DisplayName("BLOCKED 判断为拒绝")
        void blockedIsRejected() {
            ReviewCase c = ReviewCase.create("u1", ContentTarget.text("c1", "text"),
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);
            c.autoReview(ReviewDecision.BLOCKED, List.of(), 1.0f, "v1");

            assertTrue(c.isRejected());
        }

        @Test
        @DisplayName("FLAGGED 既不通过也不拒绝")
        void flaggedIsNeither() {
            ReviewCase c = ReviewCase.create("u1", ContentTarget.text("c1", "text"),
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);
            c.autoReview(ReviewDecision.FLAGGED, List.of(), 1.0f, "v1");

            assertFalse(c.isPassed());
            assertFalse(c.isRejected());
        }
    }
}
