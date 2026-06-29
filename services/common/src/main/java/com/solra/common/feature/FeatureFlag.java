package com.solra.common.feature;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * FeatureFlag — 功能开关值对象。
 * 支持 Boolean/Percentage/User/Device 四种开关类型。
 * INF-007: 灰度发布与A/B实验平台。
 */
public class FeatureFlag {

    private String flagKey;
    private String name;
    private String description;
    private FlagType type;
    private boolean enabled;
    private FlagValue defaultValue;
    private Map<String, FlagValue> targetingRules; // 按 userId% 分桶的规则
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
    private String owner;

    public enum FlagType {
        /** 布尔开关 — 全量开启/关闭 */
        BOOLEAN,
        /** 百分比灰度 — 按 userId hash% 分桶 */
        PERCENTAGE,
        /** 用户白名单 — 指定用户启用 */
        USER_WHITELIST,
        /** 设备特征 — 按设备型号/OS版本启用 */
        DEVICE
    }

    public static class FlagValue {
        private boolean booleanValue;
        private String stringValue;
        private int intValue;
        private double doubleValue;

        public static FlagValue booleanFlag(boolean value) {
            FlagValue fv = new FlagValue();
            fv.booleanValue = value;
            return fv;
        }

        public static FlagValue stringFlag(String value) {
            FlagValue fv = new FlagValue();
            fv.stringValue = value;
            return fv;
        }

        public static FlagValue intFlag(int value) {
            FlagValue fv = new FlagValue();
            fv.intValue = value;
            return fv;
        }

        public static FlagValue doubleFlag(double value) {
            FlagValue fv = new FlagValue();
            fv.doubleValue = value;
            return fv;
        }

        public boolean getBooleanValue() { return booleanValue; }
        public void setBooleanValue(boolean booleanValue) { this.booleanValue = booleanValue; }
        public String getStringValue() { return stringValue; }
        public void setStringValue(String stringValue) { this.stringValue = stringValue; }
        public int getIntValue() { return intValue; }
        public void setIntValue(int intValue) { this.intValue = intValue; }
        public double getDoubleValue() { return doubleValue; }
        public void setDoubleValue(double doubleValue) { this.doubleValue = doubleValue; }
    }

    public FeatureFlag() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * 评估功能开关是否对指定用户启用。
     *
     * @param userId     用户ID
     * @param deviceType 设备类型（可选）
     * @return 如果该功能对用户启用返回 true
     */
    public boolean evaluate(String userId, String deviceType) {
        if (!enabled) return false;

        switch (type) {
            case BOOLEAN:
                return defaultValue != null && defaultValue.booleanValue;

            case PERCENTAGE:
                if (defaultValue == null) return false;
                int bucket = Math.abs(userId.hashCode()) % 100;
                return bucket < defaultValue.intValue;

            case USER_WHITELIST:
                if (targetingRules == null) return false;
                return targetingRules.containsKey(userId);

            case DEVICE:
                if (deviceType == null || targetingRules == null) return false;
                return targetingRules.containsKey(deviceType);

            default:
                return false;
        }
    }

    /** 启用开关 */
    public void enable() { this.enabled = true; this.updatedAt = Instant.now(); }
    /** 禁用开关 */
    public void disable() { this.enabled = false; this.updatedAt = Instant.now(); }
    /** 更新灰度百分比 */
    public void updatePercentage(int percent) {
        if (type == FlagType.PERCENTAGE) {
            this.defaultValue = FlagValue.intFlag(Math.min(100, Math.max(0, percent)));
            this.updatedAt = Instant.now();
        }
    }

    // ---- getters/setters ----

    public String getFlagKey() { return flagKey; }
    public void setFlagKey(String flagKey) { this.flagKey = flagKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public FlagType getType() { return type; }
    public void setType(FlagType type) { this.type = type; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public FlagValue getDefaultValue() { return defaultValue; }
    public void setDefaultValue(FlagValue defaultValue) { this.defaultValue = defaultValue; }
    public Map<String, FlagValue> getTargetingRules() { return targetingRules; }
    public void setTargetingRules(Map<String, FlagValue> targetingRules) { this.targetingRules = targetingRules; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
}
