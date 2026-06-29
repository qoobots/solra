package com.solra.soc.domain.model;

/**
 * 好友关系状态枚举。
 * <p>
 * PENDING: 好友请求待确认
 * ACCEPTED: 已接受，成为好友
 * BLOCKED: 已拉黑
 */
public enum FriendStatus {
    PENDING, ACCEPTED, BLOCKED
}
