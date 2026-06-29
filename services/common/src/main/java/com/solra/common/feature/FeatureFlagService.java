package com.solra.common.feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FeatureFlagService — 功能开关与灰度发布服务。
 * 提供功能开关评估、A/B实验分桶、灰度百分比控制。
 * 内存缓存 + 定时刷新，确保实时生效无需重启。
 * INF-007: 灰度发布与A/B实验平台。
 */
@Service
public class FeatureFlagService {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagService.class);

    /** 功能开关内存缓存（实时生效，无需重启） */
    private final Map<String, FeatureFlag> flagCache = new ConcurrentHashMap<>();

    /** 初始化预置功能开关 */
    public FeatureFlagService() {
        initDefaultFlags();
    }

    private void initDefaultFlags() {
        // 新功能默认关闭的开关
        registerFlag(createBooleanFlag("feature.new-recommendation-engine",
                "新推荐引擎", "基于深度学习的空间推荐算法", false));
        registerFlag(createBooleanFlag("feature.ai-space-generator-v2",
                "AI空间生成V2", "新一代AI辅助空间搭建", false));
        registerFlag(createBooleanFlag("feature.spatial-audio",
                "空间音频引擎", "距离衰减+声源定位的3D音频", false));
        registerFlag(createBooleanFlag("feature.virtual-gifting",
                "虚拟礼物系统", "空间内虚拟物品赠送", false));
        registerFlag(createBooleanFlag("feature.live-streaming",
                "空间直播", "空间内实时直播功能", false));

        // 灰度发布开关
        registerFlag(createPercentageFlag("grayscale.new-ui",
                "新版UI灰度", "全新用户界面设计", 0));
        registerFlag(createPercentageFlag("grayscale.dark-mode",
                "深色模式灰度", "系统级深色主题", 0));
        registerFlag(createPercentageFlag("experiment.avatar-rendering",
                "虚拟人渲染实验", "A/B测试新渲染管线", 0));
        registerFlag(createPercentageFlag("experiment.recommendation-algorithm",
                "推荐算法实验", "A/B测试协同过滤vs深度学习", 0));
    }

    private FeatureFlag createBooleanFlag(String key, String name, String description, boolean defaultValue) {
        FeatureFlag flag = new FeatureFlag();
        flag.setFlagKey(key);
        flag.setName(name);
        flag.setDescription(description);
        flag.setType(FeatureFlag.FlagType.BOOLEAN);
        flag.setEnabled(true);
        flag.setDefaultValue(FeatureFlag.FlagValue.booleanFlag(defaultValue));
        flag.setTags(List.of("feature"));
        return flag;
    }

    private FeatureFlag createPercentageFlag(String key, String name, String description, int percent) {
        FeatureFlag flag = new FeatureFlag();
        flag.setFlagKey(key);
        flag.setName(name);
        flag.setDescription(description);
        flag.setType(FeatureFlag.FlagType.PERCENTAGE);
        flag.setEnabled(true);
        flag.setDefaultValue(FeatureFlag.FlagValue.intFlag(percent));
        flag.setTags(List.of("grayscale"));
        return flag;
    }

    // ===== Public API =====

    /**
     * 检查功能开关是否对指定用户启用。
     *
     * @param flagKey    开关标识
     * @param userId     用户ID（用于百分比分桶）
     * @param deviceType 设备类型（可选）
     * @return 是否启用
     */
    public boolean isEnabled(String flagKey, String userId, String deviceType) {
        FeatureFlag flag = flagCache.get(flagKey);
        if (flag == null) {
            log.warn("Feature flag not found: {}", flagKey);
            return false; // 未知功能默认关闭
        }
        return flag.evaluate(userId, deviceType);
    }

    /**
     * 检查功能开关是否启用（简化版，不需要用户上下文）。
     */
    public boolean isEnabled(String flagKey) {
        return isEnabled(flagKey, "system", null);
    }

    /**
     * 获取 A/B 实验分桶。
     *
     * @param flagKey 实验开关标识
     * @param userId  用户ID
     * @return 分桶标识 (A/B/C...)
     */
    public String getExperimentBucket(String flagKey, String userId) {
        FeatureFlag flag = flagCache.get(flagKey);
        if (flag == null || !flag.isEnabled()) return "control";

        int hash = Math.abs(userId.hashCode()) % 100;
        int percentage = flag.getDefaultValue() != null ? flag.getDefaultValue().getIntValue() : 0;

        if (hash < percentage / 2) return "A";
        if (hash < percentage) return "B";
        return "control";
    }

    /**
     * 获取所有功能开关列表。
     */
    public Collection<FeatureFlag> getAllFlags() {
        return Collections.unmodifiableCollection(flagCache.values());
    }

    /**
     * 注册功能开关。
     */
    public void registerFlag(FeatureFlag flag) {
        flagCache.put(flag.getFlagKey(), flag);
        log.info("Feature flag registered: {} (type={}, enabled={})",
                flag.getFlagKey(), flag.getType(), flag.isEnabled());
    }

    /**
     * 更新功能开关。
     */
    public void updateFlag(String flagKey, boolean enabled) {
        FeatureFlag flag = flagCache.get(flagKey);
        if (flag != null) {
            if (enabled) flag.enable(); else flag.disable();
            log.info("Feature flag updated: {} enabled={}", flagKey, enabled);
        }
    }

    /**
     * 更新灰度百分比。
     */
    public void updatePercentage(String flagKey, int percent) {
        FeatureFlag flag = flagCache.get(flagKey);
        if (flag != null && flag.getType() == FeatureFlag.FlagType.PERCENTAGE) {
            flag.updatePercentage(percent);
            log.info("Feature flag percentage updated: {} percent={}%", flagKey, percent);
        }
    }

    /**
     * 删除功能开关。
     */
    public void removeFlag(String flagKey) {
        flagCache.remove(flagKey);
        log.info("Feature flag removed: {}", flagKey);
    }

    /**
     * 定时刷新开关状态（可对接远程配置中心）。
     * 默认每分钟检查一次。
     */
    @Scheduled(fixedDelay = 60000)
    public void refreshFlags() {
        log.debug("Feature flag refresh triggered, {} flags in cache", flagCache.size());
        // TODO: 对接远程配置中心（LaunchDarkly/自研Config Center）
    }
}
