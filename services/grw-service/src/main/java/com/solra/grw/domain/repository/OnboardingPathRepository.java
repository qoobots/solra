package com.solra.grw.domain.repository;

import com.solra.grw.domain.model.OnboardingPath;
import java.util.Optional;

/** 引导路径仓储接口 */
public interface OnboardingPathRepository {
    OnboardingPath save(OnboardingPath path);
    Optional<OnboardingPath> findByUserId(String userId);
}
