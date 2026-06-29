package com.solra.avt.domain.model;

/**
 * GreetingTrigger 枚举 — AVT-002 社交主动性触发类型。
 * 根据在场事件类型决定虚拟人的主动互动策略。
 */
public enum GreetingTrigger {

    /** 新用户首次进入：热情欢迎 */
    FIRST_TIME_WELCOME("first_time_welcome", "Hi~ 欢迎来到索拉! 我是{avatar_name}，让我带你逛逛吧 ✨", 0.9f),

    /** 回访用户：个性化招呼 */
    RETURNING_GREETING("returning_greeting", "你回来啦 {user_name}! 好久不见～", 0.7f),

    /** 用户靠近：微互动 */
    APPROACH_REACTION("approach_reaction", "(微笑点头) 你好呀 👋", 0.5f),

    /** 用户注视：回应眼神经 */
    GAZE_RESPONSE("gaze_response", "(好奇地歪头) 在看什么呢？", 0.4f),

    /** 用户逗留：主动搭话 */
    LINGERING_PROMPT("lingering_prompt", "要不要一起聊聊天？我今天有很多新鲜事想告诉你~", 0.6f),

    /** 好友在场：社交催化 */
    FRIEND_PRESENCE("friend_presence", "你和{friend_name}都在这儿呀，太巧了!", 0.5f),

    /** 多次回访用户：深度互动引导 */
    ENGAGED_USER_PROMPT("engaged_user_prompt", "感觉你越来越熟悉这里了~要不要试试{feature_hint}？", 0.65f);

    private final String code;
    private final String template;
    private final float enthusiasm; // 0.0-1.0 互动热情度

    GreetingTrigger(String code, String template, float enthusiasm) {
        this.code = code;
        this.template = template;
        this.enthusiasm = enthusiasm;
    }

    /** 选择最佳招呼策略 */
    public static GreetingTrigger select(PresenceEvent event) {
        var ctx = event.getUserContext();
        if (ctx == null) return APPROACH_REACTION;

        // 新用户
        if (ctx.isNewUser()) return FIRST_TIME_WELCOME;

        // 距上次访问>7天 → 深度回访
        if (ctx.getTimeSinceLastVisit() > 604800) return RETURNING_GREETING;

        // 根据事件类型
        return switch (event.getEventType()) {
            case USER_ENTERED -> RETURNING_GREETING;
            case USER_APPROACHED -> APPROACH_REACTION;
            case USER_GAZING -> GAZE_RESPONSE;
            case USER_LINGERING -> (ctx.getTotalVisits() > 5)
                    ? ENGAGED_USER_PROMPT : LINGERING_PROMPT;
            default -> APPROACH_REACTION;
        };
    }

    // -- getters --
    public String getCode() { return code; }
    public String getTemplate() { return template; }
    public float getEnthusiasm() { return enthusiasm; }
}
