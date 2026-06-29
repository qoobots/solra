package com.solra.soc.domain.model;

/**
 * 分享状态枚举。
 * <p>
 * ACTIVE: 分享链接有效
 * EXPIRED: 分享链接已过期
 * CONSUMED: 分享已被消费（如邀请已被接受）
 */
public enum ShareStatus {
    ACTIVE, EXPIRED, CONSUMED
}
