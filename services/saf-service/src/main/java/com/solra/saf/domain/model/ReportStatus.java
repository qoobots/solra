package com.solra.saf.domain.model;

/**
 * ReportStatus — lifecycle of a user-submitted report.
 * Covers: SAF-003 (用户举报→审核→处理闭环).
 */
public enum ReportStatus {
    /** User has submitted the report */
    SUBMITTED,
    /** Report queued for review (auto-triage complete) */
    PENDING_REVIEW,
    /** Reviewer is actively investigating */
    IN_REVIEW,
    /** Report has been resolved (action taken or dismissed) */
    RESOLVED
}
