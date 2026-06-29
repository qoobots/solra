package com.solra.grw.application.service;

import com.solra.grw.application.dto.*;
import com.solra.grw.domain.event.GrwDomainEvents;
import com.solra.grw.domain.model.*;
import com.solra.grw.domain.repository.*;
import com.solra.grw.domain.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GrwApplicationService — 用户成长应用层服务。
 * 编排 GRW-001 等级系统、GRW-002 决定性时刻、GRW-003 布道者体系、
 * GRW-004 虚拟人收集、GRW-005 成就系统、GRW-006 新用户引导、
 * GRW-007 召回推送、GRW-008 信仰可视化。
 */
@Service
public class GrwApplicationService {

    private static final Logger log = LoggerFactory.getLogger(GrwApplicationService.class);

    private final UserProfileRepository userProfileRepo;
    private final DecisiveMomentRepository decisiveMomentRepo;
    private final OnboardingPathRepository onboardingPathRepo;
    private final ExperienceEventRepository experienceEventRepo;
    private final RecallTaskRepository recallTaskRepo;
    private final RecallStrategyRepository recallStrategyRepo;
    private final DecisiveMomentDetector decisiveMomentDetector;
    private final OnboardingEngine onboardingEngine;
    private final ReengagementEngine reengagementEngine;
    private final ExperienceService experienceService;
    private final AchievementService achievementService;
    private final EvangelistService evangelistService;
    private final AvatarCollectionService avatarCollectionService;
    private final FaithDashboardService faithDashboardService;

    public GrwApplicationService(UserProfileRepository userProfileRepo,
                                  DecisiveMomentRepository decisiveMomentRepo,
                                  OnboardingPathRepository onboardingPathRepo,
                                  ExperienceEventRepository experienceEventRepo,
                                  RecallTaskRepository recallTaskRepo,
                                  RecallStrategyRepository recallStrategyRepo,
                                  DecisiveMomentDetector decisiveMomentDetector,
                                  OnboardingEngine onboardingEngine,
                                  ReengagementEngine reengagementEngine,
                                  ExperienceService experienceService,
                                  AchievementService achievementService,
                                  EvangelistService evangelistService,
                                  AvatarCollectionService avatarCollectionService,
                                  FaithDashboardService faithDashboardService) {
        this.userProfileRepo = userProfileRepo;
        this.decisiveMomentRepo = decisiveMomentRepo;
        this.onboardingPathRepo = onboardingPathRepo;
        this.experienceEventRepo = experienceEventRepo;
        this.recallTaskRepo = recallTaskRepo;
        this.recallStrategyRepo = recallStrategyRepo;
        this.decisiveMomentDetector = decisiveMomentDetector;
        this.onboardingEngine = onboardingEngine;
        this.reengagementEngine = reengagementEngine;
        this.experienceService = experienceService;
        this.achievementService = achievementService;
        this.evangelistService = evangelistService;
        this.avatarCollectionService = avatarCollectionService;
        this.faithDashboardService = faithDashboardService;
    }

    // ===== GRW-002 决定性时刻 =====

    /** 获取用户画像 */
    public UserProfileResultDTO getUserProfile(String userId) {
        UserProfile profile = userProfileRepo.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfile p = new UserProfile(userId);
                    return userProfileRepo.save(p);
                });
        return new UserProfileResultDTO(
                profile.getUserId(), profile.getPresenceScore(), profile.getFaithLevel(),
                profile.getTotalInteractions(), profile.getSpacesVisited(), profile.getConversationsHad(),
                profile.getFriendsCount(), profile.getLastActiveAt(), profile.isOnboardingCompletedFlag());
    }

    /** 记录用户行为并检测决定性时刻 */
    public List<DecisiveMomentResultDTO> recordUserAction(String userId, String eventType, int value) {
        log.info("GRW-002 recordAction: user={} event={} value={}", userId, eventType, value);

        // 1. 记录经验事件
        ExperienceEvent event = new ExperienceEvent(UUID.randomUUID().toString(), userId, eventType, value);
        experienceEventRepo.save(event);

        // 2. 更新用户画像
        UserProfile profile = userProfileRepo.findByUserId(userId)
                .orElseGet(() -> new UserProfile(userId));
        profile.recordInteraction();
        profile.adjustPresenceScore(value * 0.01);
        userProfileRepo.save(profile);

        // 3. 检测决定性时刻
        Map<String, Object> currentState = new HashMap<>();
        currentState.put("totalInteractions", profile.getTotalInteractions());
        currentState.put("spacesVisited", profile.getSpacesVisited());
        currentState.put("conversationsHad", profile.getConversationsHad());
        currentState.put("friendsCount", profile.getFriendsCount());

        List<DecisiveMoment> moments = decisiveMomentDetector.detectMoments(
                userId, List.of(eventType), currentState);

        List<DecisiveMomentResultDTO> results = new ArrayList<>();
        for (DecisiveMoment dm : moments) {
            if (decisiveMomentDetector.shouldTrigger(dm)) {
                dm.trigger(dm.getConversionValue());
            }
            decisiveMomentRepo.save(dm);
            results.add(new DecisiveMomentResultDTO(
                    dm.getMomentId(), dm.getUserId(), dm.getMomentType().name(),
                    dm.getDetectedAt(), dm.getConversionValue(), dm.getTriggered()));
        }

        log.info("GRW-002 detected {} moments for user={}", results.size(), userId);
        return results;
    }

    /** 获取信誉等级 */
    public FaithLevelResultDTO getFaithLevel(String userId) {
        UserProfile profile = userProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return new FaithLevelResultDTO(userId, profile.getFaithLevel(), profile.getFaithLevel(), false);
    }

    /** 获取经验值 */
    public ExperienceResultDTO getExperience(String userId) {
        int total = experienceEventRepo.sumValueByUserId(userId);
        return new ExperienceResultDTO(null, userId, "TOTAL", total, total, null);
    }

    // ===== GRW-006 新用户引导 =====

    /** 启动引导 */
    public OnboardingPathResultDTO startOnboarding(String userId) {
        OnboardingPath path = onboardingEngine.startOnboarding(userId);
        return toPathDTO(path);
    }

    /** 推进引导步骤 */
    public OnboardingStepResultDTO advanceOnboardingStep(String userId) {
        OnboardingStep step = onboardingEngine.advanceStep(userId);
        return toStepDTO(step);
    }

    /** 跳过引导步骤 */
    public OnboardingStepResultDTO skipOnboardingStep(String userId) {
        OnboardingStep step = onboardingEngine.skipStep(userId);
        return toStepDTO(step);
    }

    /** 完成引导 */
    public OnboardingPathResultDTO completeOnboarding(String userId) {
        OnboardingPath path = onboardingEngine.completeOnboarding(userId);
        return toPathDTO(path);
    }

    /** 获取引导进度 */
    public OnboardingPathResultDTO getOnboardingProgress(String userId) {
        return onboardingPathRepo.findByUserId(userId)
                .map(this::toPathDTO)
                .orElseGet(() -> new OnboardingPathResultDTO(
                        null, userId, 0, 0, "NOT_STARTED", null, null));
    }

    // ---- helpers ----

    private OnboardingPathResultDTO toPathDTO(OnboardingPath p) {
        return new OnboardingPathResultDTO(
                p.getPathId(), p.getUserId(), p.getCurrentStep(),
                p.getTotalSteps(), p.getStatus() != null ? p.getStatus().name() : "UNKNOWN",
                p.getStartTime(), p.getCompletedAt());
    }

    private OnboardingStepResultDTO toStepDTO(OnboardingStep s) {
        return new OnboardingStepResultDTO(
                s.getStepNumber(), s.getStepType() != null ? s.getStepType().name() : "UNKNOWN",
                s.isCompleted(), s.isSkipped());
    }

    // ===== GRW-007 用户召回推送 =====

    /**
     * 评估单个用户的流失风险并生成召回任务。
     */
    public ChurnRiskResultDTO evaluateChurnRisk(String userId, String avatarName) {
        UserProfile profile = userProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        int inactiveDays = (int) ChronoUnit.DAYS.between(profile.getLastActiveAt(), Instant.now());
        ChurnRiskLevel riskLevel = reengagementEngine.evaluateChurnRisk(
                userId, inactiveDays, profile.getTotalInteractions(),
                profile.getFriendsCount(), profile.getPresenceScore());

        // 查找匹配策略
        List<RecallStrategy> strategies = recallStrategyRepo.findByRiskLevel(riskLevel.name());
        RecallStrategy strategy = strategies.stream()
                .filter(RecallStrategy::isActive)
                .findFirst().orElse(null);

        boolean shouldRecall = false;
        if (strategy != null) {
            int previousAttempts = recallTaskRepo.countByUserIdAndStatus(userId, "SENT");
            // 找最近一次召回时间
            List<RecallTask> recentTasks = recallTaskRepo.findRecentByUserId(userId, 720, 1);
            int lastRecallHoursAgo = recentTasks.isEmpty() ? Integer.MAX_VALUE
                    : (int) ChronoUnit.HOURS.between(recentTasks.get(0).getCreatedAt(), Instant.now());
            shouldRecall = reengagementEngine.shouldRecall(riskLevel, previousAttempts,
                    lastRecallHoursAgo, strategy.getCooldownHours(), strategy.getMaxAttempts());
        }

        double churnProbability = Math.min(0.95, inactiveDays / 100.0
                + (riskLevel.ordinal() * 0.1)
                - (profile.getTotalInteractions() * 0.001));

        log.info("GRW-007 churn risk: user={} inactiveDays={} riskLevel={} shouldRecall={}",
                userId, inactiveDays, riskLevel, shouldRecall);

        return new ChurnRiskResultDTO(userId, riskLevel.name(), inactiveDays,
                churnProbability, shouldRecall, Instant.now());
    }

    /**
     * 为指定用户生成并发送召回任务。
     */
    public List<RecallTaskResultDTO> generateRecallTasks(String userId, String avatarName) {
        UserProfile profile = userProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        int inactiveDays = (int) ChronoUnit.DAYS.between(profile.getLastActiveAt(), Instant.now());
        ChurnRiskLevel riskLevel = reengagementEngine.evaluateChurnRisk(
                userId, inactiveDays, profile.getTotalInteractions(),
                profile.getFriendsCount(), profile.getPresenceScore());

        if (riskLevel == ChurnRiskLevel.NONE) {
            log.info("GRW-007 user={} has no churn risk, skipping recall", userId);
            return List.of();
        }

        List<RecallTask> tasks = reengagementEngine.generateRecallTasks(
                userId, riskLevel, inactiveDays, avatarName);

        List<RecallTaskResultDTO> results = new ArrayList<>();
        for (RecallTask task : tasks) {
            task.markSent();
            recallTaskRepo.save(task);
            results.add(toRecallDTO(task));
        }

        log.info("GRW-007 generated {} recall tasks for user={}", results.size(), userId);
        return results;
    }

    /**
     * 处理召回任务回调（用户点击/转化等）。
     */
    public RecallTaskResultDTO handleRecallCallback(String taskId, String action) {
        RecallTask task = recallTaskRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Recall task not found: " + taskId));

        switch (action.toUpperCase()) {
            case "CLICKED":
                task.markClicked();
                break;
            case "CONVERTED":
                task.markConverted();
                // 重新激活用户
                userProfileRepo.findByUserId(task.getUserId()).ifPresent(profile -> {
                    profile.recordInteraction();
                    userProfileRepo.save(profile);
                });
                break;
            case "EXPIRED":
                task.markExpired();
                break;
            case "CANCELLED":
                task.cancel();
                break;
            default:
                throw new IllegalArgumentException("Unknown callback action: " + action);
        }

        task = recallTaskRepo.save(task);
        return toRecallDTO(task);
    }

    /**
     * 获取用户的召回历史。
     */
    public List<RecallTaskResultDTO> getRecallHistory(String userId) {
        return recallTaskRepo.findByUserId(userId).stream()
                .map(this::toRecallDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有活跃召回策略。
     */
    public List<RecallStrategyResultDTO> getRecallStrategies() {
        return recallStrategyRepo.findActive().stream()
                .map(s -> new RecallStrategyResultDTO(
                        s.getStrategyId(), s.getName(), s.getTargetRiskLevel().name(),
                        s.getInactiveDaysMin(), s.getInactiveDaysMax(),
                        s.getChannels().stream().map(Enum::name).collect(Collectors.toList()),
                        s.getMaxAttempts(), s.getCooldownHours(), s.isActive(), s.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * 创建/更新召回策略。
     */
    public RecallStrategyResultDTO saveRecallStrategy(String strategyId, String name,
                                                       String riskLevel, int inactiveDaysMin,
                                                       int inactiveDaysMax,
                                                       String titleTemplate, String messageTemplate,
                                                       List<String> channels) {
        ChurnRiskLevel level = ChurnRiskLevel.valueOf(riskLevel);
        List<RecallChannel> channelList = channels.stream()
                .map(RecallChannel::valueOf).collect(Collectors.toList());

        RecallStrategy strategy = new RecallStrategy(
                strategyId, name, level, inactiveDaysMin, inactiveDaysMax,
                titleTemplate, messageTemplate, channelList);
        strategy = recallStrategyRepo.save(strategy);

        return new RecallStrategyResultDTO(
                strategy.getStrategyId(), strategy.getName(), strategy.getTargetRiskLevel().name(),
                strategy.getInactiveDaysMin(), strategy.getInactiveDaysMax(),
                strategy.getChannels().stream().map(Enum::name).collect(Collectors.toList()),
                strategy.getMaxAttempts(), strategy.getCooldownHours(),
                strategy.isActive(), strategy.getCreatedAt());
    }

    /**
     * 获取召回统计数据。
     */
    public RecallStatsDTO getRecallStats() {
        // 此处为简化实现，实际应使用聚合查询
        List<RecallTask> pendingTasks = recallTaskRepo.findByStatus("SENT", 1000);
        int sent = (int) pendingTasks.stream().filter(t -> t.getStatus() == RecallTaskStatus.SENT).count();
        int clicked = (int) pendingTasks.stream().filter(t -> t.getStatus() == RecallTaskStatus.CLICKED).count();
        int converted = (int) pendingTasks.stream().filter(t -> t.getStatus() == RecallTaskStatus.CONVERTED).count();
        double rate = sent > 0 ? (double) converted / sent : 0.0;

        return new RecallStatsDTO(0, 0, 0, sent, clicked, converted, rate);
    }

    private RecallTaskResultDTO toRecallDTO(RecallTask t) {
        return new RecallTaskResultDTO(
                t.getTaskId(), t.getUserId(), t.getStrategyId(), t.getStrategyName(),
                t.getRiskLevel() != null ? t.getRiskLevel().name() : null,
                t.getInactiveDays(),
                t.getChannel() != null ? t.getChannel().name() : null,
                t.getStatus() != null ? t.getStatus().name() : null,
                t.getTitle(), t.getMessage(), t.getAttemptNumber(),
                t.getCreatedAt(), t.getSentAt(), t.getClickedAt(), t.getConvertedAt());
    }

    // ===== GRW-001 用户等级与存在值系统 =====

    /** 增加经验值 */
    public LevelUpResultDTO addExperience(String userId, int amount, String eventType) {
        log.info("GRW-001 addExperience: user={} amount={} event={}", userId, amount, eventType);
        ExperienceService.LevelUpResult result = experienceService.addExperience(userId, amount, eventType);
        // 同时触发成就检查
        achievementService.checkAndUnlock(userId, eventType,
                experienceEventRepo.sumValueByUserId(userId));
        return LevelUpResultDTO.from(result);
    }

    /** 获取等级进度 */
    public LevelProgressDTO getLevelProgress(String userId) {
        return LevelProgressDTO.from(experienceService.getLevelProgress(userId));
    }

    /** 获取存在值 */
    public double getPresenceScore(String userId) {
        return experienceService.calculatePresenceScore(userId);
    }

    // ===== GRW-005 成就系统 =====

    /** 检查并解锁成就 */
    public List<AchievementUnlockDTO> checkAchievements(String userId, String eventType, int currentProgress) {
        log.info("GRW-005 checkAchievements: user={} event={} progress={}", userId, eventType, currentProgress);
        return achievementService.checkAndUnlock(userId, eventType, currentProgress).stream()
                .map(AchievementUnlockDTO::from).toList();
    }

    /** 获取用户成就状态 */
    public Map<String, Boolean> getAchievementStatus(String userId) {
        return achievementService.getUserAchievementStatus(userId);
    }

    /** 获取已解锁成就 */
    public List<AchievementUnlockDTO> getUnlockedAchievements(String userId) {
        return achievementService.getUnlockedAchievements(userId).stream()
                .map(a -> new AchievementUnlockDTO(a.getAchievementId(), a.getCode(), a.getName(),
                        a.getCategory().name(), a.getRarity().name(),
                        a.getBadgeEffect(), a.getSoundEffect(), a.getExperienceReward(), true))
                .toList();
    }

    /** 获取未解锁成就进度 */
    public List<AchievementProgressDTO> getLockedAchievements(String userId) {
        return achievementService.getLockedAchievements(userId).stream()
                .map(AchievementProgressDTO::from).toList();
    }

    /** 获取成就完成度 */
    public AchievementCompletionDTO getAchievementCompletion(String userId) {
        return AchievementCompletionDTO.from(achievementService.getCompletion(userId));
    }

    /** 获取成就全局统计 */
    public AchievementService.AchievementStats getAchievementGlobalStats() {
        return achievementService.getGlobalStats();
    }

    // ===== GRW-003 布道者体系 =====

    /** 提交布道者申请 */
    public EvangelistDTO applyEvangelist(String userId, String displayName, String bio) {
        log.info("GRW-003 applyEvangelist: user={}", userId);
        Evangelist ev = evangelistService.apply(userId, displayName, bio);
        return EvangelistDTO.from(ev);
    }

    /** 审批布道者申请 */
    public EvangelistDTO reviewEvangelist(String applicationId, boolean approved,
                                           String reviewerId, String comment) {
        log.info("GRW-003 reviewEvangelist: app={} approved={}", applicationId, approved);
        Evangelist ev = evangelistService.review(applicationId, approved, reviewerId, comment);
        return EvangelistDTO.from(ev);
    }

    /** 获取布道者状态 */
    public Optional<EvangelistDTO> getEvangelistStatus(String userId) {
        return evangelistService.getEvangelistStatus(userId).map(EvangelistDTO::from);
    }

    /** 获取活跃布道者列表 */
    public List<EvangelistDTO> getActiveEvangelists(int page, int size) {
        return evangelistService.getActiveEvangelists(page, size).stream()
                .map(EvangelistDTO::from).toList();
    }

    /** 按等级获取布道者 */
    public List<EvangelistDTO> getEvangelistsByTier(String tier, int page, int size) {
        Evangelist.EvangelistTier t = Evangelist.EvangelistTier.valueOf(tier);
        return evangelistService.getEvangelistsByTier(t, page, size).stream()
                .map(EvangelistDTO::from).toList();
    }

    /** 暂停布道者 */
    public void suspendEvangelist(String userId, String reason) {
        evangelistService.suspend(userId, reason);
    }

    /** 撤销布道者 */
    public void revokeEvangelist(String userId, String reason) {
        evangelistService.revoke(userId, reason);
    }

    /** 获取布道者统计 */
    public EvangelistStatsDTO getEvangelistStats() {
        return EvangelistStatsDTO.from(evangelistService.getStats());
    }

    // ===== GRW-004 虚拟人收集与养成 =====

    /** 收集虚拟人 */
    public AvatarEntryDTO collectAvatar(String userId, String avatarTypeId, String name,
                                         String rarity, String element) {
        log.info("GRW-004 collectAvatar: user={} type={}", userId, avatarTypeId);
        AvatarCollection.AvatarRarity r = AvatarCollection.AvatarRarity.valueOf(rarity);
        AvatarCollection.AvatarElement el = AvatarCollection.AvatarElement.valueOf(element);
        AvatarCollection.AvatarEntry entry = avatarCollectionService.collectAvatar(
                userId, avatarTypeId, name, r, el);
        // 触发成就检查
        achievementService.checkAndUnlock(userId, "AVATAR_COLLECT",
                avatarCollectionService.getOrCreateCollection(userId).getUsedSlots());
        return AvatarEntryDTO.from(entry);
    }

    /** 虚拟人养成 */
    public AvatarEntryDTO addAvatarExperience(String userId, String avatarTypeId, int amount) {
        log.info("GRW-004 addAvatarExperience: user={} type={} amount={}", userId, avatarTypeId, amount);
        return AvatarEntryDTO.from(avatarCollectionService.addAvatarExperience(userId, avatarTypeId, amount));
    }

    /** 增加虚拟人好感度 */
    public AvatarEntryDTO addAvatarAffection(String userId, String avatarTypeId, int amount) {
        return AvatarEntryDTO.from(avatarCollectionService.addAvatarAffection(userId, avatarTypeId, amount));
    }

    /** 设置最爱虚拟人 */
    public void setFavoriteAvatar(String userId, String avatarTypeId) {
        avatarCollectionService.setFavoriteAvatar(userId, avatarTypeId);
    }

    /** 获取图鉴进度 */
    public CollectionProgressDTO getCollectionProgress(String userId) {
        return CollectionProgressDTO.from(avatarCollectionService.getCollectionProgress(userId));
    }

    /** 获取用户虚拟人列表 */
    public List<AvatarEntryDTO> getUserAvatars(String userId) {
        return avatarCollectionService.getUserAvatars(userId).stream()
                .map(AvatarEntryDTO::from).toList();
    }

    /** 扩展槽位 */
    public CollectionProgressDTO expandSlots(String userId, int additionalSlots) {
        return CollectionProgressDTO.from(
                avatarCollectionService.getCollectionProgress(userId));
    }

    // ===== GRW-008 信仰体系可视化 =====

    /** 生成信仰仪表盘 */
    public FaithDashboardDTO generateFaithDashboard(String userId) {
        log.info("GRW-008 generateDashboard: user={}", userId);
        return FaithDashboardDTO.from(faithDashboardService.generateDashboard(userId));
    }

    /** 获取信仰仪表盘 */
    public FaithDashboardDTO getFaithDashboard(String userId) {
        return FaithDashboardDTO.from(faithDashboardService.getDashboard(userId));
    }

    /** 刷新信仰仪表盘 */
    public FaithDashboardDTO refreshFaithDashboard(String userId) {
        return FaithDashboardDTO.from(faithDashboardService.refreshDashboard(userId));
    }

    /** 获取全球信仰统计 */
    public GlobalFaithStatsDTO getGlobalFaithStats() {
        return GlobalFaithStatsDTO.from(faithDashboardService.getGlobalStats());
    }
}
