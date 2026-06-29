package com.solra.grw.domain.service;

import com.solra.grw.domain.model.AvatarCollection;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AvatarCollectionService — 虚拟人收集与养成领域服务接口。
 * GRW-004: ≥10个基础虚拟人，图鉴系统。
 */
public interface AvatarCollectionService {

    /**
     * 获取或创建用户的虚拟人图鉴。
     */
    AvatarCollection getOrCreateCollection(String userId);

    /**
     * 收集一个新虚拟人。
     */
    AvatarCollection.AvatarEntry collectAvatar(String userId, String avatarTypeId, String name,
                                                AvatarCollection.AvatarRarity rarity,
                                                AvatarCollection.AvatarElement element);

    /**
     * 给虚拟人增加经验值（养成）。
     */
    AvatarCollection.AvatarEntry addAvatarExperience(String userId, String avatarTypeId, int amount);

    /**
     * 增加虚拟人好感度。
     */
    AvatarCollection.AvatarEntry addAvatarAffection(String userId, String avatarTypeId, int amount);

    /**
     * 设置最爱虚拟人。
     */
    void setFavoriteAvatar(String userId, String avatarTypeId);

    /**
     * 获取用户的图鉴完成度。
     */
    CollectionProgress getCollectionProgress(String userId);

    /**
     * 获取用户的虚拟人列表。
     */
    List<AvatarCollection.AvatarEntry> getUserAvatars(String userId);

    /**
     * 获取最高好感度的虚拟人。
     */
    Optional<AvatarCollection.AvatarEntry> getHighestAffectionAvatar(String userId);

    /**
     * 获取最高等级的虚拟人。
     */
    Optional<AvatarCollection.AvatarEntry> getHighestLevelAvatar(String userId);

    /**
     * 扩展槽位。
     */
    AvatarCollection expandSlots(String userId, int additionalSlots);

    /** 图鉴进度 */
    record CollectionProgress(String userId, int collected, int totalAvailable,
                               double completionPercent,
                               Map<AvatarCollection.AvatarRarity, Long> rarityDistribution,
                               Map<AvatarCollection.AvatarElement, Long> elementDistribution,
                               int totalSlots, int usedSlots) {}
}
