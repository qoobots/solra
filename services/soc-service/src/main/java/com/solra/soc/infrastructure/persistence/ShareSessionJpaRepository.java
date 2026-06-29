package com.solra.soc.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ShareSession JPA Repository。
 */
@Repository
public interface ShareSessionJpaRepository extends JpaRepository<ShareSessionEntity, String> {

    Optional<ShareSessionEntity> findByShareCode(String shareCode);

    List<ShareSessionEntity> findBySharerUserIdOrderByCreatedAtDesc(String sharerUserId);
}
