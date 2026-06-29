package com.solra.saf.domain.model;

/**
 * AppealStatus — lifecycle of a user appeal.
 * Covers: SAF-005 (用户申诉通道).
 */
public enum AppealStatus {
    SUBMITTED,
    IN_REVIEW,
    RESOLVED
}
