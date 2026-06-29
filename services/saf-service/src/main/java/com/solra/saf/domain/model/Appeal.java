package com.solra.saf.domain.model;

import java.time.Instant;
import java.util.*;

/**
 * Appeal aggregate root — user appeal against a review decision.
 * Covers: SAF-005 (用户申诉通道).
 *
 * An appeal goes through: SUBMITTED → IN_REVIEW → RESOLVED.
 * Resolved can be: UPHELD (申诉成功), DENIED (驳回), or PARTIAL (部分撤销).
 */
public class Appeal {

    private String appealId;
    private String caseId;              // 关联的原始审核案例ID
    private String userId;              // 申诉用户
    private String reason;              // 申诉理由
    private List<String> evidenceUrls = new ArrayList<>();
    private AppealStatus status;
    private AppealDecision decision;
    private String decisionReason;
    private String reviewerId;
    private Instant submittedAt;
    private Instant reviewedAt;
    private Instant resolvedAt;
    private Map<String, String> metadata = new HashMap<>();

    private Appeal() {}

    public static Appeal create(String caseId, String userId, String reason,
                                 List<String> evidenceUrls) {
        Appeal a = new Appeal();
        a.appealId = UUID.randomUUID().toString();
        a.caseId = caseId;
        a.userId = userId;
        a.reason = reason;
        a.evidenceUrls = evidenceUrls != null ? new ArrayList<>(evidenceUrls) : new ArrayList<>();
        a.status = AppealStatus.SUBMITTED;
        a.submittedAt = Instant.now();
        return a;
    }

    /**
     * Start review by a reviewer.
     */
    public void startReview(String reviewerId) {
        this.status = AppealStatus.IN_REVIEW;
        this.reviewerId = reviewerId;
        this.reviewedAt = Instant.now();
    }

    /**
     * Uphold the appeal — reverse the original decision.
     */
    public void uphold(String reason, String reviewerId) {
        this.status = AppealStatus.RESOLVED;
        this.decision = AppealDecision.UPHELD;
        this.decisionReason = reason;
        this.reviewerId = reviewerId;
        this.resolvedAt = Instant.now();
    }

    /**
     * Deny the appeal — original decision stands.
     */
    public void deny(String reason, String reviewerId) {
        this.status = AppealStatus.RESOLVED;
        this.decision = AppealDecision.DENIED;
        this.decisionReason = reason;
        this.reviewerId = reviewerId;
        this.resolvedAt = Instant.now();
    }

    /**
     * Partially uphold — some violations removed.
     */
    public void partialUphold(String reason, String reviewerId) {
        this.status = AppealStatus.RESOLVED;
        this.decision = AppealDecision.PARTIAL;
        this.decisionReason = reason;
        this.reviewerId = reviewerId;
        this.resolvedAt = Instant.now();
    }

    public boolean isResolved() {
        return status == AppealStatus.RESOLVED;
    }

    // -- Getters --
    public String getAppealId() { return appealId; }
    public String getCaseId() { return caseId; }
    public String getUserId() { return userId; }
    public String getReason() { return reason; }
    public List<String> getEvidenceUrls() { return Collections.unmodifiableList(evidenceUrls); }
    public AppealStatus getStatus() { return status; }
    public AppealDecision getDecision() { return decision; }
    public String getDecisionReason() { return decisionReason; }
    public String getReviewerId() { return reviewerId; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Map<String, String> getMetadata() { return Collections.unmodifiableMap(metadata); }
}
