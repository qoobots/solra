package com.solra.saf.domain.service;

import com.solra.saf.domain.model.*;

/**
 * Content filtering interface — pluggable filter implementations per content type.
 * SAF-002: Virtual avatar dialogue safety filter is the primary H1 implementation.
 */
public interface ContentFilter {

    /**
     * Filter a piece of content and return safety score + violations.
     *
     * @param content the content to filter
     * @return filter result with pass/fail + violations
     */
    FilterResult filter(String content);

    /**
     * Check if this filter supports the given content type.
     */
    boolean supports(ContentType contentType);

    record FilterResult(boolean passed, SafetyScore score,
                        java.util.List<PolicyViolation> violations) {
        public static FilterResult pass(SafetyScore score) {
            return new FilterResult(true, score, java.util.List.of());
        }

        public static FilterResult reject(SafetyScore score,
                                           java.util.List<PolicyViolation> violations) {
            return new FilterResult(false, score, violations);
        }
    }
}
