package com.solra.avt.domain.model;

import java.util.List;
import java.util.Map;

public class AvatarConfig {
    private String modelId;
    private String voiceId;
    private String languageCode;
    private List<String> supportedLanguages;
    private AvatarAppearance appearance;
    private AnimationConfig animation;

    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public String getVoiceId() { return voiceId; }
    public void setVoiceId(String voiceId) { this.voiceId = voiceId; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
    public List<String> getSupportedLanguages() { return supportedLanguages; }
    public void setSupportedLanguages(List<String> supportedLanguages) { this.supportedLanguages = supportedLanguages; }
    public AvatarAppearance getAppearance() { return appearance; }
    public void setAppearance(AvatarAppearance appearance) { this.appearance = appearance; }
    public AnimationConfig getAnimation() { return animation; }
    public void setAnimation(AnimationConfig animation) { this.animation = animation; }

    public static class AvatarAppearance {
        private String avatarType;
        private Map<String, String> customizations;
        public String getAvatarType() { return avatarType; }
        public void setAvatarType(String avatarType) { this.avatarType = avatarType; }
        public Map<String, String> getCustomizations() { return customizations; }
        public void setCustomizations(Map<String, String> customizations) { this.customizations = customizations; }
    }

    public static class AnimationConfig {
        private float gestureSpeed = 1.0f;
        private float expressionIntensity = 1.0f;
        private boolean autoBlink = true;
        private boolean autoGesture = true;
        public float getGestureSpeed() { return gestureSpeed; }
        public void setGestureSpeed(float gestureSpeed) { this.gestureSpeed = gestureSpeed; }
        public float getExpressionIntensity() { return expressionIntensity; }
        public void setExpressionIntensity(float expressionIntensity) { this.expressionIntensity = expressionIntensity; }
        public boolean isAutoBlink() { return autoBlink; }
        public void setAutoBlink(boolean autoBlink) { this.autoBlink = autoBlink; }
        public boolean isAutoGesture() { return autoGesture; }
        public void setAutoGesture(boolean autoGesture) { this.autoGesture = autoGesture; }
    }
}
