package com.solra.soc.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * FriendGroup 实体 — 好友分组，支持用户自定义好友分组管理。
 * <p>
 * SOC-004: 好友分组（如"亲密好友""同事""游戏伙伴"等）。
 */
public class FriendGroup {

    private String groupId;
    private String userId;
    private String groupName;
    private int sortOrder;
    private List<String> memberUserIds = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public FriendGroup() {}

    public FriendGroup(String groupId, String userId, String groupName, int sortOrder) {
        this.groupId = groupId;
        this.userId = userId;
        this.groupName = groupName;
        this.sortOrder = sortOrder;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // ---- business methods ----

    /** 添加成员到分组 */
    public void addMember(String friendUserId) {
        if (!memberUserIds.contains(friendUserId)) {
            memberUserIds.add(friendUserId);
            this.updatedAt = Instant.now();
        }
    }

    /** 从分组移除成员 */
    public void removeMember(String friendUserId) {
        memberUserIds.remove(friendUserId);
        this.updatedAt = Instant.now();
    }

    /** 重命名分组 */
    public void rename(String newName) {
        this.groupName = newName;
        this.updatedAt = Instant.now();
    }

    /** 调整排序 */
    public void reorder(int newOrder) {
        this.sortOrder = newOrder;
        this.updatedAt = Instant.now();
    }

    /** 检查某用户是否在分组中 */
    public boolean contains(String friendUserId) {
        return memberUserIds.contains(friendUserId);
    }

    // ---- getters / setters ----

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public List<String> getMemberUserIds() { return memberUserIds; }
    public void setMemberUserIds(List<String> memberUserIds) { this.memberUserIds = memberUserIds; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
