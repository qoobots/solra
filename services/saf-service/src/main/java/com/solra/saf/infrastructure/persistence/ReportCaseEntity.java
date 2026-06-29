package com.solra.saf.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for ReportCase persistence.
 * Covers: SAF-003 (用户举报→审核→处理闭环).
 */
@Entity
@Table(name = "report_cases", indexes = {
    @Index(name = "idx_rpt_reporter", columnList = "reporterUserId"),
    @Index(name = "idx_rpt_reported", columnList = "reportedUserId"),
    @Index(name = "idx_rpt_status", columnList = "status"),
    @Index(name = "idx_rpt_submitted", columnList = "submittedAt")
})
public class ReportCaseEntity {

    @Id
    @Column(name = "report_id", length = 64)
    private String reportId;

    @Column(name = "reporter_user_id", length = 64)
    private String reporterUserId;

    @Column(name = "reported_user_id", length = 64)
    private String reportedUserId;

    @Column(name = "reported_content_id", length = 128)
    private String reportedContentId;

    @Column(name = "report_reason", length = 1024)
    private String reportReason;

    @Column(name = "evidence_urls_json", columnDefinition = "TEXT")
    private String evidenceUrlsJson;

    @Column(name = "status", length = 24)
    private String status;

    @Column(name = "category", length = 32)
    private String category;

    @Column(name = "decision", length = 16)
    private String decision;

    @Column(name = "decision_reason", length = 1024)
    private String decisionReason;

    @Column(name = "reviewer_id", length = 64)
    private String reviewerId;

    @Column
    private Instant submittedAt;

    @Column
    private Instant reviewedAt;

    @Column
    private Instant resolvedAt;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    // -- Getters and Setters --
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getReporterUserId() { return reporterUserId; }
    public void setReporterUserId(String reporterUserId) { this.reporterUserId = reporterUserId; }
    public String getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(String reportedUserId) { this.reportedUserId = reportedUserId; }
    public String getReportedContentId() { return reportedContentId; }
    public void setReportedContentId(String reportedContentId) { this.reportedContentId = reportedContentId; }
    public String getReportReason() { return reportReason; }
    public void setReportReason(String reportReason) { this.reportReason = reportReason; }
    public String getEvidenceUrlsJson() { return evidenceUrlsJson; }
    public void setEvidenceUrlsJson(String evidenceUrlsJson) { this.evidenceUrlsJson = evidenceUrlsJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }
    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
}
