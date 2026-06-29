package com.solra.saf.domain.event;

/**
 * Domain event emitted when content is flagged for review.
 */
public record ContentFlaggedEvent(
    String caseId,
    String userId,
    String contentType,
    String decision,
    long flaggedAt
) {}

/**
 * Domain event emitted when content passes safety review.
 */
public record ContentApprovedEvent(
    String caseId,
    String userId,
    String reviewerId,
    long approvedAt
) {}

/**
 * Domain event emitted when a user files an appeal.
 */
public record UserAppealedEvent(
    String appealId,
    String caseId,
    String userId,
    long appealedAt
) {}
