package com.solra.avt.domain.model;

import java.time.Instant;

/**
 * AvatarState 值对象 — 虚拟人实时状态快照。
 */
public class AvatarState {

    private String avatarId;
    private EmotionState emotion;
    private ActivityState activity;
    private Instant lastActive;

    public AvatarState() {
        this.activity = ActivityState.IDLE;
        this.lastActive = Instant.now();
    }

    public boolean isBusy() {
        return activity == ActivityState.SPEAKING || activity == ActivityState.THINKING;
    }

    // ---- getters / setters ----
    public String getAvatarId() { return avatarId; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }

    public EmotionState getEmotion() { return emotion; }
    public void setEmotion(EmotionState emotion) { this.emotion = emotion; }

    public ActivityState getActivity() { return activity; }
    public void setActivity(ActivityState activity) { this.activity = activity; }

    public Instant getLastActive() { return lastActive; }
    public void setLastActive(Instant lastActive) { this.lastActive = lastActive; }
}
