package com.solra.saf.domain.model;

/**
 * AppealDecision — outcome of an appeal review.
 * Covers: SAF-005 (用户申诉通道).
 */
public enum AppealDecision {
    /** Appeal successful — original decision reversed */
    UPHELD,
    /** Appeal denied — original decision stands */
    DENIED,
    /** Partially upheld — some violations removed */
    PARTIAL
}
