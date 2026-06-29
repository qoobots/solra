package com.solra.grw.domain.model;

/**
 * FaithLevel 枚举 — 用户信誉等级体系。
 * 代表用户在 Solra 平台中的成长阶段。
 */
public enum FaithLevel {
    /**
     * 探索者 (Lv1-10)：新注册用户，正在了解平台。
     */
    SEEKER,

    /**
     * 信徒 (Lv11-30)：已参与基本互动，开始信任平台。
     */
    BELIEVER,

    /**
     * 虔诚者 (Lv31-60)：深度使用用户，对平台有较强归属感。
     */
    DISCIPLE,

    /**
     * 布道者 (Lv61-100)：核心用户，主动传播平台价值。
     */
    EVANGELIST;

    /**
     * 根据信誉等级数值获取对应的 FaithLevel。
     *
     * @param score 信誉等级数值 (1-100)
     * @return 对应的 FaithLevel 枚举值
     */
    public static FaithLevel fromScore(int score) {
        if (score <= 10) return SEEKER;
        if (score <= 30) return BELIEVER;
        if (score <= 60) return DISCIPLE;
        return EVANGELIST;
    }
}
