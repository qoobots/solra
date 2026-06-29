package com.solra.grw.infrastructure.engine;

import com.solra.grw.domain.model.*;
import com.solra.grw.domain.repository.ExperienceEventRepository;
import com.solra.grw.domain.repository.UserProfileRepository;
import com.solra.grw.domain.service.ExperienceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * DefaultExperienceService — 等级与存在值系统实现。
 * GRW-001: 用户等级与存在值系统。
 */
@Component
public class DefaultExperienceService implements ExperienceService {

    private static final Logger log = LoggerFactory.getLogger(DefaultExperienceService.class);

    private final UserProfileRepository userProfileRepo;
    private final ExperienceEventRepository experienceEventRepo;

    public DefaultExperienceService(UserProfileRepository userProfileRepo,
                                     ExperienceEventRepository experienceEventRepo) {
        this.userProfileRepo = userProfileRepo;
        this.experienceEventRepo = experienceEventRepo;
    }

    @Override
    public LevelUpResult addExperience(String userId, int amount, String eventType) {
        UserProfile profile = userProfileRepo.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfile p = new UserProfile(userId);
                    return userProfileRepo.save(p);
                });

        // 记录经验事件
        ExperienceEvent event = new ExperienceEvent(UUID.randomUUID().toString(), userId, eventType, amount);
        experienceEventRepo.save(event);

        // 获取当前等级
        int totalExp = experienceEventRepo.sumValueByUserId(userId);
        PresenceLevel oldLevel = PresenceLevel.fromExperience(totalExp - amount);
        PresenceLevel newLevel = PresenceLevel.fromExperience(totalExp);

        boolean leveledUp = oldLevel != newLevel;
        if (leveledUp) {
            log.info("GRW-001 level up: user={} oldLevel={} newLevel={}", userId,
                    oldLevel.getDisplayName(), newLevel.getDisplayName());
            // 更新信誉等级
            profile.updateFaithLevel(FaithLevel.fromScore(newLevel.getLevel()));
        }

        // 更新存在值
        profile.adjustPresenceScore(amount * 0.01);
        profile.recordInteraction();
        userProfileRepo.save(profile);

        double progress = newLevel.progressToNextLevel(totalExp);
        int expToNext = newLevel.experienceToNextLevel(totalExp);

        return new LevelUpResult(userId, oldLevel, newLevel, leveledUp,
                amount, totalExp, expToNext, progress);
    }

    @Override
    public PresenceLevel getLevel(String userId) {
        int totalExp = experienceEventRepo.sumValueByUserId(userId);
        return PresenceLevel.fromExperience(totalExp);
    }

    @Override
    public FaithLevel getFaithLevel(String userId) {
        return userProfileRepo.findByUserId(userId)
                .map(UserProfile::getFaithLevel)
                .orElse(FaithLevel.SEEKER);
    }

    @Override
    public LevelProgress getLevelProgress(String userId) {
        int totalExp = experienceEventRepo.sumValueByUserId(userId);
        PresenceLevel currentLevel = PresenceLevel.fromExperience(totalExp);
        int expToNext = currentLevel.experienceToNextLevel(totalExp);
        double progress = currentLevel.progressToNextLevel(totalExp);

        UserProfile profile = userProfileRepo.findByUserId(userId)
                .orElse(new UserProfile(userId));

        return new LevelProgress(userId, currentLevel, totalExp,
                expToNext, progress, profile.getFaithLevel(), profile.getPresenceScore());
    }

    @Override
    public double calculatePresenceScore(String userId) {
        UserProfile profile = userProfileRepo.findByUserId(userId)
                .orElse(new UserProfile(userId));

        // 多维度加权计算
        double score = 0.0;
        score += profile.getTotalInteractions() * 0.5;
        score += profile.getSpacesVisited() * 2.0;
        score += profile.getConversationsHad() * 3.0;
        score += profile.getFriendsCount() * 5.0;
        score += experienceEventRepo.sumValueByUserId(userId) * 0.1;

        return Math.min(10000, score);
    }
}
