package com.solra.saf.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Appeal 领域模型单元测试 — SAF-005")
class AppealTest {

    @Nested
    @DisplayName("create — 创建申诉")
    class CreateTests {

        @Test
        @DisplayName("创建申诉 → 初始状态为 SUBMITTED")
        void createAppealWithSubmittedStatus() {
            Appeal appeal = Appeal.create("case1", "user1",
                    "审核有误", List.of("http://evidence/1.jpg"));

            assertNotNull(appeal.getAppealId());
            assertEquals("case1", appeal.getCaseId());
            assertEquals("user1", appeal.getUserId());
            assertEquals(AppealStatus.SUBMITTED, appeal.getStatus());
            assertEquals("审核有误", appeal.getReason());
            assertEquals(1, appeal.getEvidenceUrls().size());
            assertNotNull(appeal.getSubmittedAt());
            assertFalse(appeal.isResolved());
        }

        @Test
        @DisplayName("无证据URL创建申诉 → 证据列表为空")
        void createAppealWithoutEvidence() {
            Appeal appeal = Appeal.create("case2", "user2", "理由", null);

            assertNotNull(appeal.getEvidenceUrls());
            assertTrue(appeal.getEvidenceUrls().isEmpty());
        }
    }

    @Nested
    @DisplayName("startReview — 开始审核申诉")
    class StartReviewTests {

        @Test
        @DisplayName("开始审核 → 状态变为 IN_REVIEW")
        void startReview() {
            Appeal appeal = Appeal.create("case1", "user1", "理由", List.of());
            appeal.startReview("reviewer1");

            assertEquals(AppealStatus.IN_REVIEW, appeal.getStatus());
            assertEquals("reviewer1", appeal.getReviewerId());
            assertNotNull(appeal.getReviewedAt());
        }
    }

    @Nested
    @DisplayName("裁决申诉")
    class DecisionTests {

        @Test
        @DisplayName("申诉成功 → UPHELD")
        void uphold() {
            Appeal appeal = Appeal.create("case1", "user1", "理由", List.of());
            appeal.uphold("审核有误，撤销决定", "reviewer1");

            assertTrue(appeal.isResolved());
            assertEquals(AppealStatus.RESOLVED, appeal.getStatus());
            assertEquals(AppealDecision.UPHELD, appeal.getDecision());
            assertNotNull(appeal.getResolvedAt());
        }

        @Test
        @DisplayName("申诉驳回 → DENIED")
        void deny() {
            Appeal appeal = Appeal.create("case2", "user2", "理由", List.of());
            appeal.deny("维持原决定", "reviewer1");

            assertTrue(appeal.isResolved());
            assertEquals(AppealStatus.RESOLVED, appeal.getStatus());
            assertEquals(AppealDecision.DENIED, appeal.getDecision());
        }

        @Test
        @DisplayName("部分撤销 → PARTIAL")
        void partialUphold() {
            Appeal appeal = Appeal.create("case3", "user3", "理由", List.of());
            appeal.partialUphold("部分违规成立", "reviewer1");

            assertTrue(appeal.isResolved());
            assertEquals(AppealStatus.RESOLVED, appeal.getStatus());
            assertEquals(AppealDecision.PARTIAL, appeal.getDecision());
        }
    }
}
