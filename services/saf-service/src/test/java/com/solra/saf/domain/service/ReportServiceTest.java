package com.solra.saf.domain.service;

import com.solra.saf.domain.model.*;
import com.solra.saf.domain.repository.ReportCaseRepository;
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
@DisplayName("ReportService 单元测试 — SAF-003 用户举报闭环")
class ReportServiceTest {

    @Mock
    private ReportCaseRepository reportCaseRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportCaseRepository);
        lenient().when(reportCaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(reportCaseRepository.countByReporterUserId(any())).thenReturn(0L);
    }

    @Nested
    @DisplayName("submitReport — 提交举报")
    class SubmitReportTests {

        @Test
        @DisplayName("正常举报提交成功 → 返回 SUBMITTED 状态")
        void submitValidReport() {
            ReportCase result = reportService.submitReport(
                    "reporter1", "reported1", "content123",
                    "包含色情内容", ReportCategory.NSFW,
                    List.of("http://evidence/1.jpg"));

            assertNotNull(result);
            assertEquals("reporter1", result.getReporterUserId());
            assertEquals("reported1", result.getReportedUserId());
            assertEquals(ReportStatus.SUBMITTED, result.getStatus());
            assertEquals(ReportCategory.NSFW, result.getCategory());
            assertEquals(1, result.getEvidenceUrls().size());
            verify(reportCaseRepository).save(any());
        }

        @Test
        @DisplayName("紧急类别举报需要升级标记")
        void illegalCategoryRequiresEscalation() {
            ReportCase result = reportService.submitReport(
                    "reporter1", "reported1", "content456",
                    "违法内容", ReportCategory.ILLEGAL_CONTENT, List.of());

            assertTrue(result.requiresEscalation());
            assertEquals(ReportStatus.SUBMITTED, result.getStatus());
        }

        @Test
        @DisplayName("每日举报超过20次限制 → 抛出 TooManyRequestsException")
        void rateLimitExceeded() {
            when(reportCaseRepository.countByReporterUserId("reporter1")).thenReturn(20L);

            assertThrows(SolraException.TooManyRequestsException.class,
                    () -> reportService.submitReport(
                            "reporter1", "reported1", "content789",
                            "spam", ReportCategory.SPAM, List.of()));
        }
    }

    @Nested
    @DisplayName("startReview — 开始审核举报")
    class StartReviewTests {

        @Test
        @DisplayName("举报被审核员认领 → 状态变为 IN_REVIEW")
        void startReviewTransitionsToInReview() {
            ReportCase report = ReportCase.create("reporter1", "reported1", "c1",
                    "reason", ReportCategory.SPAM, List.of());
            when(reportCaseRepository.findById(report.getReportId()))
                    .thenReturn(Optional.of(report));

            ReportCase result = reportService.startReview(report.getReportId(), "reviewer1");

            assertEquals(ReportStatus.IN_REVIEW, result.getStatus());
            assertEquals("reviewer1", result.getReviewerId());
            verify(reportCaseRepository).save(any());
        }

        @Test
        @DisplayName("已处理的举报再次审核 → 抛出异常")
        void alreadyResolvedThrowsException() {
            ReportCase report = ReportCase.create("reporter1", "reported1", "c2",
                    "reason", ReportCategory.SPAM, List.of());
            report.resolve(ReviewDecision.APPROVED, "已处理", "reviewer1");
            when(reportCaseRepository.findById(report.getReportId()))
                    .thenReturn(Optional.of(report));

            assertThrows(SolraException.BadRequestException.class,
                    () -> reportService.startReview(report.getReportId(), "reviewer2"));
        }

        @Test
        @DisplayName("不存在的举报 → 抛出 NotFoundException")
        void notFoundThrowsException() {
            when(reportCaseRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThrows(SolraException.NotFoundException.class,
                    () -> reportService.startReview("nonexistent", "reviewer1"));
        }
    }

    @Nested
    @DisplayName("resolveReport — 处理举报")
    class ResolveReportTests {

        @Test
        @DisplayName("确认违规 → 状态变为 RESOLVED + REJECTED")
        void confirmViolation() {
            ReportCase report = ReportCase.create("reporter1", "reported1", "c1",
                    "hate speech", ReportCategory.HATE_SPEECH, List.of());
            when(reportCaseRepository.findById(report.getReportId()))
                    .thenReturn(Optional.of(report));

            ReportCase result = reportService.resolveReport(
                    report.getReportId(), ReviewDecision.REJECTED,
                    "确认违规，已屏蔽内容", "reviewer1");

            assertEquals(ReportStatus.RESOLVED, result.getStatus());
            assertEquals(ReviewDecision.REJECTED, result.getDecision());
            assertTrue(result.isResolved());
        }

        @Test
        @DisplayName("驳回举报 → 状态变为 RESOLVED + APPROVED")
        void dismissReport() {
            ReportCase report = ReportCase.create("reporter1", "reported1", "c2",
                    "误报", ReportCategory.OTHER, List.of());
            when(reportCaseRepository.findById(report.getReportId()))
                    .thenReturn(Optional.of(report));

            ReportCase result = reportService.resolveReport(
                    report.getReportId(), ReviewDecision.APPROVED,
                    "未发现违规", "reviewer1");

            assertEquals(ReportStatus.RESOLVED, result.getStatus());
            assertEquals(ReviewDecision.APPROVED, result.getDecision());
        }
    }

    @Nested
    @DisplayName("queryReport — 查询举报")
    class QueryReportTests {

        @Test
        @DisplayName("存在的举报 → 返回举报详情")
        void existingReportFound() {
            ReportCase report = ReportCase.create("reporter1", "reported1", "c1",
                    "reason", ReportCategory.NSFW, List.of());
            when(reportCaseRepository.findById(report.getReportId()))
                    .thenReturn(Optional.of(report));

            ReportCase result = reportService.queryReport(report.getReportId());

            assertNotNull(result);
            assertEquals(report.getReportId(), result.getReportId());
        }

        @Test
        @DisplayName("不存在的举报 → 抛出 NotFoundException")
        void notFoundThrowsException() {
            when(reportCaseRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThrows(SolraException.NotFoundException.class,
                    () -> reportService.queryReport("nonexistent"));
        }
    }
}
