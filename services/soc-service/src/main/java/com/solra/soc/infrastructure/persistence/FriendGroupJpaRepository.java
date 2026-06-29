package com.solra.soc.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * FriendGroup JPA Repository。
 */
@Repository
public interface FriendGroupJpaRepository extends JpaRepository<FriendGroupEntity, String> {

    List<FriendGroupEntity> findByUserIdOrderBySortOrder(String userId);

    @Query("SELECT g FROM FriendGroupEntity g WHERE g.userId = :userId AND g.memberUserIds LIKE %:friendUserId%")
    List<FriendGroupEntity> findByUserIdAndMember(@Param("userId") String userId,
                                                   @Param("friendUserId") String friendUserId);
}
