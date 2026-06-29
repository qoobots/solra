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
 * GrwApplicationService — GRW-002/GRW-006/GRW-007 用户成长应用层服务。
 * 编排决定性时刻检测、新用户引导和用户召回推送流程。
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

    public GrwApplicationService(UserProfileRepository userProfileRepo,
                                  DecisiveMomentRepository decisiveMomentRepo,
                                  OnboardingPathRepository onboardingPathRepo,
                                  ExperienceEventRepository experienceEventRepo,
                                  RecallTaskRepository recallTaskRepo,
                                  RecallStrategyRepository recallStrategyRepo,
                                  DecisiveMomentDetector decisiveMomentDetector,
                                  OnboardingEngine onboardingEngine,
                                  ReengagementEngine reengagementEngine) {
        this.userProfileRepo = userProfileRepo;
        this.decisiveMomentRepo = decisiveMomentRepo;
        this.onboardingPathRepo = onboardingPathRepo;
        this.experienceEventRepo = experienceEventRepo;
        this.recallTaskRepo = recallTaskRepo;
        this.recallStrategyRepo = recallStrategyRepo;
        this.decisiveMomentDetector = decisiveMomentDetector;
        this.onboardingEngine = onboardingEngine;
        this.reengagementEngine = reengagementEngine;
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
}
