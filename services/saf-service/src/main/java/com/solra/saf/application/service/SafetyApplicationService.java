package com.solra.saf.application.service;

import com.solra.saf.application.dto.FilterRequest;
import com.solra.saf.application.dto.FilterResultDTO;
import com.solra.saf.domain.model.*;
import com.solra.saf.domain.service.*;
import com.solra.common.exception.SolraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for content safety operations.
 * Covers: SAF-001 (content review), SAF-002 (dialogue safety filter),
 *         SAF-003 (user report), SAF-004 (manual review workbench),
 *         SAF-005 (user appeal).
 */
@Service
public class SafetyApplicationService {

    private static final Logger log = LoggerFactory.getLogger(SafetyApplicationService.class);
    private final SafetyDomainService domainService;
    private final ReportService reportService;
    private final ManualReviewWorkbenchService workbenchService;
    private final AppealService appealService;

    public SafetyApplicationService(SafetyDomainService domainService,
                                     ReportService reportService,
                                     ManualReviewWorkbenchService workbenchService,
                                     AppealService appealService) {
        this.domainService = domainService;
        this.reportService = reportService;
        this.workbenchService = workbenchService;
        this.appealService = appealService;
    }

    // ========== SAF-001 & SAF-002: Existing methods ==========

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

    // ========== SAF-003: User Report ==========

    /**
     * SAF-003: Submit a report against content or user.
     */
    @Transactional
    public ReportCase submitReport(String reporterUserId, String reportedUserId,
                                    String reportedContentId, String reportReason,
                                    ReportCategory category, List<String> evidenceUrls) {
        return reportService.submitReport(reporterUserId, reportedUserId,
                reportedContentId, reportReason, category, evidenceUrls);
    }

    /**
     * SAF-003: Query a report by ID.
     */
    public ReportCase queryReport(String reportId) {
        return reportService.queryReport(reportId);
    }

    /**
     * SAF-003: List reports by reporter user.
     */
    public List<ReportCase> listUserReports(String userId) {
        return reportService.listUserReports(userId);
    }

    // ========== SAF-004: Manual Review Workbench ==========

    /**
     * SAF-004: Get work queue for a reviewer.
     */
    public List<ReviewWorkbenchItem> getWorkQueue(String reviewerId, int limit) {
        return workbenchService.getWorkQueue(reviewerId, limit);
    }

    /**
     * SAF-004: Claim a review item from the queue.
     */
    @Transactional
    public ReviewWorkbenchItem claimReviewItem(String caseId, String reviewerId) {
        return workbenchService.claimItem(caseId, reviewerId);
    }

    /**
     * SAF-004: Submit manual review decision.
     */
    @Transactional
    public ReviewCase submitManualReview(String caseId, ReviewDecision decision,
                                          String reason, String reviewerId) {
        return workbenchService.submitManualReview(caseId, decision, reason, reviewerId);
    }

    /**
     * SAF-004: Get reviewer statistics.
     */
    public ManualReviewWorkbenchService.ReviewerStats getReviewerStats(String reviewerId) {
        return workbenchService.getReviewerStats(reviewerId);
    }

    // ========== SAF-005: User Appeal ==========

    /**
     * SAF-005: Submit an appeal against a review decision.
     */
    @Transactional
    public Appeal submitAppeal(String caseId, String userId, String reason,
                                List<String> evidenceUrls) {
        return appealService.submitAppeal(caseId, userId, reason, evidenceUrls);
    }

    /**
     * SAF-005: Query an appeal by ID.
     */
    public Appeal queryAppeal(String appealId) {
        return appealService.queryAppeal(appealId);
    }

    /**
     * SAF-005: List appeals for a user.
     */
    public List<Appeal> listUserAppeals(String userId) {
        return appealService.listUserAppeals(userId);
    }
}
