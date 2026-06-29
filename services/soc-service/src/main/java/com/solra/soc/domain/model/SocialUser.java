package com.solra.soc.domain.model;

import java.time.Instant;

/**
 * SocialUser 实体 — 用户社交基础档案。
 * <p>
 * 存储用户在社交场景下的基本展示信息，包括昵称、头像和在线状态。
 */
public class SocialUser {

    private String userId;
    private String nickname;
    private String avatarUrl;
    private OnlineStatus onlineStatus;
    private Instant lastActiveAt;

    public SocialUser() {}

    /**
     * 创建一个新的社交用户档案。
     *
     * @param userId   用户ID
     * @param nickname 昵称
     * @param avatarUrl 头像URL
     */
    public SocialUser(String userId, String nickname, String avatarUrl) {
        this.userId = userId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.onlineStatus = OnlineStatus.OFFLINE;
        this.lastActiveAt = Instant.now();
    }

    // ---- business methods ----

    /** 标记用户上线。 */
    public void markOnline() {
        this.onlineStatus = OnlineStatus.ONLINE;
        this.lastActiveAt = Instant.now();
    }

    /** 标记用户离线。 */
    public void markOffline() {
        this.onlineStatus = OnlineStatus.OFFLINE;
        this.lastActiveAt = Instant.now();
    }

    /** 标记用户暂离。 */
    public void markAway() {
        this.onlineStatus = OnlineStatus.AWAY;
        this.lastActiveAt = Instant.now();
    }

    /** 更新最后活跃时间。 */
    public void touch() {
        this.lastActiveAt = Instant.now();
    }

    /** 更新个人资料。 */
    public void updateProfile(String nickname, String avatarUrl) {
        if (nickname != null) this.nickname = nickname;
        if (avatarUrl != null) this.avatarUrl = avatarUrl;
    }

    // ---- getters / setters ----

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public OnlineStatus getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(OnlineStatus onlineStatus) { this.onlineStatus = onlineStatus; }

    public Instant getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(Instant lastActiveAt) { this.lastActiveAt = lastActiveAt; }
}
