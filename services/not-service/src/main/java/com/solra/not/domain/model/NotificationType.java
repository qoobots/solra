package com.solra.not.domain.model;

/**
 * 通知类型枚举 — 定义系统支持的所有通知类别。
 * 对齐 proto NotificationType，按社交/创作/系统/商业化分类。
 */
public enum NotificationType {
    // 社交类
    FOLLOW,
    INTERACTION,
    SESSION_INVITE,
    SESSION_JOINED,
    FRIEND_REQUEST,
    SPACE_INVITE,

    // 创作类
    PROJECT_PUBLISHED,
    PROJECT_LIKED,
    ASSET_UPLOADED,

    // 系统类
    SYSTEM_ANNOUNCEMENT,
    ACHIEVEMENT,
    FAITH_LEVEL_UP,
    REVIEW_RESULT,

    // 商业化
    PURCHASE_SUCCESS,
    SUBSCRIPTION_EXPIRY,
    GIFT_RECEIVED
}
