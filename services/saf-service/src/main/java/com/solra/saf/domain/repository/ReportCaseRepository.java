package com.solra.saf.domain.repository;

import com.solra.saf.domain.model.ReportCase;
import com.solra.saf.domain.model.ReportStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ReportCase aggregate.
 * Covers: SAF-003 (用户举报→审核→处理闭环).
 */
public interface ReportCaseRepository {
    Optional<ReportCase> findById(String reportId);
    List<ReportCase> findByReporterUserId(String userId);
    List<ReportCase> findByStatus(ReportStatus status);
    List<ReportCase> findPendingReview(int limit);
    ReportCase save(ReportCase reportCase);
    long countByReporterUserId(String userId);
    long countByReportedUserId(String userId);
}
