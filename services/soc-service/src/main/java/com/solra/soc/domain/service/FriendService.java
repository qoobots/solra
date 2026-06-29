package com.solra.soc.domain.service;

import com.solra.soc.domain.model.FriendGroup;
import com.solra.soc.domain.model.FriendPresence;

import java.util.List;
import java.util.Optional;

/**
 * FriendService 领域服务 — 好友系统核心业务逻辑。
 * <p>
 * SOC-004: 好友分组管理 + 在线状态感知 + 好友上线/同空间通知。
 */
public interface FriendService {

    // ---- 好友分组 ----

    /** 创建好友分组 */
    FriendGroup createGroup(String userId, String groupName, int sortOrder);

    /** 获取用户所有分组 */
    List<FriendGroup> getUserGroups(String userId);

    /** 获取指定分组 */
    Optional<FriendGroup> getGroup(String groupId);

    /** 更新分组（重命名/排序） */
    FriendGroup updateGroup(FriendGroup group);

    /** 删除分组 */
    void deleteGroup(String groupId);

    /** 添加好友到分组 */
    void addFriendToGroup(String groupId, String friendUserId);

    /** 从分组移除好友 */
    void removeFriendFromGroup(String groupId, String friendUserId);

    /** 获取好友所属分组列表 */
    List<FriendGroup> getFriendGroups(String userId, String friendUserId);

    // ---- 在线状态感知 ----

    /** 更新好友在线状态 */
    void updateFriendPresence(String userId, String spaceId, String spaceName, boolean isOnline);

    /** 获取单个好友的在线状态 */
    Optional<FriendPresence> getFriendPresence(String userId, String friendUserId);

    /** 获取用户所有在线好友 */
    List<FriendPresence> getOnlineFriends(String userId);

    /** 获取同空间内的好友 */
    List<FriendPresence> getFriendsInSameSpace(String userId, String spaceId);

    /** 获取好友在线状态列表（含离线） */
    List<FriendPresence> getAllFriendsPresence(String userId, int page, int size);

    // ---- 好友统计 ----

    /** 获取好友统计 */
    FriendStats getFriendStats(String userId);

    record FriendStats(long totalFriends, long onlineFriends, long inSameSpace, int groupCount) {}
}
