package com.solra.avt.domain.model;

import java.time.Instant;

/**
 * Avatar 实体 — 虚拟人（由创作者配置 / 平台默认提供）。
 */
public class Avatar {

    private String avatarId;
    private String displayName;
    private AvatarConfig config;
    private AvatarState state;
    private AvatarPersonality personality;
    private Instant createdAt;
    private Instant updatedAt;

    public Avatar() {}

    public Avatar(String avatarId, String displayName) {
        this.avatarId = avatarId;
        this.displayName = displayName;
        this.state = new AvatarState();
        this.state.setAvatarId(avatarId);
        this.state.setActivity(ActivityState.IDLE);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // ---- getters / setters ----
    public String getAvatarId() { return avatarId; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public AvatarConfig getConfig() { return config; }
    public void setConfig(AvatarConfig config) { this.config = config; }

    public AvatarState getState() { return state; }
    public void setState(AvatarState state) { this.state = state; }

    public AvatarPersonality getPersonality() { return personality; }
    public void setPersonality(AvatarPersonality personality) { this.personality = personality; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
