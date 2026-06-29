package com.solra.saf.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for ReviewCase persistence (SAF-001).
 */
@Entity
@Table(name = "review_cases", indexes = {
    @Index(name = "idx_rc_user", columnList = "userId"),
    @Index(name = "idx_rc_status", columnList = "status"),
    @Index(name = "idx_rc_created", columnList = "createdAt")
})
public class ReviewCaseEntity {

    @Id
    @Column(name = "case_id", length = 64)
    private String caseId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "content_type", length = 32)
    private String contentType;

    @Column(name = "content_text", length = 4096)
    private String contentText;

    @Column(name = "content_url", length = 1024)
    private String contentUrl;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "content_id", length = 128)
    private String contentId;

    @Column(name = "review_type", length = 16)
    private String reviewType;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private String decision;

    @Column(length = 16)
    private String priority;

    @Column(name = "violations_json", columnDefinition = "TEXT")
    private String violationsJson;

    @Column
    private Instant createdAt = Instant.now();

    @Column
    private Instant reviewedAt;

    @Column(length = 128)
    private String reviewerId;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    // -- Getters and setters --
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public String getContentUrl() { return contentUrl; }
    public void setContentUrl(String contentUrl) { this.contentUrl = contentUrl; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    public String getReviewType() { return reviewType; }
    public void setReviewType(String reviewType) { this.reviewType = reviewType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getViolationsJson() { return violationsJson; }
    public void setViolationsJson(String violationsJson) { this.violationsJson = violationsJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
}
