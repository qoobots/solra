package com.solra.saf.domain.service;

import com.solra.saf.domain.model.*;
import com.solra.saf.domain.repository.AppealRepository;
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
@DisplayName("AppealService 单元测试 — SAF-005 用户申诉通道")
class AppealServiceTest {

    @Mock
    private AppealRepository appealRepository;

    @Mock
    private ReviewCaseRepository reviewCaseRepository;

    private AppealService appealService;

    @BeforeEach
    void setUp() {
        appealService = new AppealService(appealRepository, reviewCaseRepository);
        lenient().when(appealRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(appealRepository.existsByCaseIdAndUserId(any(), any())).thenReturn(false);
    }

    @Nested
    @DisplayName("submitAppeal — 提交申诉")
    class SubmitAppealTests {

        @Test
        @DisplayName("对已拒绝案例提交申诉 → 成功")
        void submitAppealForRejectedCase() {
            ContentTarget target = ContentTarget.text("ct1", "test content");
            ReviewCase reviewCase = ReviewCase.create("user1", target,
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);
            reviewCase.autoReview(ReviewDecision.REJECTED, List.of(), 0.95f, "model-v1");

            when(reviewCaseRepository.findById(reviewCase.getCaseId()))
                    .thenReturn(Optional.of(reviewCase));

            Appeal result = appealService.submitAppeal(
                    reviewCase.getCaseId(), "user1",
                    "我认为审核有误", List.of("http://evidence/1.jpg"));

            assertNotNull(result);
            assertEquals(reviewCase.getCaseId(), result.getCaseId());
            assertEquals("user1", result.getUserId());
            assertEquals(AppealStatus.SUBMITTED, result.getStatus());
            assertEquals(1, result.getEvidenceUrls().size());
            verify(appealRepository).save(any());
        }

        @Test
        @DisplayName("对已通过案例申诉 → 抛出异常")
        void submitAppealForApprovedCase() {
            ContentTarget target = ContentTarget.text("ct2", "safe content");
            ReviewCase reviewCase = ReviewCase.create("user1", target,
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);
            reviewCase.autoReview(ReviewDecision.APPROVED, List.of(), 0.95f, "model-v1");

            when(reviewCaseRepository.findById(reviewCase.getCaseId()))
                    .thenReturn(Optional.of(reviewCase));

            assertThrows(SolraException.BadRequestException.class,
                    () -> appealService.submitAppeal(
                            reviewCase.getCaseId(), "user1",
                            "我不服", List.of()));
        }

        @Test
        @DisplayName("重复申诉同一案例 → 抛出异常")
        void duplicateAppealThrowsException() {
            ContentTarget target = ContentTarget.text("ct3", "rejected content");
            ReviewCase reviewCase = ReviewCase.create("user1", target,
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);
            reviewCase.autoReview(ReviewDecision.REJECTED, List.of(), 0.95f, "model-v1");

            when(reviewCaseRepository.findById(reviewCase.getCaseId()))
                    .thenReturn(Optional.of(reviewCase));
            when(appealRepository.existsByCaseIdAndUserId(reviewCase.getCaseId(), "user1"))
                    .thenReturn(true);

            assertThrows(SolraException.BadRequestException.class,
                    () -> appealService.submitAppeal(
                            reviewCase.getCaseId(), "user1",
                            "再次申诉", List.of()));
        }

        @Test
        @DisplayName("空理由申诉 → 抛出异常")
        void emptyReasonThrowsException() {
            ContentTarget target = ContentTarget.text("ct4", "rejected content");
            ReviewCase reviewCase = ReviewCase.create("user1", target,
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);
            reviewCase.autoReview(ReviewDecision.REJECTED, List.of(), 0.95f, "model-v1");

            when(reviewCaseRepository.findById(reviewCase.getCaseId()))
                    .thenReturn(Optional.of(reviewCase));

            assertThrows(SolraException.BadRequestException.class,
                    () -> appealService.submitAppeal(
                            reviewCase.getCaseId(), "user1", "   ", List.of()));
        }

        @Test
        @DisplayName("超过5个证据URL → 抛出异常")
        void tooManyEvidenceUrlsThrowsException() {
            ContentTarget target = ContentTarget.text("ct5", "rejected content");
            ReviewCase reviewCase = ReviewCase.create("user1", target,
                    ReviewType.AUTOMATIC, ReviewPriority.NORMAL);
            reviewCase.autoReview(ReviewDecision.REJECTED, List.of(), 0.95f, "model-v1");

            when(reviewCaseRepository.findById(reviewCase.getCaseId()))
                    .thenReturn(Optional.of(reviewCase));

            assertThrows(SolraException.BadRequestException.class,
                    () -> appealService.submitAppeal(
                            reviewCase.getCaseId(), "user1",
                            "reason", List.of("u1", "u2", "u3", "u4", "u5", "u6")));
        }

        @Test
        @DisplayName("不存在的案例申诉 → 抛出 NotFoundException")
        void nonExistentCaseThrowsException() {
            when(reviewCaseRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThrows(SolraException.NotFoundException.class,
                    () -> appealService.submitAppeal(
                            "nonexistent", "user1", "reason", List.of()));
        }
    }

    @Nested
    @DisplayName("upholdAppeal / denyAppeal — 申诉裁决")
    class ResolveAppealTests {

        @Test
        @DisplayName("申诉成功 → UPHELD")
        void upholdAppeal() {
            Appeal appeal = Appeal.create("case1", "user1", "理由", List.of());
            when(appealRepository.findById(appeal.getAppealId()))
                    .thenReturn(Optional.of(appeal));

            Appeal result = appealService.upholdAppeal(
                    appeal.getAppealId(), "审核错误，撤销决定", "reviewer1");

            assertEquals(AppealStatus.RESOLVED, result.getStatus());
            assertEquals(AppealDecision.UPHELD, result.getDecision());
            assertTrue(result.isResolved());
        }

        @Test
        @DisplayName("申诉驳回 → DENIED")
        void denyAppeal() {
            Appeal appeal = Appeal.create("case2", "user2", "理由", List.of());
            when(appealRepository.findById(appeal.getAppealId()))
                    .thenReturn(Optional.of(appeal));

            Appeal result = appealService.denyAppeal(
                    appeal.getAppealId(), "审核无误，维持原决定", "reviewer1");

            assertEquals(AppealStatus.RESOLVED, result.getStatus());
            assertEquals(AppealDecision.DENIED, result.getDecision());
        }
    }

    @Nested
    @DisplayName("queryAppeal — 查询申诉")
    class QueryAppealTests {

        @Test
        @DisplayName("存在的申诉 → 返回申诉详情")
        void existingAppealFound() {
            Appeal appeal = Appeal.create("case1", "user1", "理由", List.of());
            when(appealRepository.findById(appeal.getAppealId()))
                    .thenReturn(Optional.of(appeal));

            Appeal result = appealService.queryAppeal(appeal.getAppealId());

            assertNotNull(result);
            assertEquals(appeal.getAppealId(), result.getAppealId());
        }

        @Test
        @DisplayName("不存在的申诉 → 抛出 NotFoundException")
        void notFoundThrowsException() {
            when(appealRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThrows(SolraException.NotFoundException.class,
                    () -> appealService.queryAppeal("nonexistent"));
        }
    }
}
