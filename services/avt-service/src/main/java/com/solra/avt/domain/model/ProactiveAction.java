package com.solra.avt.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * ProactiveAction 值对象 — 虚拟人主动互动行为（AVT-002）。
 * 由 ProactiveGreetingService 根据 PresenceEvent 生成。
 */
public class ProactiveAction {

    private String actionId;
    private GreetingTrigger trigger;
    private String message;
    private float enthusiasm;
    private String avatarId;
    private String avatarName;
    private String userId;
    private String spaceId;
    private boolean requiresResponse;
    private Map<String, String> metadata;
    private String suggestedAnimation;
    private Instant generatedAt;

    public ProactiveAction() {}

    // ---- builder pattern ----
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ProactiveAction action = new ProactiveAction();

        public Builder actionId(String actionId) { action.actionId = actionId; return this; }
        public Builder trigger(GreetingTrigger trigger) { action.trigger = trigger; return this; }
        public Builder message(String message) { action.message = message; return this; }
        public Builder enthusiasm(float enthusiasm) { action.enthusiasm = enthusiasm; return this; }
        public Builder avatarId(String avatarId) { action.avatarId = avatarId; return this; }
        public Builder avatarName(String avatarName) { action.avatarName = avatarName; return this; }
        public Builder userId(String userId) { action.userId = userId; return this; }
        public Builder spaceId(String spaceId) { action.spaceId = spaceId; return this; }
        public Builder requiresResponse(boolean requiresResponse) { action.requiresResponse = requiresResponse; return this; }
        public Builder metadata(Map<String, String> metadata) { action.metadata = metadata; return this; }
        public Builder suggestedAnimation(String suggestedAnimation) { action.suggestedAnimation = suggestedAnimation; return this; }
        public Builder generatedAt(Instant generatedAt) { action.generatedAt = generatedAt; return this; }

        public ProactiveAction build() { return action; }
    }

    // ---- getters / setters ----
    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }

    public GreetingTrigger getTrigger() { return trigger; }
    public void setTrigger(GreetingTrigger trigger) { this.trigger = trigger; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public float getEnthusiasm() { return enthusiasm; }
    public void setEnthusiasm(float enthusiasm) { this.enthusiasm = enthusiasm; }

    public String getAvatarId() { return avatarId; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }

    public String getAvatarName() { return avatarName; }
    public void setAvatarName(String avatarName) { this.avatarName = avatarName; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }

    public boolean isRequiresResponse() { return requiresResponse; }
    public void setRequiresResponse(boolean requiresResponse) { this.requiresResponse = requiresResponse; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public String getSuggestedAnimation() { return suggestedAnimation; }
    public void setSuggestedAnimation(String suggestedAnimation) { this.suggestedAnimation = suggestedAnimation; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
