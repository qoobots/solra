package com.solra.saf.domain.repository;

import com.solra.saf.domain.model.Appeal;
import com.solra.saf.domain.model.AppealStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Appeal aggregate.
 * Covers: SAF-005 (用户申诉通道).
 */
public interface AppealRepository {
    Optional<Appeal> findById(String appealId);
    List<Appeal> findByUserId(String userId);
    List<Appeal> findByCaseId(String caseId);
    List<Appeal> findByStatus(AppealStatus status);
    List<Appeal> findPendingReview(int limit);
    Appeal save(Appeal appeal);
    boolean existsByCaseIdAndUserId(String caseId, String userId);
}
