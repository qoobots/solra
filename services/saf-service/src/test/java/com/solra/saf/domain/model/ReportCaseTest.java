package com.solra.saf.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReportCase 领域模型单元测试 — SAF-003")
class ReportCaseTest {

    @Nested
    @DisplayName("create — 创建举报案例")
    class CreateTests {

        @Test
        @DisplayName("创建举报 → 初始状态为 SUBMITTED")
        void createReportWithSubmittedStatus() {
            ReportCase report = ReportCase.create("reporter1", "reported1",
                    "content123", "违规内容", ReportCategory.NSFW,
                    List.of("http://evidence/1.jpg"));

            assertNotNull(report.getReportId());
            assertEquals("reporter1", report.getReporterUserId());
            assertEquals("reported1", report.getReportedUserId());
            assertEquals(ReportStatus.SUBMITTED, report.getStatus());
            assertEquals(ReportCategory.NSFW, report.getCategory());
            assertNotNull(report.getSubmittedAt());
            assertFalse(report.isResolved());
        }

        @Test
        @DisplayName("无证据URL创建举报 → 证据列表为空")
        void createReportWithoutEvidence() {
            ReportCase report = ReportCase.create("reporter1", "reported1",
                    "content456", "spam", ReportCategory.SPAM, null);

            assertNotNull(report.getEvidenceUrls());
            assertTrue(report.getEvidenceUrls().isEmpty());
        }
    }

    @Nested
    @DisplayName("startReview — 开始审核")
    class StartReviewTests {

        @Test
        @DisplayName("开始审核 → 状态变为 IN_REVIEW，记录审核员")
        void startReview() {
            ReportCase report = ReportCase.create("r1", "d1", "c1",
                    "reason", ReportCategory.HATE_SPEECH, List.of());
            report.startReview("reviewer1");

            assertEquals(ReportStatus.IN_REVIEW, report.getStatus());
            assertEquals("reviewer1", report.getReviewerId());
            assertNotNull(report.getReviewedAt());
        }
    }

    @Nested
    @DisplayName("resolve — 处理举报")
    class ResolveTests {

        @Test
        @DisplayName("确认违规 → RESOLVED + REJECTED")
        void confirmViolation() {
            ReportCase report = ReportCase.create("r1", "d1", "c1",
                    "hate speech", ReportCategory.HATE_SPEECH, List.of());
            report.confirm("确认违规", "reviewer1");

            assertTrue(report.isResolved());
            assertEquals(ReportStatus.RESOLVED, report.getStatus());
            assertEquals(ReviewDecision.REJECTED, report.getDecision());
            assertNotNull(report.getResolvedAt());
        }

        @Test
        @DisplayName("驳回举报 → RESOLVED + APPROVED")
        void dismissReport() {
            ReportCase report = ReportCase.create("r1", "d1", "c1",
                    "误报", ReportCategory.OTHER, List.of());
            report.dismiss("未发现违规", "reviewer1");

            assertTrue(report.isResolved());
            assertEquals(ReportStatus.RESOLVED, report.getStatus());
            assertEquals(ReviewDecision.APPROVED, report.getDecision());
        }
    }

    @Nested
    @DisplayName("requiresEscalation — 是否需要升级")
    class RequiresEscalationTests {

        @Test
        @DisplayName("违法内容需要升级")
        void illegalContentRequiresEscalation() {
            ReportCase report = ReportCase.create("r1", "d1", "c1",
                    "illegal", ReportCategory.ILLEGAL_CONTENT, List.of());
            assertTrue(report.requiresEscalation());
        }

        @Test
        @DisplayName("未成年人安全需要升级")
        void minorSafetyRequiresEscalation() {
            ReportCase report = ReportCase.create("r1", "d1", "c1",
                    "minor safety", ReportCategory.MINOR_SAFETY, List.of());
            assertTrue(report.requiresEscalation());
        }

        @Test
        @DisplayName("普通类别不需要升级")
        void normalCategoryDoesNotRequireEscalation() {
            ReportCase report = ReportCase.create("r1", "d1", "c1",
                    "spam", ReportCategory.SPAM, List.of());
            assertFalse(report.requiresEscalation());
        }
    }
}
