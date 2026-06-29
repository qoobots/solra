package com.solra.grw.infrastructure.engine;

import com.solra.grw.domain.model.*;
import com.solra.grw.domain.repository.OnboardingPathRepository;
import com.solra.grw.domain.service.OnboardingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DefaultOnboardingEngine — 默认新用户引导引擎。
 * 6 步引导流程：欢迎 → 虚拟人介绍 → 空间探索 → 好友推荐 → 分享提示 → 设置通知。
 */
@Component
public class DefaultOnboardingEngine implements OnboardingEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultOnboardingEngine.class);

    /** 预定义引导步骤 */
    private static final List<OnboardingStepType> DEFAULT_STEPS = List.of(
            OnboardingStepType.WELCOME,
            OnboardingStepType.AVATAR_INTRODUCTION,
            OnboardingStepType.SPACE_EXPLORATION,
            OnboardingStepType.FRIEND_SUGGESTION,
            OnboardingStepType.SHARE_PROMPT,
            OnboardingStepType.NOTIFICATION_ENABLE
    );

    private final OnboardingPathRepository onboardingPathRepo;

    public DefaultOnboardingEngine(OnboardingPathRepository onboardingPathRepo) {
        this.onboardingPathRepo = onboardingPathRepo;
    }

    @Override
    public OnboardingPath startOnboarding(String userId) {
        // 检查是否已有引导路径
        Optional<OnboardingPath> existing = onboardingPathRepo.findByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        OnboardingPath path = new OnboardingPath();
        path.setPathId(UUID.randomUUID().toString());
        path.setUserId(userId);
        path.setCurrentStep(0);
        path.setTotalSteps(DEFAULT_STEPS.size());
        path.setStatus(OnboardingStatus.IN_PROGRESS);
        path.setStepHistory(new ArrayList<>());

        OnboardingPath saved = onboardingPathRepo.save(path);
        log.info("GRW-006 onboarding started: user={} path={}", userId, saved.getPathId());
        return saved;
    }

    @Override
    public OnboardingStep advanceStep(String userId) {
        OnboardingPath path = onboardingPathRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No onboarding path for user: " + userId));

        if (path.getCurrentStep() >= path.getTotalSteps()) {
            throw new IllegalStateException("Onboarding already completed for user: " + userId);
        }

        OnboardingStepType stepType = DEFAULT_STEPS.get(path.getCurrentStep());
        OnboardingStep step = new OnboardingStep(path.getCurrentStep() + 1, stepType);
        step.complete();

        List<OnboardingStep> history = path.getStepHistory() != null ? path.getStepHistory() : new ArrayList<>();
        history.add(step);
        path.setStepHistory(history);
        path.setCurrentStep(path.getCurrentStep() + 1);

        if (path.getCurrentStep() >= path.getTotalSteps()) {
            path.setStatus(OnboardingStatus.COMPLETED);
            log.info("GRW-006 onboarding completed: user={}", userId);
        }

        onboardingPathRepo.save(path);
        return step;
    }

    @Override
    public OnboardingStep skipStep(String userId) {
        OnboardingPath path = onboardingPathRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No onboarding path for user: " + userId));

        if (path.getCurrentStep() >= path.getTotalSteps()) {
            throw new IllegalStateException("Onboarding already completed for user: " + userId);
        }

        OnboardingStepType stepType = DEFAULT_STEPS.get(path.getCurrentStep());
        OnboardingStep step = new OnboardingStep(path.getCurrentStep() + 1, stepType);
        step.skip();

        List<OnboardingStep> history = path.getStepHistory() != null ? path.getStepHistory() : new ArrayList<>();
        history.add(step);
        path.setStepHistory(history);
        path.setCurrentStep(path.getCurrentStep() + 1);

        if (path.getCurrentStep() >= path.getTotalSteps()) {
            path.setStatus(OnboardingStatus.COMPLETED);
        }

        onboardingPathRepo.save(path);
        return step;
    }

    @Override
    public OnboardingPath completeOnboarding(String userId) {
        OnboardingPath path = onboardingPathRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No onboarding path for user: " + userId));

        path.setCurrentStep(path.getTotalSteps());
        path.setStatus(OnboardingStatus.COMPLETED);
        OnboardingPath saved = onboardingPathRepo.save(path);
        log.info("GRW-006 onboarding force-completed: user={}", userId);
        return saved;
    }

    @Override
    public Optional<OnboardingStep> getNextRecommendedAction(String userId) {
        return onboardingPathRepo.findByUserId(userId)
                .filter(p -> p.getStatus() == OnboardingStatus.IN_PROGRESS)
                .filter(p -> p.getCurrentStep() < p.getTotalSteps())
                .map(p -> {
                    OnboardingStepType stepType = DEFAULT_STEPS.get(p.getCurrentStep());
                    return new OnboardingStep(p.getCurrentStep() + 1, stepType);
                });
    }
}
