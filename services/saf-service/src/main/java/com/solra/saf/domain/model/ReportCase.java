package com.solra.saf.domain.model;

import java.time.Instant;
import java.util.*;

/**
 * ReportCase aggregate root — user-submitted report against content or user.
 * Covers: SAF-003 (用户举报→审核→处理闭环).
 *
 * A report goes through: SUBMITTED → PENDING_REVIEW → IN_REVIEW → RESOLVED.
 */
public class ReportCase {

    private String reportId;
    private String reporterUserId;      // 举报人
    private String reportedUserId;      // 被举报人
    private String reportedContentId;   // 被举报内容
    private String reportReason;        // 举报原因（用户选择/填写）
    private List<String> evidenceUrls = new ArrayList<>();
    private ReportStatus status;
    private ReportCategory category;
    private ReviewDecision decision;
    private String decisionReason;      // 处理理由
    private String reviewerId;          // 审核员
    private Instant submittedAt;
    private Instant reviewedAt;
    private Instant resolvedAt;
    private Map<String, String> metadata = new HashMap<>();

    private ReportCase() {}

    public static ReportCase create(String reporterUserId, String reportedUserId,
                                     String reportedContentId, String reportReason,
                                     ReportCategory category, List<String> evidenceUrls) {
        ReportCase r = new ReportCase();
        r.reportId = UUID.randomUUID().toString();
        r.reporterUserId = reporterUserId;
        r.reportedUserId = reportedUserId;
        r.reportedContentId = reportedContentId;
        r.reportReason = reportReason;
        r.category = category;
        r.evidenceUrls = evidenceUrls != null ? new ArrayList<>(evidenceUrls) : new ArrayList<>();
        r.status = ReportStatus.SUBMITTED;
        r.submittedAt = Instant.now();
        return r;
    }

    /**
     * Assign to reviewer and mark as in-review.
     */
    public void startReview(String reviewerId) {
        this.status = ReportStatus.IN_REVIEW;
        this.reviewerId = reviewerId;
        this.reviewedAt = Instant.now();
    }

    /**
     * Resolve report with a decision.
     */
    public void resolve(ReviewDecision decision, String reason, String reviewerId) {
        this.status = ReportStatus.RESOLVED;
        this.decision = decision;
        this.decisionReason = reason;
        this.reviewerId = reviewerId;
        this.resolvedAt = Instant.now();
    }

    /**
     * Reject report as invalid (no violation found).
     */
    public void dismiss(String reason, String reviewerId) {
        resolve(ReviewDecision.APPROVED, reason, reviewerId);
    }

    /**
     * Confirm violation and take action.
     */
    public void confirm(String reason, String reviewerId) {
        resolve(ReviewDecision.REJECTED, reason, reviewerId);
    }

    public boolean isResolved() {
        return status == ReportStatus.RESOLVED;
    }

    public boolean requiresEscalation() {
        return category == ReportCategory.ILLEGAL_CONTENT
                || category == ReportCategory.MINOR_SAFETY;
    }

    // -- Getters --
    public String getReportId() { return reportId; }
    public String getReporterUserId() { return reporterUserId; }
    public String getReportedUserId() { return reportedUserId; }
    public String getReportedContentId() { return reportedContentId; }
    public String getReportReason() { return reportReason; }
    public List<String> getEvidenceUrls() { return Collections.unmodifiableList(evidenceUrls); }
    public ReportStatus getStatus() { return status; }
    public ReportCategory getCategory() { return category; }
    public ReviewDecision getDecision() { return decision; }
    public String getDecisionReason() { return decisionReason; }
    public String getReviewerId() { return reviewerId; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Map<String, String> getMetadata() { return Collections.unmodifiableMap(metadata); }
}
