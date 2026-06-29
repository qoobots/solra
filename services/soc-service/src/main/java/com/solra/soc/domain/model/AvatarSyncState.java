package com.solra.soc.domain.model;

/**
 * AvatarSyncState — SOC-001 虚拟人同步状态值对象。
 * 用于多人空间内虚拟人姿态/动画/表情的实时同步。
 */
public class AvatarSyncState {

    private String avatarId;
    private Transform transform;
    private AnimationState animation;
    private EmotionState emotion;
    private GestureState gesture;
    private long timestampMs;

    public AvatarSyncState() {
        this.transform = Transform.defaults();
        this.animation = AnimationState.idle();
        this.emotion = EmotionState.neutral();
        this.gesture = GestureState.none();
        this.timestampMs = System.currentTimeMillis();
    }

    public AvatarSyncState(String avatarId) {
        this();
        this.avatarId = avatarId;
    }

    // -- Getters/Setters --
    public String getAvatarId() { return avatarId; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }
    public Transform getTransform() { return transform; }
    public void setTransform(Transform transform) { this.transform = transform; }
    public AnimationState getAnimation() { return animation; }
    public void setAnimation(AnimationState animation) { this.animation = animation; }
    public EmotionState getEmotion() { return emotion; }
    public void setEmotion(EmotionState emotion) { this.emotion = emotion; }
    public GestureState getGesture() { return gesture; }
    public void setGesture(GestureState gesture) { this.gesture = gesture; }
    public long getTimestampMs() { return timestampMs; }
    public void setTimestampMs(long timestampMs) { this.timestampMs = timestampMs; }

    // -- Nested value objects --

    public static class Transform {
        private float positionX, positionY, positionZ;
        private float rotationX, rotationY, rotationZ;
        private float scaleX = 1f, scaleY = 1f, scaleZ = 1f;

        public static Transform defaults() { return new Transform(); }

        public float getPositionX() { return positionX; }
        public void setPositionX(float x) { this.positionX = x; }
        public float getPositionY() { return positionY; }
        public void setPositionY(float y) { this.positionY = y; }
        public float getPositionZ() { return positionZ; }
        public void setPositionZ(float z) { this.positionZ = z; }
        public float getRotationX() { return rotationX; }
        public void setRotationX(float x) { this.rotationX = x; }
        public float getRotationY() { return rotationY; }
        public void setRotationY(float y) { this.rotationY = y; }
        public float getRotationZ() { return rotationZ; }
        public void setRotationZ(float z) { this.rotationZ = z; }
        public float getScaleX() { return scaleX; }
        public void setScaleX(float x) { this.scaleX = x; }
        public float getScaleY() { return scaleY; }
        public void setScaleY(float y) { this.scaleY = y; }
        public float getScaleZ() { return scaleZ; }
        public void setScaleZ(float z) { this.scaleZ = z; }
    }

    public static class AnimationState {
        private String currentAnimationId = "idle";
        private float playheadTime;
        private float speed = 1f;
        private float blendWeight = 1f;
        private boolean loop = true;

        public static AnimationState idle() { return new AnimationState(); }

        public String getCurrentAnimationId() { return currentAnimationId; }
        public void setCurrentAnimationId(String id) { this.currentAnimationId = id; }
        public float getPlayheadTime() { return playheadTime; }
        public void setPlayheadTime(float t) { this.playheadTime = t; }
        public float getSpeed() { return speed; }
        public void setSpeed(float s) { this.speed = s; }
        public float getBlendWeight() { return blendWeight; }
        public void setBlendWeight(float w) { this.blendWeight = w; }
        public boolean isLoop() { return loop; }
        public void setLoop(boolean loop) { this.loop = loop; }
    }

    public static class EmotionState {
        private String emotionId = "neutral";
        private float intensity;
        private long emotionTimestampMs;

        public static EmotionState neutral() { return new EmotionState(); }

        public String getEmotionId() { return emotionId; }
        public void setEmotionId(String id) { this.emotionId = id; }
        public float getIntensity() { return intensity; }
        public void setIntensity(float i) { this.intensity = i; }
        public long getEmotionTimestampMs() { return emotionTimestampMs; }
        public void setEmotionTimestampMs(long ts) { this.emotionTimestampMs = ts; }
    }

    public static class GestureState {
        private GestureType type = GestureType.IDLE;
        private float progress;
        private long gestureTimestampMs;

        public static GestureState none() { return new GestureState(); }

        public GestureType getType() { return type; }
        public void setType(GestureType type) { this.type = type; }
        public float getProgress() { return progress; }
        public void setProgress(float p) { this.progress = p; }
        public long getGestureTimestampMs() { return gestureTimestampMs; }
        public void setGestureTimestampMs(long ts) { this.gestureTimestampMs = ts; }
    }

    public enum GestureType {
        WAVE, CLAP, POINT, THUMBS_UP, DANCE, IDLE, CUSTOM
    }
}
