package com.solra.grw.domain.model;

/**
 * OnboardingStepType 枚举 — 新用户引导步骤类型。
 * 定义引导流程中的各个关键步骤。
 */
public enum OnboardingStepType {
    /** 欢迎页面 */
    WELCOME,

    /** 虚拟人介绍与互动 */
    AVATAR_INTRODUCTION,

    /** 空间探索引导 */
    SPACE_EXPLORATION,

    /** 好友推荐 */
    FRIEND_SUGGESTION,

    /** 分享提示 */
    SHARE_PROMPT,

    /** 个人资料设置 */
    PROFILE_SETUP,

    /** 通知权限开启引导 */
    NOTIFICATION_ENABLE;
}
