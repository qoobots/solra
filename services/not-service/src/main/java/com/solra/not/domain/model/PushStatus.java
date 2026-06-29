package com.solra.not.domain.model;

/**
 * 推送消息状态枚举 — 跟踪单条推送的生命周期。
 */
public enum PushStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    OPENED
}
