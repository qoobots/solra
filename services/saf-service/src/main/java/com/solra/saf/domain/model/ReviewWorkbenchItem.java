package com.solra.saf.domain.model;

import java.time.Instant;
import java.util.*;

/**
 * ReviewWorkbenchItem — view model for the manual review workbench.
 * Covers: SAF-004 (UGC内容人工审核工作台).
 *
 * Represents a single item in the reviewer's work queue.
 */
public class ReviewWorkbenchItem {

    private String itemId;
    private String reviewCaseId;
    private String reporterUserId;          // null if system-auto review
    private String targetContentText;       // preview of content
    private String targetContentUrl;        // link to full content
    private ContentType contentType;
    private ReviewType reviewType;
    private ReviewPriority priority;
    private ReviewStatus status;
    private String assignedReviewerId;
    private Instant queuedAt;
    private Instant claimedAt;
    private Instant completedAt;
    private Map<String, String> metadata = new HashMap<>();

    private ReviewWorkbenchItem() {}

    public static ReviewWorkbenchItem fromReviewCase(ReviewCase reviewCase) {
        ReviewWorkbenchItem item = new ReviewWorkbenchItem();
        item.itemId = UUID.randomUUID().toString();
        item.reviewCaseId = reviewCase.getCaseId();
        item.targetContentText = reviewCase.getTarget().getContentText();
        item.targetContentUrl = reviewCase.getTarget().getContentUrl();
        item.contentType = reviewCase.getTarget().getContentType();
        item.reviewType = reviewCase.getReviewType();
        item.priority = reviewCase.getPriority();
        item.status = reviewCase.getStatus();
        item.queuedAt = reviewCase.getSubmittedAt();
        return item;
    }

    /**
     * Claim by a reviewer.
     */
    public void claim(String reviewerId) {
        this.assignedReviewerId = reviewerId;
        this.status = ReviewStatus.IN_PROGRESS;
        this.claimedAt = Instant.now();
    }

    /**
     * Complete review.
     */
    public void complete() {
        this.status = ReviewStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /**
     * Escalate to senior reviewer.
     */
    public void escalate() {
        this.status = ReviewStatus.ESCALATED;
    }

    // -- Getters --
    public String getItemId() { return itemId; }
    public String getReviewCaseId() { return reviewCaseId; }
    public String getReporterUserId() { return reporterUserId; }
    public String getTargetContentText() { return targetContentText; }
    public String getTargetContentUrl() { return targetContentUrl; }
    public ContentType getContentType() { return contentType; }
    public ReviewType getReviewType() { return reviewType; }
    public ReviewPriority getPriority() { return priority; }
    public ReviewStatus getStatus() { return status; }
    public String getAssignedReviewerId() { return assignedReviewerId; }
    public Instant getQueuedAt() { return queuedAt; }
    public Instant getClaimedAt() { return claimedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Map<String, String> getMetadata() { return Collections.unmodifiableMap(metadata); }

    public void setReporterUserId(String reporterUserId) { this.reporterUserId = reporterUserId; }
}
