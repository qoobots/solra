package com.solra.grw.domain.model;

/**
 * 召回任务状态。
 */
public enum RecallTaskStatus {
    /** 待发送 */
    PENDING,
    /** 已发送 */
    SENT,
    /** 用户已点击 */
    CLICKED,
    /** 用户已转化（重新活跃） */
    CONVERTED,
    /** 已过期未转化 */
    EXPIRED,
    /** 已取消 */
    CANCELLED
}
