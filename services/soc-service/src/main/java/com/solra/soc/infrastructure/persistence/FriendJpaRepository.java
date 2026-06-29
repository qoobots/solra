package com.solra.soc.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Friend JPA Repository。
 */
@Repository
public interface FriendJpaRepository extends JpaRepository<FriendEntity, String> {

    List<FriendEntity> findByUserId(String userId, Pageable pageable);

    long countByUserId(String userId);

    Optional<FriendEntity> findByUserIdAndFriendUserId(String userId, String friendUserId);
}
