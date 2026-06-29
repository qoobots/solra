package com.solra.soc.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Friend JPA 实体 — 好友关系持久化映射。
 */
@Entity
@Table(name = "friends", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "friend_user_id"})
})
public class FriendEntity {

    @Id
    @Column(name = "friendship_id", length = 64)
    private String friendshipId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "friend_user_id", length = 64, nullable = false)
    private String friendUserId;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    public FriendEntity() {}

    public String getFriendshipId() { return friendshipId; }
    public void setFriendshipId(String friendshipId) { this.friendshipId = friendshipId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFriendUserId() { return friendUserId; }
    public void setFriendUserId(String friendUserId) { this.friendUserId = friendUserId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
}
