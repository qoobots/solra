package com.solra.saf.domain.repository;

import com.solra.saf.domain.model.ReviewCase;
import com.solra.saf.domain.model.ReviewStatus;
import java.util.List;
import java.util.Optional;

public interface ReviewCaseRepository {
    Optional<ReviewCase> findById(String caseId);
    List<ReviewCase> findByUserId(String userId);
    List<ReviewCase> findPending(ReviewStatus status, int limit);
    ReviewCase save(ReviewCase reviewCase);
    long countByUserId(String userId);
}
