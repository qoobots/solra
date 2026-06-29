package com.solra.saf.domain.service;

import com.solra.saf.domain.model.*;
import com.solra.saf.domain.repository.ReviewCaseRepository;
import com.solra.common.exception.SolraException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain service — core safety review orchestration.
 * Covers: SAF-001 (content review), SAF-002 (dialogue safety filter).
 */
@Service
public class SafetyDomainService {

    private final ReviewCaseRepository reviewCaseRepository;
    private final List<ContentFilter> contentFilters;

    public SafetyDomainService(ReviewCaseRepository reviewCaseRepository,
                                List<ContentFilter> contentFilters) {
        this.reviewCaseRepository = reviewCaseRepository;
        this.contentFilters = contentFilters;
    }

    /**
     * SAF-001: Submit content for review.
     */
    public ReviewCase submitReview(String userId, ContentTarget target,
                                    ReviewType type, ReviewPriority priority) {
        ReviewCase reviewCase = ReviewCase.create(userId, target, type, priority);

        if (type == ReviewType.AUTOMATIC || type == ReviewType.HYBRID) {
            ContentFilter.FilterResult result = filterContent(target.getContentText(), target.getContentType());
            if (result != null) {
                ReviewDecision decision = result.passed() ? ReviewDecision.APPROVED : ReviewDecision.REJECTED;
                reviewCase.autoReview(decision, result.violations(), result.score().getOverallScore(), "auto-v1");
            }
        }

        return reviewCaseRepository.save(reviewCase);
    }

    /**
     * SAF-002: Real-time text content filtering for virtual avatar dialogue.
     * @return true if content passes safety check
     */
    public boolean filterDialogue(String userId, String dialogueText) {
        ContentFilter.FilterResult result = filterContent(dialogueText, ContentType.AVATAR_SPEECH);
        if (result == null) return true; // No applicable filter — pass through

        if (!result.passed()) {
            // Log rejected dialogue for audit
            ContentTarget target = ContentTarget.avatarSpeech(userId + "_" + System.currentTimeMillis(), dialogueText);
            ReviewCase reviewCase = ReviewCase.create(userId, target,
                    ReviewType.AUTOMATIC, ReviewPriority.URGENT);
            reviewCase.autoReview(ReviewDecision.BLOCKED, result.violations(),
                    result.score().getOverallScore(), "dialogue-filter-v1");
            reviewCaseRepository.save(reviewCase);
        }
        return result.passed();
    }

    /**
     * SAF-001: Query review result.
     */
    public ReviewCase queryReview(String caseId) {
        return reviewCaseRepository.findById(caseId)
                .orElseThrow(() -> new SolraException.NotFoundException("Review case not found: " + caseId));
    }

    /**
     * Get safety score for content without creating a review case.
     */
    public SafetyScore getSafetyScore(String content, ContentType contentType) {
        ContentFilter.FilterResult result = filterContent(content, contentType);
        if (result == null) return SafetyScore.safe("no-filter");
        return result.score();
    }

    /**
     * Batch review multiple pieces of content.
     */
    public List<ReviewCase> batchReview(String userId, List<ContentTarget> targets,
                                         ReviewType type, ReviewPriority priority) {
        List<ReviewCase> results = new ArrayList<>();
        for (ContentTarget target : targets) {
            results.add(submitReview(userId, target, type, priority));
        }
        return results;
    }

    private ContentFilter.FilterResult filterContent(String content, ContentType contentType) {
        if (content == null || content.isBlank()) return null;

        for (ContentFilter filter : contentFilters) {
            if (filter.supports(contentType)) {
                return filter.filter(content);
            }
        }
        return null;
    }
}
