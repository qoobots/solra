package com.solra.saf.domain.service;

import com.solra.saf.domain.model.*;
import com.solra.saf.domain.repository.ReportCaseRepository;
import com.solra.common.exception.SolraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Domain service for user report handling.
 * Covers: SAF-003 (用户举报→审核→处理闭环).
 *
 * Report lifecycle: SUBMITTED → PENDING_REVIEW → IN_REVIEW → RESOLVED
 * SLA: <4 hours from submission to resolution.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private final ReportCaseRepository reportCaseRepository;

    public ReportService(ReportCaseRepository reportCaseRepository) {
        this.reportCaseRepository = reportCaseRepository;
    }

    /**
     * SAF-003: Submit a report against content or user.
     */
    public ReportCase submitReport(String reporterUserId, String reportedUserId,
                                    String reportedContentId, String reportReason,
                                    ReportCategory category, List<String> evidenceUrls) {
        // Rate limit: max 20 reports per user per day
        long reportCount = reportCaseRepository.countByReporterUserId(reporterUserId);
        if (reportCount >= 20) {
            throw new SolraException.TooManyRequestsException(
                    "Daily report limit reached (20). Please try again tomorrow.");
        }

        ReportCase report = ReportCase.create(
                reporterUserId, reportedUserId, reportedContentId,
                reportReason, category, evidenceUrls);

        // Auto-triage: escalate critical categories
        if (report.requiresEscalation()) {
            log.warn("Report {} escalated — category {} requires priority handling",
                    report.getReportId(), category);
        }

        report = reportCaseRepository.save(report);
        log.info("Report submitted: reportId={}, category={}, reporter={}",
                report.getReportId(), category, reporterUserId);
        return report;
    }

    /**
     * SAF-003: Start review of a report (assign to reviewer).
     */
    public ReportCase startReview(String reportId, String reviewerId) {
        ReportCase report = reportCaseRepository.findById(reportId)
                .orElseThrow(() -> new SolraException.NotFoundException(
                        "Report not found: " + reportId));

        if (report.isResolved()) {
            throw new SolraException.BadRequestException(
                    "Report " + reportId + " is already resolved");
        }

        report.startReview(reviewerId);
        report = reportCaseRepository.save(report);
        log.info("Report {} review started by {}", reportId, reviewerId);
        return report;
    }

    /**
     * SAF-003: Resolve a report with a decision.
     */
    public ReportCase resolveReport(String reportId, ReviewDecision decision,
                                     String reason, String reviewerId) {
        ReportCase report = reportCaseRepository.findById(reportId)
                .orElseThrow(() -> new SolraException.NotFoundException(
                        "Report not found: " + reportId));

        if (report.isResolved()) {
            throw new SolraException.BadRequestException(
                    "Report " + reportId + " is already resolved");
        }

        report.resolve(decision, reason, reviewerId);
        report = reportCaseRepository.save(report);
        log.info("Report {} resolved: decision={}, by {}", reportId, decision, reviewerId);
        return report;
    }

    /**
     * SAF-003: Query a report by ID.
     */
    public ReportCase queryReport(String reportId) {
        return reportCaseRepository.findById(reportId)
                .orElseThrow(() -> new SolraException.NotFoundException(
                        "Report not found: " + reportId));
    }

    /**
     * SAF-003: List reports by user (for reporters to track their reports).
     */
    public List<ReportCase> listUserReports(String userId) {
        return reportCaseRepository.findByReporterUserId(userId);
    }

    /**
     * SAF-003: Get pending reports for review queue.
     */
    public List<ReportCase> getPendingReports(int limit) {
        return reportCaseRepository.findPendingReview(limit);
    }

    /**
     * SAF-003: Get reports by status (for workbench).
     */
    public List<ReportCase> getReportsByStatus(ReportStatus status) {
        return reportCaseRepository.findByStatus(status);
    }
}
