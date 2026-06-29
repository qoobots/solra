package com.solra.soc.domain.repository;

import com.solra.soc.domain.model.Friend;

import java.util.List;
import java.util.Optional;

/**
 * Friend 仓储接口。
 * <p>
 * 定义领域层对好友关系的持久化操作契约，不依赖任何基础设施框架。
 */
public interface FriendRepository {

    /**
     * 保存（新增或更新）一条好友关系。
     */
    Friend save(Friend friend);

    /** 分页查找用户好友列表 */
    List<Friend> findByUserId(String userId, int page, int size);

    /** 统计用户好友总数 */
    long countByUserId(String userId);

    /** 按ID查找 */
    Optional<Friend> findByFriendshipId(String friendshipId);

    Optional<Friend> findFriendship(String userId, String friendUserId);

    void delete(String friendshipId);
}
