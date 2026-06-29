package com.solra.spc.domain.model;

/**
 * SPC-008: 排行榜周期枚举。
 * 日榜(DAILY) / 周榜(WEEKLY) / 月榜(MONTHLY)，均为 T+1 更新。
 */
public enum LeaderboardPeriod {
    /** 日榜 — 前一日数据，每日 00:00 UTC 更新 */
    DAILY,
    /** 周榜 — 前一周数据，每周一 00:00 UTC 更新 */
    WEEKLY,
    /** 月榜 — 前一月数据，每月 1 日 00:00 UTC 更新 */
    MONTHLY;

    /**
     * 返回 T+1 快照标识：前一个完整周期的起始/结束时间。
     */
    public String snapshotLabel() {
        return name();
    }
}
