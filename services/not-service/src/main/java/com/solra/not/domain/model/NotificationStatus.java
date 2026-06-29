package com.solra.not.domain.model;

/**
 * 通知状态枚举 — 跟踪通知的生命周期。
 * 对齐 proto NotificationStatus，同时兼容内部状态机。
 */
public enum NotificationStatus {
    /** 待发送 */
    PENDING,
    /** 已发送 */
    SENT,
    /** 已送达 */
    DELIVERED,
    /** 已读 */
    READ,
    /** 已存档 */
    ARCHIVED,
    /** 已过期 */
    EXPIRED,
    /** 已关闭（用户手动关闭） */
    DISMISSED
}
