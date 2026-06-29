package com.solra.grw.infrastructure.engine;

import com.solra.grw.domain.model.*;
import com.solra.grw.domain.repository.ExperienceEventRepository;
import com.solra.grw.domain.repository.UserProfileRepository;
import com.solra.grw.domain.service.AchievementService;
import com.solra.grw.domain.service.AvatarCollectionService;
import com.solra.grw.domain.service.ExperienceService;
import com.solra.grw.domain.service.FaithDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * DefaultFaithDashboardService — 信仰体系可视化实现。
 * GRW-008: 展示用户"存在深度"的多维度证据。
 */
@Component
public class DefaultFaithDashboardService implements FaithDashboardService {

    private static final Logger log = LoggerFactory.getLogger(DefaultFaithDashboardService.class);

    private final UserProfileRepository userProfileRepo;
    private final ExperienceEventRepository experienceEventRepo;
    private final ExperienceService experienceService;
    private final AchievementService achievementService;
    private final AvatarCollectionService avatarCollectionService;

    /** 仪表盘缓存 (userId -> dashboard) */
    private final Map<String, FaithDashboard> dashboardCache = new LinkedHashMap<>();

    public DefaultFaithDashboardService(UserProfileRepository userProfileRepo,
                                         ExperienceEventRepository experienceEventRepo,
                                         ExperienceService experienceService,
                                         AchievementService achievementService,
                                         AvatarCollectionService avatarCollectionService) {
        this.userProfileRepo = userProfileRepo;
        this.experienceEventRepo = experienceEventRepo;
        this.experienceService = experienceService;
        this.achievementService = achievementService;
        this.avatarCollectionService = avatarCollectionService;
    }

    @Override
    public FaithDashboard generateDashboard(String userId) {
        UserProfile profile = userProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        FaithDashboard dashboard = new FaithDashboard(userId);

        // 计算各维度度量
        int totalExp = experienceEventRepo.sumValueByUserId(userId);

        // 时间投入
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.TIME_INVESTED,
                profile.getTotalInteractions() * 0.5,  // 简化估算
                calculatePercentile(profile.getTotalInteractions(), 1000), 0.0));

        // 空间足迹
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.SPACES_EXPLORED,
                profile.getSpacesVisited(),
                calculatePercentile(profile.getSpacesVisited(), 200), 0.0));

        // 对话深度
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.CONVERSATIONS_HAD,
                profile.getConversationsHad(),
                calculatePercentile(profile.getConversationsHad(), 500), 0.0));

        // 社交联结
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.FRIENDS_MADE,
                profile.getFriendsCount(),
                calculatePercentile(profile.getFriendsCount(), 100), 0.0));

        // 创造贡献
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.CREATIONS_BUILT,
                profile.getSpacesVisited(),  // 用空间访问数近似
                calculatePercentile(profile.getSpacesVisited(), 100), 0.0));

        // 分享传播
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.SHARES_SPREAD,
                profile.getTotalInteractions() * 0.1,
                calculatePercentile(profile.getTotalInteractions() / 10, 100), 0.0));

        // 成就解锁
        int unlockedAchievements = achievementService.getUnlockedAchievements(userId).size();
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.ACHIEVEMENTS_UNLOCKED,
                unlockedAchievements,
                calculatePercentile(unlockedAchievements, 35), 0.0));

        // 虚拟人羁绊
        AvatarCollectionService.CollectionProgress colProgress =
                avatarCollectionService.getCollectionProgress(userId);
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.AVATARS_COLLECTED,
                colProgress.collected(),
                calculatePercentile(colProgress.collected(), 12), 0.0));

        // 事件参与
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.EVENTS_PARTICIPATED,
                profile.getTotalInteractions(),
                calculatePercentile(profile.getTotalInteractions(), 1000), 0.0));

        // 连续活跃
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.CONSECUTIVE_DAYS,
                1,  // 简化实现
                calculatePercentile(1, 365), 0.0));

        // 添加里程碑
        addMilestones(dashboard, profile, unlockedAchievements);

        // 计算综合分数
        dashboard.calculateOverallFaithScore();
        dashboard.setFaithLevel(profile.getFaithLevel());
        dashboard.setPresenceLevel(experienceService.getLevel(userId));
        dashboard.generateNarrative();

        dashboardCache.put(userId, dashboard);
        log.info("GRW-008 faith dashboard generated for user={} score={}",
                userId, dashboard.getOverallFaithScore());

        return dashboard;
    }

    private void addMilestones(FaithDashboard dashboard, UserProfile profile, int unlockedAchievements) {
        if (profile.getSpacesVisited() >= 1) {
            dashboard.addMilestone(new FaithDashboard.Milestone(
                    "MS-" + profile.getUserId() + "-first-space",
                    "首次探索", "你踏出了在索拉的第一步",
                    profile.getCreatedAt()));
        }
        if (profile.getConversationsHad() >= 1) {
            dashboard.addMilestone(new FaithDashboard.Milestone(
                    "MS-" + profile.getUserId() + "-first-conv",
                    "初次对话", "你与虚拟人完成了第一次对话",
                    profile.getUpdatedAt()));
        }
        if (profile.getFriendsCount() >= 1) {
            dashboard.addMilestone(new FaithDashboard.Milestone(
                    "MS-" + profile.getUserId() + "-first-friend",
                    "第一个朋友", "你在索拉交到了第一个朋友",
                    profile.getUpdatedAt()));
        }
        if (unlockedAchievements >= 5) {
            FaithDashboard.Milestone m = new FaithDashboard.Milestone(
                    "MS-" + profile.getUserId() + "-5-achievements",
                    "成就猎人", "你已经解锁了5个成就",
                    Instant.now());
            m.setHighlight(true);
            dashboard.addMilestone(m);
        }
        if (unlockedAchievements >= 10) {
            FaithDashboard.Milestone m = new FaithDashboard.Milestone(
                    "MS-" + profile.getUserId() + "-10-achievements",
                    "十全十美", "你已经解锁了10个成就",
                    Instant.now());
            m.setHighlight(true);
            dashboard.addMilestone(m);
        }
        if (profile.getTotalInteractions() >= 100) {
            FaithDashboard.Milestone m = new FaithDashboard.Milestone(
                    "MS-" + profile.getUserId() + "-100-interactions",
                    "百次互动", "你已在索拉互动超过100次",
                    Instant.now());
            m.setHighlight(true);
            dashboard.addMilestone(m);
        }
    }

    private double calculatePercentile(double value, double max) {
        if (max <= 0) return 0.0;
        return Math.min(100, (value / max) * 100);
    }

    @Override
    public FaithDashboard getDashboard(String userId) {
        FaithDashboard cached = dashboardCache.get(userId);
        if (cached != null) return cached;
        return generateDashboard(userId);
    }

    @Override
    public FaithDashboard refreshDashboard(String userId) {
        dashboardCache.remove(userId);
        return generateDashboard(userId);
    }

    @Override
    public double getPercentile(String userId, FaithDashboard.DepthDimension dimension) {
        FaithDashboard dashboard = getDashboard(userId);
        return dashboard.getDimensions().values().stream()
                .filter(m -> m.getDimension() == dimension)
                .mapToDouble(FaithDashboard.DimensionMetric::getPercentile)
                .findFirst().orElse(0.0);
    }

    @Override
    public GlobalFaithStats getGlobalStats() {
        long totalUsers = userProfileRepo.count();
        // 简化实现
        return new GlobalFaithStats(totalUsers, 250.0, 1500.0, 10000,
                FaithDashboard.DepthDimension.CONVERSATIONS_HAD);
    }
}
