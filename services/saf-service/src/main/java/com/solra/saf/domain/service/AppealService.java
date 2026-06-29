package com.solra.saf.domain.service;

import com.solra.saf.domain.model.*;
import com.solra.saf.domain.repository.AppealRepository;
import com.solra.saf.domain.repository.ReviewCaseRepository;
import com.solra.common.exception.SolraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Domain service for user appeal handling.
 * Covers: SAF-005 (用户申诉通道).
 *
 * Appeal lifecycle: SUBMITTED → IN_REVIEW → RESOLVED (UPHELD/DENIED/PARTIAL)
 * SLA: ≤3 steps to submit, <48 hours to process.
 */
@Service
public class AppealService {

    private static final Logger log = LoggerFactory.getLogger(AppealService.class);
    private final AppealRepository appealRepository;
    private final ReviewCaseRepository reviewCaseRepository;

    public AppealService(AppealRepository appealRepository,
                         ReviewCaseRepository reviewCaseRepository) {
        this.appealRepository = appealRepository;
        this.reviewCaseRepository = reviewCaseRepository;
    }

    /**
     * SAF-005: Submit an appeal against a review decision.
     * User must provide a reason and optional evidence.
     */
    public Appeal submitAppeal(String caseId, String userId, String reason,
                                List<String> evidenceUrls) {
        // Validate that the review case exists
        ReviewCase reviewCase = reviewCaseRepository.findById(caseId)
                .orElseThrow(() -> new SolraException.NotFoundException(
                        "Review case not found: " + caseId));

        // Only rejected/blocked cases can be appealed
        if (!reviewCase.isRejected()) {
            throw new SolraException.BadRequestException(
                    "Only rejected or blocked cases can be appealed. Current decision: "
                            + reviewCase.getDecision());
        }

        // One appeal per case per user
        if (appealRepository.existsByCaseIdAndUserId(caseId, userId)) {
            throw new SolraException.BadRequestException(
                    "An appeal already exists for case " + caseId + " by user " + userId);
        }

        // Reason is required
        if (reason == null || reason.isBlank()) {
            throw new SolraException.BadRequestException(
                    "Appeal reason is required");
        }

        // Max 5 evidence URLs
        if (evidenceUrls != null && evidenceUrls.size() > 5) {
            throw new SolraException.BadRequestException(
                    "Maximum 5 evidence URLs allowed");
        }

        Appeal appeal = Appeal.create(caseId, userId, reason, evidenceUrls);
        appeal = appealRepository.save(appeal);
        log.info("Appeal submitted: appealId={}, caseId={}, userId={}",
                appeal.getAppealId(), caseId, userId);
        return appeal;
    }

    /**
     * SAF-005: Start appeal review.
     */
    public Appeal startAppealReview(String appealId, String reviewerId) {
        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new SolraException.NotFoundException(
                        "Appeal not found: " + appealId));

        if (appeal.isResolved()) {
            throw new SolraException.BadRequestException(
                    "Appeal " + appealId + " is already resolved");
        }

        appeal.startReview(reviewerId);
        appeal = appealRepository.save(appeal);
        log.info("Appeal {} review started by {}", appealId, reviewerId);
        return appeal;
    }

    /**
     * SAF-005: Uphold appeal (reverse original decision).
     */
    public Appeal upholdAppeal(String appealId, String reason, String reviewerId) {
        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new SolraException.NotFoundException(
                        "Appeal not found: " + appealId));

        if (appeal.isResolved()) {
            throw new SolraException.BadRequestException(
                    "Appeal " + appealId + " is already resolved");
        }

        appeal.uphold(reason, reviewerId);
        appeal = appealRepository.save(appeal);
        log.info("Appeal {} UPHELD by {}: {}", appealId, reviewerId, reason);
        return appeal;
    }

    /**
     * SAF-005: Deny appeal (original decision stands).
     */
    public Appeal denyAppeal(String appealId, String reason, String reviewerId) {
        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new SolraException.NotFoundException(
                        "Appeal not found: " + appealId));

        if (appeal.isResolved()) {
            throw new SolraException.BadRequestException(
                    "Appeal " + appealId + " is already resolved");
        }

        appeal.deny(reason, reviewerId);
        appeal = appealRepository.save(appeal);
        log.info("Appeal {} DENIED by {}: {}", appealId, reviewerId, reason);
        return appeal;
    }

    /**
     * SAF-005: Query appeal by ID.
     */
    public Appeal queryAppeal(String appealId) {
        return appealRepository.findById(appealId)
                .orElseThrow(() -> new SolraException.NotFoundException(
                        "Appeal not found: " + appealId));
    }

    /**
     * SAF-005: List appeals for a user.
     */
    public List<Appeal> listUserAppeals(String userId) {
        return appealRepository.findByUserId(userId);
    }

    /**
     * SAF-005: Get pending appeals for review queue.
     */
    public List<Appeal> getPendingAppeals(int limit) {
        return appealRepository.findPendingReview(limit);
    }
}
