package com.solra.soc.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ShareClick JPA Repository。
 */
@Repository
public interface ShareClickJpaRepository extends JpaRepository<ShareClickEntity, String> {

    java.util.List<ShareClickEntity> findByShareIdOrderByTimestampDesc(String shareId);

    Optional<ShareClickEntity> findByShareIdAndVisitorUserId(String shareId, String visitorUserId);
}
