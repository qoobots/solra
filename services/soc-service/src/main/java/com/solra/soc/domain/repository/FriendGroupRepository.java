package com.solra.soc.domain.repository;

import com.solra.soc.domain.model.FriendGroup;

import java.util.List;
import java.util.Optional;

/**
 * FriendGroup 仓储接口。
 */
public interface FriendGroupRepository {

    FriendGroup save(FriendGroup group);

    Optional<FriendGroup> findById(String groupId);

    List<FriendGroup> findByUserId(String userId);

    List<FriendGroup> findByUserIdAndMember(String userId, String friendUserId);

    void deleteById(String groupId);
}
