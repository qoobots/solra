package com.solra.saf.domain.service;

import com.solra.saf.domain.model.*;
import com.solra.saf.domain.repository.ReviewCaseRepository;
import com.solra.common.exception.SolraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Domain service for the manual review workbench.
 * Covers: SAF-004 (UGC内容人工审核工作台).
 *
 * Provides queue management for human reviewers to process content reviews.
 * Target: ≥200 items per person per hour review efficiency.
 */
@Service
public class ManualReviewWorkbenchService {

    private static final Logger log = LoggerFactory.getLogger(ManualReviewWorkbenchService.class);
    private static final int MAX_CLAIM_PER_REVIEWER = 20;

    private final ReviewCaseRepository reviewCaseRepository;

    public ManualReviewWorkbenchService(ReviewCaseRepository reviewCaseRepository) {
        this.reviewCaseRepository = reviewCaseRepository;
    }

    /**
     * SAF-004: Get review queue for workbench.
     * Returns cases that are PENDING/ESCALATED and require manual or hybrid review.
     */
    public List<ReviewWorkbenchItem> getWorkQueue(String reviewerId, int limit) {
        List<ReviewCase> pendingCases = reviewCaseRepository
                .findByStatusAndReviewType(ReviewStatus.PENDING, ReviewType.MANUAL, limit);

        List<ReviewCase> escalatedCases = reviewCaseRepository
                .findByStatusAndReviewType(ReviewStatus.ESCALATED, ReviewType.HYBRID, limit);

        List<ReviewCase> hybridPending = reviewCaseRepository
                .findByStatusAndReviewType(ReviewStatus.PENDING, ReviewType.HYBRID, limit);

        List<ReviewWorkbenchItem> queue = new ArrayList<>();
        queue.addAll(escalatedCases.stream()
                .map(ReviewWorkbenchItem::fromReviewCase)
                .collect(Collectors.toList()));
        queue.addAll(pendingCases.stream()
                .map(ReviewWorkbenchItem::fromReviewCase)
                .collect(Collectors.toList()));
        queue.addAll(hybridPending.stream()
                .map(ReviewWorkbenchItem::fromReviewCase)
                .collect(Collectors.toList()));

        // Sort by priority: URGENT → HIGH → NORMAL → LOW
        queue.sort((a, b) -> Integer.compare(
                priorityWeight(b.getPriority()), priorityWeight(a.getPriority())));

        log.debug("Work queue returned {} items for reviewer {}", queue.size(), reviewerId);
        return queue.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * SAF-004: Claim a review item from the queue.
     */
    public ReviewWorkbenchItem claimItem(String caseId, String reviewerId) {
        ReviewCase reviewCase = reviewCaseRepository.findById(caseId)
                .orElseThrow(() -> new SolraException.NotFoundException(
                        "Review case not found: " + caseId));

        if (reviewCase.getStatus() != ReviewStatus.PENDING
                && reviewCase.getStatus() != ReviewStatus.ESCALATED) {
            throw new SolraException.BadRequestException(
                    "Case " + caseId + " is not available for review (status=" + reviewCase.getStatus() + ")");
        }

        ReviewWorkbenchItem item = ReviewWorkbenchItem.fromReviewCase(reviewCase);
        item.claim(reviewerId);
        log.info("Review case {} claimed by {}", caseId, reviewerId);
        return item;
    }

    /**
     * SAF-004: Submit manual review decision.
     */
    public ReviewCase submitManualReview(String caseId, ReviewDecision decision,
                                          String reason, String reviewerId) {
        ReviewCase reviewCase = reviewCaseRepository.findById(caseId)
                .orElseThrow(() -> new SolraException.NotFoundException(
                        "Review case not found: " + caseId));

        reviewCase.manualReview(decision, reason, reviewerId);
        reviewCase = reviewCaseRepository.save(reviewCase);
        log.info("Manual review submitted for case {}: decision={}, by {}",
                caseId, decision, reviewerId);
        return reviewCase;
    }

    /**
     * SAF-004: Get reviewer statistics.
     */
    public ReviewerStats getReviewerStats(String reviewerId) {
        long pendingCount = reviewCaseRepository.countByStatus(ReviewStatus.PENDING);
        long escalatedCount = reviewCaseRepository.countByStatus(ReviewStatus.ESCALATED);
        return new ReviewerStats(reviewerId, pendingCount, escalatedCount);
    }

    /**
     * Reviewer statistics value object.
     */
    public record ReviewerStats(String reviewerId, long pendingCount, long escalatedCount) {}

    private int priorityWeight(ReviewPriority priority) {
        return switch (priority) {
            case URGENT -> 4;
            case HIGH -> 3;
            case NORMAL -> 2;
            case LOW -> 1;
        };
    }
}
