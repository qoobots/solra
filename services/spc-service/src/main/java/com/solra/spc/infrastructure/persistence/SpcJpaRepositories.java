package com.solra.spc.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SpaceJpaRepository extends JpaRepository<SpaceEntity, String> {
    List<SpaceEntity> findByIdIn(List<String> spaceIds);

    @Query("SELECT s FROM SpaceEntity s WHERE s.status = 'PUBLISHED' " +
           "AND (:category IS NULL OR s.category = :category) ORDER BY s.updatedAt DESC")
    List<SpaceEntity> findPublished(Pageable pageable, String category);

    @Query("SELECT s FROM SpaceEntity s WHERE s.status = 'PUBLISHED' " +
           "AND (:category IS NULL OR s.category = :category) ORDER BY s.viewCount DESC")
    List<SpaceEntity> findPublishedByPopular(Pageable pageable, String category);

    @Query("SELECT COUNT(s) FROM SpaceEntity s WHERE s.status = 'PUBLISHED' " +
           "AND (:category IS NULL OR s.category = :category)")
    long countPublished(String category);

    @Modifying
    @Query("UPDATE SpaceEntity s SET s.viewCount = s.viewCount + 1 WHERE s.spaceId = :spaceId")
    void incrementViewCount(String spaceId);

    @Modifying
    @Query("UPDATE SpaceEntity s SET s.likeCount = s.likeCount + 1 WHERE s.spaceId = :spaceId")
    void incrementLikeCount(String spaceId);

    @Modifying
    @Query("UPDATE SpaceEntity s SET s.shareCount = s.shareCount + 1 WHERE s.spaceId = :spaceId")
    void incrementShareCount(String spaceId);
}

@Repository
interface UserActionJpaRepository extends JpaRepository<UserActionEntity, String> {
    List<UserActionEntity> findByUserIdOrderByActionTimeDesc(String userId, Pageable pageable);
    List<UserActionEntity> findBySpaceIdOrderByActionTimeDesc(String spaceId, Pageable pageable);
}
