package com.solra.common.cost;

/**
 * 成本归因维度枚举。
 * INF-009: 按功能/用户/空间维度的成本归因。
 */
public enum CostDimension {
    /** 按功能模块归因（如 auth/avt/spc/llm-router） */
    FUNCTION,
    /** 按用户归因（高级用户/普通用户/访客） */
    USER,
    /** 按空间归因（特定空间ID） */
    SPACE,
    /** 按环境归因（dev/staging/prod） */
    ENVIRONMENT,
    /** 按资源类型归因 */
    RESOURCE_TYPE
}
