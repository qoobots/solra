package com.solra.saf.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewCaseJpaRepository extends JpaRepository<ReviewCaseEntity, String> {
    List<ReviewCaseEntity> findByUserId(String userId);
    List<ReviewCaseEntity> findByStatusOrderByCreatedAtAsc(String status);
    long countByUserId(String userId);
}
