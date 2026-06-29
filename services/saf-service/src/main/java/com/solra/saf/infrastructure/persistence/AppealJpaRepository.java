package com.solra.saf.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppealJpaRepository extends JpaRepository<AppealEntity, String> {
    List<AppealEntity> findByUserId(String userId);
    List<AppealEntity> findByCaseId(String caseId);
    List<AppealEntity> findByStatusOrderBySubmittedAtAsc(String status);
    boolean existsByCaseIdAndUserId(String caseId, String userId);
}
