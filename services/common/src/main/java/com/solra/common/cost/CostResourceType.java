package com.solra.common.cost;

/**
 * 成本资源类型枚举。
 * 对应 infra/finops/cost-management.yaml 中的预算模块划分。
 */
public enum CostResourceType {
    /** 计算资源：K8s集群 + Bare Metal + GPU实例 */
    COMPUTE("compute", "计算资源"),
    /** 存储资源：S3/OSS + CDN + DB存储 */
    STORAGE("storage", "存储资源"),
    /** 网络资源：CDN流量 + NAT + Load Balancer */
    NETWORK("network", "网络资源"),
    /** API调用：LLM API + TTS + 第三方API */
    API("apis", "API调用"),
    /** 可观测性：日志/监控/追踪/告警 */
    OBSERVABILITY("observability", "可观测性"),
    /** 杂项：域名/DNS/SSL + 其他 */
    MISCELLANEOUS("miscellaneous", "杂项");

    private final String configKey;
    private final String displayName;

    CostResourceType(String configKey, String displayName) {
        this.configKey = configKey;
        this.displayName = displayName;
    }

    public String getConfigKey() { return configKey; }
    public String getDisplayName() { return displayName; }
}
