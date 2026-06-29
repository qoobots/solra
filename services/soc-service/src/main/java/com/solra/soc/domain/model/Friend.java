package com.solra.soc.domain.model;

import java.time.Instant;

/**
 * Friend 实体 — 表示用户之间的好友关系。
 * <p>
 * 覆盖 SOC-004 好友功能：发送好友请求、接受/拒绝、拉黑。
 */
public class Friend {

    private String friendshipId;
    private String userId;
    private String friendUserId;
    private FriendStatus status;
    private Instant createdAt;
    private Instant acceptedAt;

    public Friend() {}

    /**
     * 创建一条好友关系记录。
     *
     * @param friendshipId 关系唯一标识
     * @param userId       发起方用户ID
     * @param friendUserId 目标方用户ID
     */
    public Friend(String friendshipId, String userId, String friendUserId) {
        this.friendshipId = friendshipId;
        this.userId = userId;
        this.friendUserId = friendUserId;
        this.status = FriendStatus.PENDING;
        this.createdAt = Instant.now();
    }

    // ---- business methods ----

    /** 接受好友请求。 */
    public void accept() {
        if (this.status != FriendStatus.PENDING) {
            throw new IllegalStateException("Friendship is not in PENDING state: " + friendshipId);
        }
        this.status = FriendStatus.ACCEPTED;
        this.acceptedAt = Instant.now();
    }

    /** 拉黑好友关系。 */
    public void block() {
        this.status = FriendStatus.BLOCKED;
    }

    /** 取消拉黑，恢复为已接受状态。 */
    public void unblock() {
        if (this.status == FriendStatus.BLOCKED) {
            this.status = FriendStatus.ACCEPTED;
        }
    }

    public boolean isPending() { return this.status == FriendStatus.PENDING; }
    public boolean isAccepted() { return this.status == FriendStatus.ACCEPTED; }
    public boolean isBlocked() { return this.status == FriendStatus.BLOCKED; }

    // ---- getters / setters ----

    public String getFriendshipId() { return friendshipId; }
    public void setFriendshipId(String friendshipId) { this.friendshipId = friendshipId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFriendUserId() { return friendUserId; }
    public void setFriendUserId(String friendUserId) { this.friendUserId = friendUserId; }

    public FriendStatus getStatus() { return status; }
    public void setStatus(FriendStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
}
