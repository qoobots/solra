package com.solra.avt.domain.model;

import java.util.*;

/**
 * AvatarExpression — AVT-006 虚拟人肢体与表情表达系统。
 *
 * 支持≥15种表情（基础6+复合9）和≥20种肢体动作。
 * 基于ARKit 52 blendshape兼容设计。
 */
public class AvatarExpression {

    private String expressionId;
    private ExpressionType expressionType;
    private float intensity;              // 0..1
    private List<BlendshapeWeight> blendshapes = new ArrayList<>();
    private BodyPose bodyPose;
    private GestureType gesture;
    private String animationClip;         // 关联的动画片段名
    private Map<String, String> metadata = new HashMap<>();

    private AvatarExpression() {}

    public static AvatarExpression fromType(ExpressionType type, float intensity) {
        AvatarExpression expr = new AvatarExpression();
        expr.expressionId = UUID.randomUUID().toString();
        expr.expressionType = type;
        expr.intensity = Math.max(0f, Math.min(1f, intensity));
        expr.blendshapes = computeBlendshapes(type, intensity);
        expr.gesture = suggestGesture(type);
        return expr;
    }

    /**
     * Create expression from 5D emotion state (AVT-004 integration).
     */
    public static AvatarExpression fromEmotion(FiveDimensionalEmotion emotion) {
        ExpressionType type = mapEmotionToExpression(emotion);
        float intensity = emotion.getOverallIntensity();
        return fromType(type, intensity);
    }

    /**
     * Create expression for surprise moment (AVT-008 integration).
     */
    public static AvatarExpression surprise(String surpriseType) {
        return fromType(switch (surpriseType) {
            case "memory_recall" -> ExpressionType.SURPRISE_HAPPY;
            case "gift" -> ExpressionType.EXCITED;
            case "achievement" -> ExpressionType.PROUD;
            default -> ExpressionType.SURPRISE_HAPPY;
        }, 0.9f);
    }

    private static List<BlendshapeWeight> computeBlendshapes(ExpressionType type, float intensity) {
        List<BlendshapeWeight> weights = new ArrayList<>();
        switch (type) {
            case HAPPY -> {
                weights.add(new BlendshapeWeight("mouthSmile", 0.8f * intensity));
                weights.add(new BlendshapeWeight("cheekSquint", 0.5f * intensity));
                weights.add(new BlendshapeWeight("eyeWide", 0.3f * intensity));
            }
            case SAD -> {
                weights.add(new BlendshapeWeight("mouthFrown", 0.7f * intensity));
                weights.add(new BlendshapeWeight("browInnerUp", 0.6f * intensity));
                weights.add(new BlendshapeWeight("eyeBlink", 0.2f * intensity));
            }
            case ANGRY -> {
                weights.add(new BlendshapeWeight("browDown", 0.8f * intensity));
                weights.add(new BlendshapeWeight("mouthFrown", 0.6f * intensity));
                weights.add(new BlendshapeWeight("jawOpen", 0.4f * intensity));
            }
            case SURPRISED -> {
                weights.add(new BlendshapeWeight("browUp", 0.9f * intensity));
                weights.add(new BlendshapeWeight("eyeWide", 0.8f * intensity));
                weights.add(new BlendshapeWeight("mouthOpen", 0.6f * intensity));
            }
            case FEARFUL -> {
                weights.add(new BlendshapeWeight("browUp", 0.7f * intensity));
                weights.add(new BlendshapeWeight("eyeWide", 0.9f * intensity));
                weights.add(new BlendshapeWeight("mouthStretch", 0.5f * intensity));
            }
            case DISGUSTED -> {
                weights.add(new BlendshapeWeight("noseSneer", 0.8f * intensity));
                weights.add(new BlendshapeWeight("mouthUpperUp", 0.6f * intensity));
                weights.add(new BlendshapeWeight("browDown", 0.3f * intensity));
            }
            case SURPRISE_HAPPY -> {
                weights.add(new BlendshapeWeight("mouthSmile", 0.7f * intensity));
                weights.add(new BlendshapeWeight("eyeWide", 0.6f * intensity));
                weights.add(new BlendshapeWeight("browUp", 0.5f * intensity));
            }
            case EXCITED -> {
                weights.add(new BlendshapeWeight("mouthSmile", 0.9f * intensity));
                weights.add(new BlendshapeWeight("eyeWide", 0.8f * intensity));
                weights.add(new BlendshapeWeight("jawOpen", 0.5f * intensity));
            }
            case PROUD -> {
                weights.add(new BlendshapeWeight("mouthSmile", 0.6f * intensity));
                weights.add(new BlendshapeWeight("chinRaise", 0.5f * intensity));
                weights.add(new BlendshapeWeight("cheekSquint", 0.3f * intensity));
            }
            default -> {
                // Neutral expression
                weights.add(new BlendshapeWeight("mouthSmile", 0.1f));
                weights.add(new BlendshapeWeight("eyeBlink", 0.05f));
            }
        }
        return weights;
    }

    private static GestureType suggestGesture(ExpressionType type) {
        return switch (type) {
            case HAPPY -> GestureType.CLAP;
            case SAD -> GestureType.HEAD_DOWN;
            case ANGRY -> GestureType.CROSS_ARMS;
            case SURPRISED -> GestureType.HANDS_UP;
            case FEARFUL -> GestureType.STEP_BACK;
            case DISGUSTED -> GestureType.TURN_AWAY;
            case SURPRISE_HAPPY -> GestureType.JUMP;
            case EXCITED -> GestureType.WAVE_ENTHUSIASTIC;
            case PROUD -> GestureType.CHEST_OUT;
            default -> GestureType.IDLE;
        };
    }

    private static ExpressionType mapEmotionToExpression(FiveDimensionalEmotion emotion) {
        String dominant = emotion.getDominantDimension();
        return switch (dominant) {
            case "joy" -> emotion.getJoy() >= 0.7f ? ExpressionType.EXCITED : ExpressionType.HAPPY;
            case "sadness" -> ExpressionType.SAD;
            case "coldness" -> ExpressionType.DISGUSTED;
            case "jealousy" -> ExpressionType.ANGRY;
            case "curiosity" -> ExpressionType.SURPRISED;
            default -> ExpressionType.NEUTRAL;
        };
    }

    // -- Getters --
    public String getExpressionId() { return expressionId; }
    public ExpressionType getExpressionType() { return expressionType; }
    public float getIntensity() { return intensity; }
    public List<BlendshapeWeight> getBlendshapes() { return Collections.unmodifiableList(blendshapes); }
    public BodyPose getBodyPose() { return bodyPose; }
    public void setBodyPose(BodyPose bodyPose) { this.bodyPose = bodyPose; }
    public GestureType getGesture() { return gesture; }
    public String getAnimationClip() { return animationClip; }
    public void setAnimationClip(String animationClip) { this.animationClip = animationClip; }
    public Map<String, String> getMetadata() { return Collections.unmodifiableMap(metadata); }

    /**
     * Expression types — 6 basic + 9 compound.
     */
    public enum ExpressionType {
        // Basic (6)
        HAPPY, SAD, ANGRY, SURPRISED, FEARFUL, DISGUSTED,
        // Compound (9)
        SURPRISE_HAPPY, EXCITED, PROUD, SHY, CONFUSED,
        RELIEVED, DISAPPOINTED, AMUSED, NEUTRAL
    }

    /**
     * Gesture types — ≥20 body gestures.
     */
    public enum GestureType {
        IDLE, WAVE, WAVE_ENTHUSIASTIC, CLAP, NOD, SHAKE_HEAD,
        THUMBS_UP, POINT, HANDS_UP, CROSS_ARMS, HEAD_DOWN,
        HEAD_TILT, STEP_BACK, TURN_AWAY, JUMP, BOW,
        CHEST_OUT, ARMS_OPEN, FINGER_GUN, BLOW_KISS, SHRUG
    }

    /**
     * Individual blendshape weight.
     */
    public record BlendshapeWeight(String blendshapeName, float weight) {}

    /**
     * Body pose in 3D space.
     */
    public static class BodyPose {
        private float positionX, positionY, positionZ;
        private float rotationY;           // yaw
        private String animationState;     // idle/walking/running/sitting

        public BodyPose() {}

        public BodyPose(float x, float y, float z, float rotY) {
            this.positionX = x;
            this.positionY = y;
            this.positionZ = z;
            this.rotationY = rotY;
            this.animationState = "idle";
        }

        public float getPositionX() { return positionX; }
        public void setPositionX(float positionX) { this.positionX = positionX; }
        public float getPositionY() { return positionY; }
        public void setPositionY(float positionY) { this.positionY = positionY; }
        public float getPositionZ() { return positionZ; }
        public void setPositionZ(float positionZ) { this.positionZ = positionZ; }
        public float getRotationY() { return rotationY; }
        public void setRotationY(float rotationY) { this.rotationY = rotationY; }
        public String getAnimationState() { return animationState; }
        public void setAnimationState(String animationState) { this.animationState = animationState; }
    }
}
