package com.solra.grw.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface UserProfileJpaRepository extends JpaRepository<UserProfileEntity, String> {
    Optional<UserProfileEntity> findByUserId(String userId);
}

@Repository
interface DecisiveMomentJpaRepository extends JpaRepository<DecisiveMomentEntity, String> {
    List<DecisiveMomentEntity> findByUserId(String userId);
    Optional<DecisiveMomentEntity> findByUserIdAndMomentType(String userId, String momentType);
}

@Repository
interface OnboardingPathJpaRepository extends JpaRepository<OnboardingPathEntity, String> {
    Optional<OnboardingPathEntity> findByUserId(String userId);
}

@Repository
interface ExperienceEventJpaRepository extends JpaRepository<ExperienceEventEntity, String> {
    List<ExperienceEventEntity> findByUserIdOrderByTimestampDesc(String userId,
            org.springframework.data.domain.Pageable pageable);
    List<ExperienceEventEntity> findByUserIdAndTimestampBetween(String userId, java.time.Instant from,
            java.time.Instant to);
    @org.springframework.data.jpa.repository.Query(
            "SELECT COALESCE(SUM(e.value), 0) FROM ExperienceEventEntity e WHERE e.userId = :userId")
    int sumValueByUserId(@org.springframework.data.repository.query.Param("userId") String userId);
}

@Repository
interface RecallStrategyJpaRepository extends JpaRepository<RecallStrategyEntity, String> {
    List<RecallStrategyEntity> findByTargetRiskLevel(String targetRiskLevel);
    List<RecallStrategyEntity> findByActiveTrue();
}

@Repository
interface RecallTaskJpaRepository extends JpaRepository<RecallTaskEntity, String> {
    List<RecallTaskEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    List<RecallTaskEntity> findByUserIdAndStatus(String userId, String status);
    int countByUserIdAndStatus(String userId, String status);
    List<RecallTaskEntity> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(String userId, java.time.Instant after,
            org.springframework.data.domain.Pageable pageable);
    List<RecallTaskEntity> findByStatusOrderByCreatedAtAsc(String status,
            org.springframework.data.domain.Pageable pageable);
}
