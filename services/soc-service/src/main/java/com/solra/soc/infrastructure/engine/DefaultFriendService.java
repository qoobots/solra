package com.solra.soc.infrastructure.engine;

import com.solra.soc.domain.model.Friend;
import com.solra.soc.domain.model.FriendGroup;
import com.solra.soc.domain.model.FriendPresence;
import com.solra.soc.domain.model.FriendStatus;
import com.solra.soc.domain.model.OnlineStatus;
import com.solra.soc.domain.repository.FriendGroupRepository;
import com.solra.soc.domain.repository.FriendRepository;
import com.solra.soc.domain.service.FriendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DefaultFriendService — 好友系统领域服务实现。
 * <p>
 * 好友分组 + 在线状态感知（基于内存缓存，生产环境可替换为 Redis）。
 */
@Component
public class DefaultFriendService implements FriendService {

    private static final Logger log = LoggerFactory.getLogger(DefaultFriendService.class);

    private final FriendRepository friendRepository;
    private final FriendGroupRepository friendGroupRepository;

    /** 在线状态缓存: userId -> FriendPresence */
    private final Map<String, FriendPresence> presenceCache = new ConcurrentHashMap<>();

    public DefaultFriendService(FriendRepository friendRepository,
                                FriendGroupRepository friendGroupRepository) {
        this.friendRepository = friendRepository;
        this.friendGroupRepository = friendGroupRepository;
    }

    // ===== 好友分组 =====

    @Override
    public FriendGroup createGroup(String userId, String groupName, int sortOrder) {
        String groupId = UUID.randomUUID().toString();
        FriendGroup group = new FriendGroup(groupId, userId, groupName, sortOrder);
        FriendGroup saved = friendGroupRepository.save(group);
        log.info("Friend group created: {} for user {}", groupId, userId);
        return saved;
    }

    @Override
    public List<FriendGroup> getUserGroups(String userId) {
        return friendGroupRepository.findByUserId(userId);
    }

    @Override
    public Optional<FriendGroup> getGroup(String groupId) {
        return friendGroupRepository.findById(groupId);
    }

    @Override
    public FriendGroup updateGroup(FriendGroup group) {
        return friendGroupRepository.save(group);
    }

    @Override
    public void deleteGroup(String groupId) {
        friendGroupRepository.deleteById(groupId);
        log.info("Friend group deleted: {}", groupId);
    }

    @Override
    public void addFriendToGroup(String groupId, String friendUserId) {
        FriendGroup group = friendGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));
        group.addMember(friendUserId);
        friendGroupRepository.save(group);
        log.info("Friend {} added to group {}", friendUserId, groupId);
    }

    @Override
    public void removeFriendFromGroup(String groupId, String friendUserId) {
        FriendGroup group = friendGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));
        group.removeMember(friendUserId);
        friendGroupRepository.save(group);
        log.info("Friend {} removed from group {}", friendUserId, groupId);
    }

    @Override
    public List<FriendGroup> getFriendGroups(String userId, String friendUserId) {
        return friendGroupRepository.findByUserIdAndMember(userId, friendUserId);
    }

    // ===== 在线状态感知 =====

    @Override
    public void updateFriendPresence(String userId, String spaceId, String spaceName, boolean isOnline) {
        FriendPresence presence = presenceCache.computeIfAbsent(userId,
                k -> new FriendPresence(userId, "User-" + userId, null, OnlineStatus.OFFLINE, null, null, Instant.now()));

        if (isOnline) {
            presence.updateOnlineStatus(OnlineStatus.ONLINE, spaceId, spaceName);
        } else {
            presence.updateOnlineStatus(OnlineStatus.OFFLINE, null, null);
        }
        log.debug("Friend presence updated: {} status={} space={}", userId,
                isOnline ? "ONLINE" : "OFFLINE", spaceId);
    }

    @Override
    public Optional<FriendPresence> getFriendPresence(String userId, String friendUserId) {
        // 先检查是否为好友
        Optional<Friend> friendship = friendRepository.findFriendship(userId, friendUserId);
        if (friendship.isEmpty() || !friendship.get().isAccepted()) {
            return Optional.empty();
        }
        return Optional.ofNullable(presenceCache.get(friendUserId));
    }

    @Override
    public List<FriendPresence> getOnlineFriends(String userId) {
        List<Friend> acceptedFriends = friendRepository.findByUserId(userId, 0, 1000);
        return acceptedFriends.stream()
                .filter(Friend::isAccepted)
                .map(f -> presenceCache.get(f.getFriendUserId()))
                .filter(Objects::nonNull)
                .filter(FriendPresence::isOnline)
                .toList();
    }

    @Override
    public List<FriendPresence> getFriendsInSameSpace(String userId, String spaceId) {
        List<Friend> acceptedFriends = friendRepository.findByUserId(userId, 0, 1000);
        return acceptedFriends.stream()
                .filter(Friend::isAccepted)
                .map(f -> presenceCache.get(f.getFriendUserId()))
                .filter(Objects::nonNull)
                .filter(p -> p.isInSameSpace(spaceId))
                .toList();
    }

    @Override
    public List<FriendPresence> getAllFriendsPresence(String userId, int page, int size) {
        int offset = page * size;
        List<Friend> acceptedFriends = friendRepository.findByUserId(userId, 0, 1000);
        return acceptedFriends.stream()
                .filter(Friend::isAccepted)
                .skip(offset)
                .limit(size)
                .map(f -> presenceCache.getOrDefault(f.getFriendUserId(),
                        new FriendPresence(f.getFriendUserId(), "User-" + f.getFriendUserId(),
                                null, OnlineStatus.OFFLINE, null, null, Instant.now())))
                .toList();
    }

    // ===== 好友统计 =====

    @Override
    public FriendStats getFriendStats(String userId) {
        long totalFriends = friendRepository.countByUserId(userId);
        List<FriendPresence> online = getOnlineFriends(userId);
        List<FriendGroup> groups = friendGroupRepository.findByUserId(userId);
        return new FriendStats(totalFriends, online.size(), 0, groups.size());
    }
}
