package com.solra.grw.domain.service;

import com.solra.grw.domain.model.OnboardingPath;
import com.solra.grw.domain.model.OnboardingStep;
import java.util.Optional;

/**
 * OnboardingEngine — 新用户引导引擎领域服务接口。
 * 管理用户从注册到激活的引导路径，逐步引导完成关键体验。
 */
public interface OnboardingEngine {

    /**
     * 启动引导流程。
     */
    OnboardingPath startOnboarding(String userId);

    /**
     * 推进到下一步。
     */
    OnboardingStep advanceStep(String userId);

    /**
     * 跳过当前步骤。
     */
    OnboardingStep skipStep(String userId);

    /**
     * 完成引导。
     */
    OnboardingPath completeOnboarding(String userId);

    /**
     * 获取下一步推荐动作。
     */
    Optional<OnboardingStep> getNextRecommendedAction(String userId);
}
