package com.solra.grw.application.service;

import com.solra.grw.application.dto.*;
import com.solra.grw.domain.event.GrwDomainEvents;
import com.solra.grw.domain.model.*;
import com.solra.grw.domain.repository.*;
import com.solra.grw.domain.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GrwApplicationService — GRW-002/GRW-006 用户成长应用层服务。
 * 编排决定性时刻检测和新用户引导流程。
 */
@Service
public class GrwApplicationService {

    private static final Logger log = LoggerFactory.getLogger(GrwApplicationService.class);

    private final UserProfileRepository userProfileRepo;
    private final DecisiveMomentRepository decisiveMomentRepo;
    private final OnboardingPathRepository onboardingPathRepo;
    private final ExperienceEventRepository experienceEventRepo;
    private final DecisiveMomentDetector decisiveMomentDetector;
    private final OnboardingEngine onboardingEngine;

    public GrwApplicationService(UserProfileRepository userProfileRepo,
                                  DecisiveMomentRepository decisiveMomentRepo,
                                  OnboardingPathRepository onboardingPathRepo,
                                  ExperienceEventRepository experienceEventRepo,
                                  DecisiveMomentDetector decisiveMomentDetector,
                                  OnboardingEngine onboardingEngine) {
        this.userProfileRepo = userProfileRepo;
        this.decisiveMomentRepo = decisiveMomentRepo;
        this.onboardingPathRepo = onboardingPathRepo;
        this.experienceEventRepo = experienceEventRepo;
        this.decisiveMomentDetector = decisiveMomentDetector;
        this.onboardingEngine = onboardingEngine;
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
}
