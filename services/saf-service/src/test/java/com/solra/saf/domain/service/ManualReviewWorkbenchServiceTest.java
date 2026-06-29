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
@DisplayName("ManualReviewWorkbenchService 单元测试 — SAF-004 人工审核工作台")
class ManualReviewWorkbenchServiceTest {

    @Mock
    private ReviewCaseRepository reviewCaseRepository;

    private ManualReviewWorkbenchService workbenchService;

    @BeforeEach
    void setUp() {
        workbenchService = new ManualReviewWorkbenchService(reviewCaseRepository);
        lenient().when(reviewCaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(reviewCaseRepository.findByStatusAndReviewType(any(), any(), anyInt()))
                .thenReturn(List.of());
        lenient().when(reviewCaseRepository.countByStatus(any())).thenReturn(0L);
    }

    @Nested
    @DisplayName("getWorkQueue — 获取工作队列")
    class GetWorkQueueTests {

        @Test
        @DisplayName("队列为空时返回空列表")
        void emptyQueue() {
            List<ReviewWorkbenchItem> queue = workbenchService.getWorkQueue("reviewer1", 20);

            assertNotNull(queue);
            assertTrue(queue.isEmpty());
        }

        @Test
        @DisplayName("有待审案例时返回排序后的队列")
        void queueWithPendingCases() {
            ContentTarget t1 = ContentTarget.text("c1", "urgent content");
            ReviewCase rc1 = ReviewCase.create("user1", t1, ReviewType.MANUAL, ReviewPriority.URGENT);

            ContentTarget t2 = ContentTarget.text("c2", "normal content");
            ReviewCase rc2 = ReviewCase.create("user2", t2, ReviewType.MANUAL, ReviewPriority.NORMAL);

            when(reviewCaseRepository.findByStatusAndReviewType(
                    ReviewStatus.PENDING, ReviewType.MANUAL, 20))
                    .thenReturn(List.of(rc1, rc2));

            List<ReviewWorkbenchItem> queue = workbenchService.getWorkQueue("reviewer1", 20);

            assertNotNull(queue);
            assertEquals(2, queue.size());
            // URGENT should come first
            assertEquals(ReviewPriority.URGENT, queue.get(0).getPriority());
        }

        @Test
        @DisplayName("升级案例优先级最高")
        void escalatedCasesFirst() {
            ContentTarget t1 = ContentTarget.text("c1", "normal");
            ReviewCase rc1 = ReviewCase.create("user1", t1, ReviewType.MANUAL, ReviewPriority.NORMAL);

            ContentTarget t2 = ContentTarget.text("c2", "escalated");
            ReviewCase rc2 = ReviewCase.create("user2", t2, ReviewType.HYBRID, ReviewPriority.HIGH);
            // Simulate escalated case
            setField(rc2, "status", ReviewStatus.ESCALATED);

            when(reviewCaseRepository.findByStatusAndReviewType(
                    ReviewStatus.PENDING, ReviewType.MANUAL, 20))
                    .thenReturn(List.of(rc1));
            when(reviewCaseRepository.findByStatusAndReviewType(
                    ReviewStatus.ESCALATED, ReviewType.HYBRID, 20))
                    .thenReturn(List.of(rc2));

            List<ReviewWorkbenchItem> queue = workbenchService.getWorkQueue("reviewer1", 20);

            assertFalse(queue.isEmpty());
            // Escalated cases should appear first
            assertEquals("c2", queue.get(0).getReviewCaseId());
        }

        private void setField(Object obj, String fieldName, Object value) {
            try {
                java.lang.reflect.Field field = ReviewCase.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    @DisplayName("claimItem — 认领审核项")
    class ClaimItemTests {

        @Test
        @DisplayName("认领待审案例 → 成功")
        void claimPendingCase() {
            ContentTarget target = ContentTarget.text("c1", "content");
            ReviewCase rc = ReviewCase.create("user1", target, ReviewType.MANUAL, ReviewPriority.NORMAL);
            when(reviewCaseRepository.findById(rc.getCaseId()))
                    .thenReturn(Optional.of(rc));

            ReviewWorkbenchItem item = workbenchService.claimItem(rc.getCaseId(), "reviewer1");

            assertNotNull(item);
            assertEquals("reviewer1", item.getAssignedReviewerId());
            assertEquals(ReviewStatus.IN_PROGRESS, item.getStatus());
        }

        @Test
        @DisplayName("认领已完成的案例 → 抛出异常")
        void claimCompletedCaseThrowsException() {
            ContentTarget target = ContentTarget.text("c2", "content");
            ReviewCase rc = ReviewCase.create("user1", target, ReviewType.MANUAL, ReviewPriority.NORMAL);
            rc.manualReview(ReviewDecision.APPROVED, "done", "reviewer1");
            when(reviewCaseRepository.findById(rc.getCaseId()))
                    .thenReturn(Optional.of(rc));

            assertThrows(SolraException.BadRequestException.class,
                    () -> workbenchService.claimItem(rc.getCaseId(), "reviewer2"));
        }
    }

    @Nested
    @DisplayName("submitManualReview — 提交审核决定")
    class SubmitManualReviewTests {

        @Test
        @DisplayName("提交人工审核通过 → 案例状态变为 COMPLETED")
        void submitApprove() {
            ContentTarget target = ContentTarget.text("c1", "content");
            ReviewCase rc = ReviewCase.create("user1", target, ReviewType.MANUAL, ReviewPriority.NORMAL);
            when(reviewCaseRepository.findById(rc.getCaseId()))
                    .thenReturn(Optional.of(rc));

            ReviewCase result = workbenchService.submitManualReview(
                    rc.getCaseId(), ReviewDecision.APPROVED,
                    "内容合规", "reviewer1");

            assertEquals(ReviewStatus.COMPLETED, result.getStatus());
            assertEquals(ReviewDecision.APPROVED, result.getDecision());
            assertEquals("manual:reviewer1", result.getReviewerId());
        }

        @Test
        @DisplayName("提交人工审核拒绝 → 案例状态变为 COMPLETED")
        void submitReject() {
            ContentTarget target = ContentTarget.text("c2", "bad content");
            ReviewCase rc = ReviewCase.create("user1", target, ReviewType.MANUAL, ReviewPriority.NORMAL);
            when(reviewCaseRepository.findById(rc.getCaseId()))
                    .thenReturn(Optional.of(rc));

            ReviewCase result = workbenchService.submitManualReview(
                    rc.getCaseId(), ReviewDecision.REJECTED,
                    "违规内容", "reviewer1");

            assertEquals(ReviewStatus.COMPLETED, result.getStatus());
            assertEquals(ReviewDecision.REJECTED, result.getDecision());
        }
    }

    @Nested
    @DisplayName("getReviewerStats — 审核员统计")
    class ReviewerStatsTests {

        @Test
        @DisplayName("返回审核员统计数据")
        void returnStats() {
            when(reviewCaseRepository.countByStatus(ReviewStatus.PENDING)).thenReturn(42L);
            when(reviewCaseRepository.countByStatus(ReviewStatus.ESCALATED)).thenReturn(5L);

            var stats = workbenchService.getReviewerStats("reviewer1");

            assertEquals("reviewer1", stats.reviewerId());
            assertEquals(42L, stats.pendingCount());
            assertEquals(5L, stats.escalatedCount());
        }
    }
}
