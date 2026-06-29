package com.solra.saf.domain.model;

import java.time.Instant;
import java.util.*;

/**
 * ReviewCase aggregate root — each content review submission creates a case.
 * Covers: SAF-001 (3D空间内容审核), SAF-002 (虚拟人对话安全过滤).
 */
public class ReviewCase {

    private String caseId;
    private String userId;
    private ContentTarget target;
    private ReviewType reviewType;
    private ReviewStatus status;
    private ReviewDecision decision;
    private List<PolicyViolation> violations = new ArrayList<>();
    private ReviewPriority priority;
    private Instant submittedAt;
    private Instant reviewedAt;
    private String reviewerId;
    private Map<String, String> metadata = new HashMap<>();

    private ReviewCase() {}

    public static ReviewCase create(String userId, ContentTarget target,
                                     ReviewType type, ReviewPriority priority) {
        ReviewCase c = new ReviewCase();
        c.caseId = UUID.randomUUID().toString();
        c.userId = userId;
        c.target = target;
        c.reviewType = type;
        c.status = ReviewStatus.PENDING;
        c.priority = priority;
        c.submittedAt = Instant.now();
        return c;
    }

    public void autoReview(ReviewDecision decision, List<PolicyViolation> violations,
                            float confidence, String modelVersion) {
        this.decision = decision;
        this.violations = violations != null ? violations : List.of();
        this.status = confidence >= 0.9f ? ReviewStatus.COMPLETED : ReviewStatus.ESCALATED;
        this.reviewedAt = Instant.now();
        this.reviewerId = "auto:" + modelVersion;
    }

    public void manualReview(ReviewDecision decision, String reason, String reviewerId) {
        this.decision = decision;
        this.metadata.put("manual_review_reason", reason);
        this.status = ReviewStatus.COMPLETED;
        this.reviewedAt = Instant.now();
        this.reviewerId = "manual:" + reviewerId;
    }

    public boolean isPassed() {
        return decision == ReviewDecision.APPROVED;
    }

    public boolean isRejected() {
        return decision == ReviewDecision.REJECTED || decision == ReviewDecision.BLOCKED;
    }

    // -- Getters --
    public String getCaseId() { return caseId; }
    public String getUserId() { return userId; }
    public ContentTarget getTarget() { return target; }
    public ReviewType getReviewType() { return reviewType; }
    public ReviewStatus getStatus() { return status; }
    public ReviewDecision getDecision() { return decision; }
    public List<PolicyViolation> getViolations() { return Collections.unmodifiableList(violations); }
    public ReviewPriority getPriority() { return priority; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewerId() { return reviewerId; }
    public Map<String, String> getMetadata() { return Collections.unmodifiableMap(metadata); }
}
