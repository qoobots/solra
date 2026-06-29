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
