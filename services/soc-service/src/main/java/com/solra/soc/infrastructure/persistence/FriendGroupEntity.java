package com.solra.soc.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * FriendGroup JPA 实体 — 好友分组持久化映射。
 */
@Entity
@Table(name = "friend_groups")
public class FriendGroupEntity {

    @Id
    @Column(name = "group_id", length = 64)
    private String groupId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "group_name", length = 50, nullable = false)
    private String groupName;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "member_user_ids", columnDefinition = "TEXT")
    private String memberUserIds; // JSON array: ["uid1","uid2"]

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public FriendGroupEntity() {}

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public String getMemberUserIds() { return memberUserIds; }
    public void setMemberUserIds(String memberUserIds) { this.memberUserIds = memberUserIds; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
