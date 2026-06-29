package com.solra.saf.application.service;

import com.solra.saf.application.dto.FilterRequest;
import com.solra.saf.application.dto.FilterResultDTO;
import com.solra.saf.domain.model.*;
import com.solra.saf.domain.service.SafetyDomainService;
import com.solra.common.exception.SolraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for content safety operations.
 * Covers: SAF-001 (content review), SAF-002 (dialogue safety filter).
 */
@Service
public class SafetyApplicationService {

    private static final Logger log = LoggerFactory.getLogger(SafetyApplicationService.class);
    private final SafetyDomainService domainService;

    public SafetyApplicationService(SafetyDomainService domainService) {
        this.domainService = domainService;
    }

    /**
     * SAF-002: Real-time content filtering for dialogue, text, profiles, etc.
     * This is the primary safety API called by other services.
     */
    public FilterResultDTO filterContent(FilterRequest request) {
        if (request.content() == null || request.content().isBlank()) {
            return new FilterResultDTO(true, 1.0f, request.content(), List.of());
        }

        boolean passed = domainService.filterDialogue(request.userId(), request.content());

        ContentFilter.FilterResult fr = null;
        for (var filter : java.util.ServiceLoader.load(ContentFilter.class)) {
            if (filter.supports(request.contentType())) {
                fr = filter.filter(request.content());
                break;
            }
        }

        if (fr == null) {
            return new FilterResultDTO(true, 1.0f, request.content(), List.of());
        }

        List<FilterResultDTO.ViolationDTO> violations = fr.violations().stream()
                .map(v -> new FilterResultDTO.ViolationDTO(
                        v.getCategory().name(), v.getSeverity().name(), v.getDescription()))
                .toList();

        return new FilterResultDTO(fr.passed(), fr.score().getOverallScore(),
                request.content(), violations);
    }

    /**
     * SAF-001: Submit content for formal review (creates a review case).
     */
    @Transactional
    public ReviewCase submitForReview(String userId, ContentTarget target,
                                       ReviewType reviewType, ReviewPriority priority) {
        return domainService.submitReview(userId, target, reviewType, priority);
    }

    /**
     * Quick safety check — no case created, just score.
     */
    public SafetyScore checkSafety(String content, ContentType contentType) {
        return domainService.getSafetyScore(content, contentType);
    }

    /**
     * Query an existing review case.
     */
    public ReviewCase queryReviewCase(String caseId) {
        return domainService.queryReview(caseId);
    }
}
