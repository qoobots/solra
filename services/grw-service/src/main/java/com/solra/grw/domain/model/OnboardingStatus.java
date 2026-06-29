package com.solra.grw.domain.model;

/**
 * OnboardingStatus 枚举 — 引导流程状态。
 */
public enum OnboardingStatus {
    /** 尚未开始 */
    NOT_STARTED,

    /** 进行中 */
    IN_PROGRESS,

    /** 已完成 */
    COMPLETED,

    /** 已放弃 */
    ABANDONED;
}
