package com.solra.saf.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for Appeal persistence.
 * Covers: SAF-005 (用户申诉通道).
 */
@Entity
@Table(name = "appeals", indexes = {
    @Index(name = "idx_appeal_user", columnList = "userId"),
    @Index(name = "idx_appeal_case", columnList = "caseId"),
    @Index(name = "idx_appeal_status", columnList = "status"),
    @Index(name = "idx_appeal_submitted", columnList = "submittedAt")
})
public class AppealEntity {

    @Id
    @Column(name = "appeal_id", length = 64)
    private String appealId;

    @Column(name = "case_id", length = 64)
    private String caseId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "reason", length = 2048)
    private String reason;

    @Column(name = "evidence_urls_json", columnDefinition = "TEXT")
    private String evidenceUrlsJson;

    @Column(name = "status", length = 16)
    private String status;

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
    public String getAppealId() { return appealId; }
    public void setAppealId(String appealId) { this.appealId = appealId; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getEvidenceUrlsJson() { return evidenceUrlsJson; }
    public void setEvidenceUrlsJson(String evidenceUrlsJson) { this.evidenceUrlsJson = evidenceUrlsJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
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
