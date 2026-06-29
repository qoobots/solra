package com.solra.crt.domain.service;

import com.solra.crt.domain.entity.SpaceProject;
import com.solra.crt.domain.repository.ProjectRepository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 空间发布与审核工作流领域服务 (CRT-008)。
 *
 * 管理空间从编辑完成到最终上线的完整发布审核流程。
 * 状态机：DRAFT → SUBMITTED → REVIEWING → APPROVED → PUBLISHED
 *                        ↘ REJECTED → DRAFT (修改后重新提交)
 *
 * 验收标准：
 * - 发布→审核→上线闭环 <1小时
 * - 支持审核意见反馈
 * - 支持发布版本管理
 */
public class PublishingPipeline {

    private final ProjectRepository projectRepository;

    // 审核记录存储（生产环境应持久化）
    private final Map<String, List<ReviewRecord>> reviewHistory = new ConcurrentHashMap<>();

    public PublishingPipeline(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * 提交项目进行审核。
     */
    public ReviewSubmission submitForReview(String projectId, String submitterId, String notes) {
        SpaceProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (project.getStatus() != SpaceProject.ProjectStatus.DRAFT &&
            project.getStatus() != SpaceProject.ProjectStatus.BUILDING) {
            throw new IllegalStateException("Project must be in DRAFT or BUILDING status to submit for review");
        }

        ReviewRecord record = new ReviewRecord();
        record.setReviewId(UUID.randomUUID().toString());
        record.setProjectId(projectId);
        record.setSubmitterId(submitterId);
        record.setStatus(ReviewStatus.SUBMITTED);
        record.setSubmittedAt(Instant.now());
        record.setNotes(notes);
        record.setProjectVersion(project.getConfig() != null ? project.getConfig().hashCode() : 0);

        reviewHistory.computeIfAbsent(projectId, k -> new ArrayList<>()).add(record);

        ReviewSubmission submission = new ReviewSubmission();
        submission.setReviewId(record.getReviewId());
        submission.setProjectId(projectId);
        submission.setStatus("SUBMITTED");
        submission.setSubmittedAt(record.getSubmittedAt());
        submission.setMessage("Project submitted for review successfully.");
        return submission;
    }

    /**
     * 审核员认领审核任务。
     */
    public ReviewSubmission claimReview(String reviewId, String reviewerId) {
        ReviewRecord record = findReviewRecord(reviewId);
        if (record.getStatus() != ReviewStatus.SUBMITTED) {
            throw new IllegalStateException("Review is not in SUBMITTED status: " + record.getStatus());
        }
        record.setReviewerId(reviewerId);
        record.setStatus(ReviewStatus.REVIEWING);
        record.setReviewedAt(Instant.now());

        ReviewSubmission submission = new ReviewSubmission();
        submission.setReviewId(reviewId);
        submission.setProjectId(record.getProjectId());
        submission.setStatus("REVIEWING");
        submission.setReviewerId(reviewerId);
        submission.setMessage("Review claimed by " + reviewerId);
        return submission;
    }

    /**
     * 审核通过。
     */
    public ReviewSubmission approveReview(String reviewId, String reviewerId, String comment) {
        ReviewRecord record = findReviewRecord(reviewId);
        validateReviewer(record, reviewerId);
        if (record.getStatus() != ReviewStatus.REVIEWING) {
            throw new IllegalStateException("Review is not in REVIEWING status: " + record.getStatus());
        }

        record.setStatus(ReviewStatus.APPROVED);
        record.setComment(comment);
        record.setReviewedAt(Instant.now());

        // 更新项目状态为已发布
        SpaceProject project = projectRepository.findById(record.getProjectId())
                .orElseThrow(() -> new IllegalStateException("Project not found"));
        project.publish();
        projectRepository.save(project);

        ReviewSubmission submission = new ReviewSubmission();
        submission.setReviewId(reviewId);
        submission.setProjectId(record.getProjectId());
        submission.setStatus("APPROVED");
        submission.setReviewerId(reviewerId);
        submission.setMessage("Project approved and published.");
        submission.setComment(comment);
        return submission;
    }

    /**
     * 审核驳回。
     */
    public ReviewSubmission rejectReview(String reviewId, String reviewerId, String reason) {
        ReviewRecord record = findReviewRecord(reviewId);
        validateReviewer(record, reviewerId);
        if (record.getStatus() != ReviewStatus.REVIEWING) {
            throw new IllegalStateException("Review is not in REVIEWING status: " + record.getStatus());
        }

        record.setStatus(ReviewStatus.REJECTED);
        record.setComment(reason);
        record.setReviewedAt(Instant.now());

        ReviewSubmission submission = new ReviewSubmission();
        submission.setReviewId(reviewId);
        submission.setProjectId(record.getProjectId());
        submission.setStatus("REJECTED");
        submission.setReviewerId(reviewerId);
        submission.setMessage("Project rejected. Reason: " + reason);
        submission.setComment(reason);
        return submission;
    }

    /**
     * 获取项目审核历史。
     */
    public List<ReviewRecord> getReviewHistory(String projectId) {
        return reviewHistory.getOrDefault(projectId, Collections.emptyList());
    }

    /**
     * 获取当前待审核列表。
     */
    public List<ReviewRecord> getPendingReviews() {
        return reviewHistory.values().stream()
                .flatMap(List::stream)
                .filter(r -> r.getStatus() == ReviewStatus.SUBMITTED)
                .sorted(Comparator.comparing(ReviewRecord::getSubmittedAt))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取指定审核员的审核中列表。
     */
    public List<ReviewRecord> getReviewerQueue(String reviewerId) {
        return reviewHistory.values().stream()
                .flatMap(List::stream)
                .filter(r -> r.getStatus() == ReviewStatus.REVIEWING &&
                        reviewerId.equals(r.getReviewerId()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 计算审核统计数据。
     */
    public Map<String, Object> getReviewStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        List<ReviewRecord> allRecords = reviewHistory.values().stream()
                .flatMap(List::stream).toList();

        Map<String, Long> byStatus = allRecords.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.getStatus().name(),
                        java.util.stream.Collectors.counting()));
        stats.put("byStatus", byStatus);

        // 平均审核时间（毫秒）
        double avgTime = allRecords.stream()
                .filter(r -> r.getReviewedAt() != null && r.getSubmittedAt() != null)
                .mapToLong(r -> r.getReviewedAt().toEpochMilli() - r.getSubmittedAt().toEpochMilli())
                .average()
                .orElse(0);
        stats.put("avgReviewTimeMs", avgTime);

        // 审核通过率
        long approved = allRecords.stream().filter(r -> r.getStatus() == ReviewStatus.APPROVED).count();
        long total = allRecords.stream().filter(r -> r.getStatus() == ReviewStatus.APPROVED ||
                r.getStatus() == ReviewStatus.REJECTED).count();
        stats.put("approvalRate", total > 0 ? (float) approved / total : 0);

        stats.put("pendingCount", allRecords.stream().filter(r -> r.getStatus() == ReviewStatus.SUBMITTED).count());
        stats.put("reviewingCount", allRecords.stream().filter(r -> r.getStatus() == ReviewStatus.REVIEWING).count());

        return stats;
    }

    // ── 私有辅助 ──

    private ReviewRecord findReviewRecord(String reviewId) {
        return reviewHistory.values().stream()
                .flatMap(List::stream)
                .filter(r -> r.getReviewId().equals(reviewId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
    }

    private void validateReviewer(ReviewRecord record, String reviewerId) {
        if (record.getReviewerId() != null && !record.getReviewerId().equals(reviewerId)) {
            throw new IllegalStateException("Review is assigned to a different reviewer");
        }
    }

    // ── 内嵌类型 ──

    public enum ReviewStatus {
        SUBMITTED, REVIEWING, APPROVED, REJECTED
    }

    public static class ReviewRecord {
        private String reviewId;
        private String projectId;
        private String submitterId;
        private String reviewerId;
        private ReviewStatus status;
        private Instant submittedAt;
        private Instant reviewedAt;
        private String notes;
        private String comment;
        private int projectVersion;

        public String getReviewId() { return reviewId; }
        public void setReviewId(String reviewId) { this.reviewId = reviewId; }
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        public String getSubmitterId() { return submitterId; }
        public void setSubmitterId(String submitterId) { this.submitterId = submitterId; }
        public String getReviewerId() { return reviewerId; }
        public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
        public ReviewStatus getStatus() { return status; }
        public void setStatus(ReviewStatus status) { this.status = status; }
        public Instant getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
        public Instant getReviewedAt() { return reviewedAt; }
        public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public int getProjectVersion() { return projectVersion; }
        public void setProjectVersion(int projectVersion) { this.projectVersion = projectVersion; }
    }

    public static class ReviewSubmission {
        private String reviewId;
        private String projectId;
        private String status;
        private String reviewerId;
        private Instant submittedAt;
        private String message;
        private String comment;

        public String getReviewId() { return reviewId; }
        public void setReviewId(String reviewId) { this.reviewId = reviewId; }
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReviewerId() { return reviewerId; }
        public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
        public Instant getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}
