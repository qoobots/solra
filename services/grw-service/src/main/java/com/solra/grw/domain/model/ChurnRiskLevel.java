package com.solra.grw.domain.model;

/**
 * 用户流失风险等级。
 * 用于召回策略引擎判断是否需要对用户进行召回。
 */
public enum ChurnRiskLevel {
    /** 无风险 — 7天内活跃 */
    NONE,
    /** 低风险 — 7-14天未活跃 */
    LOW,
    /** 中风险 — 14-30天未活跃 */
    MEDIUM,
    /** 高风险 — 30-60天未活跃 */
    HIGH,
    /** 极高风险 — 60天以上未活跃 */
    CRITICAL,
    /** 已流失 — 90天以上未活跃 */
    CHURNED
}
