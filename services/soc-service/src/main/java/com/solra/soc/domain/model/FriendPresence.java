package com.solra.soc.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * FriendPresence 值对象 — 好友在线状态感知。
 * <p>
 * SOC-004: 好友上线/进入同空间通知。聚合好友的在线状态、所在空间信息。
 */
public class FriendPresence {

    private String userId;
    private String nickname;
    private String avatarUrl;
    private OnlineStatus onlineStatus;
    private String currentSpaceId;
    private String currentSpaceName;
    private Instant lastSeenAt;
    private String statusText;

    public FriendPresence() {}

    public FriendPresence(String userId, String nickname, String avatarUrl,
                          OnlineStatus onlineStatus, String currentSpaceId,
                          String currentSpaceName, Instant lastSeenAt) {
        this.userId = userId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.onlineStatus = onlineStatus;
        this.currentSpaceId = currentSpaceId;
        this.currentSpaceName = currentSpaceName;
        this.lastSeenAt = lastSeenAt;
    }

    /** 是否在线 */
    public boolean isOnline() {
        return onlineStatus == OnlineStatus.ONLINE;
    }

    /** 是否在同一空间 */
    public boolean isInSameSpace(String spaceId) {
        return currentSpaceId != null && currentSpaceId.equals(spaceId);
    }

    /** 更新在线状态 */
    public void updateOnlineStatus(OnlineStatus status, String spaceId, String spaceName) {
        this.onlineStatus = status;
        if (status == OnlineStatus.ONLINE) {
            this.currentSpaceId = spaceId;
            this.currentSpaceName = spaceName;
        } else {
            this.lastSeenAt = Instant.now();
        }
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

    public String getCurrentSpaceId() { return currentSpaceId; }
    public void setCurrentSpaceId(String currentSpaceId) { this.currentSpaceId = currentSpaceId; }

    public String getCurrentSpaceName() { return currentSpaceName; }
    public void setCurrentSpaceName(String currentSpaceName) { this.currentSpaceName = currentSpaceName; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
}
