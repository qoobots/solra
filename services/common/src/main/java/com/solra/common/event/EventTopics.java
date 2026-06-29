package com.solra.common.event;

/**
 * EventTopics — Kafka 事件主题常量定义。
 * 按限界上下文组织，统一管理所有跨服务事件通道。
 * INF-004: 消息队列事件驱动架构底座。
 */
public final class EventTopics {

    private EventTopics() {}

    // ===== 空间域事件 =====
    /** 用户进入空间 */
    public static final String SPACE_ENTERED = "solra.space.entered";
    /** 用户离开空间 */
    public static final String SPACE_LEFT = "solra.space.left";
    /** 空间已发布 */
    public static final String SPACE_PUBLISHED = "solra.space.published";

    // ===== 虚拟人域事件 =====
    /** 虚拟人状态变化 */
    public static final String AVATAR_STATE_CHANGED = "solra.avatar.state_changed";
    /** 对话消息 */
    public static final String CONVERSATION_MESSAGE = "solra.avatar.conversation_message";
    /** 虚拟人打招呼 */
    public static final String AVATAR_GREETING = "solra.avatar.greeting";
    /** 惊喜事件触发 */
    public static final String AVATAR_SURPRISE = "solra.avatar.surprise";

    // ===== 社交域事件 =====
    /** 用户在场状态变化 */
    public static final String USER_PRESENCE_CHANGED = "solra.social.presence_changed";
    /** 社交互动 */
    public static final String SOCIAL_INTERACTION = "solra.social.interaction";
    /** 好友请求 */
    public static final String FRIEND_REQUEST = "solra.social.friend_request";
    /** 空间分享 */
    public static final String SPACE_SHARED = "solra.social.space_shared";

    // ===== 成长域事件 =====
    /** 成就解锁 */
    public static final String ACHIEVEMENT_UNLOCKED = "solra.growth.achievement_unlocked";
    /** 信仰等级变化 */
    public static final String FAITH_LEVEL_CHANGED = "solra.growth.faith_level_changed";
    /** 决定性时刻检测 */
    public static final String DECISIVE_MOMENT = "solra.growth.decisive_moment";
    /** 用户被召回 */
    public static final String USER_REENGAGED = "solra.growth.user_reengaged";

    // ===== 通知域事件 =====
    /** 通知已发送 */
    public static final String NOTIFICATION_SENT = "solra.notification.sent";
    /** 推送已送达 */
    public static final String PUSH_DELIVERED = "solra.notification.push_delivered";
    /** 收件箱消息 */
    public static final String INBOX_MESSAGE = "solra.notification.inbox_message";

    // ===== 安全域事件 =====
    /** 内容被标记 */
    public static final String CONTENT_FLAGGED = "solra.safety.content_flagged";
    /** 审核完成 */
    public static final String REVIEW_COMPLETED = "solra.safety.review_completed";

    // ===== 系统域事件 =====
    /** Feature Flag 变更 */
    public static final String FEATURE_FLAG_CHANGED = "solra.system.feature_flag_changed";
}
