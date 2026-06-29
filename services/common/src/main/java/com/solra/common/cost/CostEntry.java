package com.solra.common.cost;

import java.time.Instant;
import java.util.Map;

/**
 * 单条成本记录值对象。
 * 记录某一时间点某资源在某维度下的成本数据。
 */
public class CostEntry {

    private final String entryId;
    private final CostResourceType resourceType;
    private final String resourceId;
    private final double amount;          // 金额（CNY分，整数避免浮点精度问题）
    private final String currency;        // 币种
    private final Instant timestamp;
    private final Map<CostDimension, String> dimensions; // 多维度归因标签
    private final String description;

    private CostEntry(Builder builder) {
        this.entryId = builder.entryId;
        this.resourceType = builder.resourceType;
        this.resourceId = builder.resourceId;
        this.amount = builder.amount;
        this.currency = builder.currency != null ? builder.currency : "CNY";
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.dimensions = Map.copyOf(builder.dimensions);
        this.description = builder.description;
    }

    // ---- Getters ----
    public String getEntryId() { return entryId; }
    public CostResourceType getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
    public double getAmount() { return amount; }
    public double getAmountYuan() { return amount / 100.0; }
    public String getCurrency() { return currency; }
    public Instant getTimestamp() { return timestamp; }
    public Map<CostDimension, String> getDimensions() { return dimensions; }
    public String getDescription() { return description; }

    /** 获取指定维度的归因值 */
    public String getDimensionValue(CostDimension dim) {
        return dimensions.getOrDefault(dim, "unknown");
    }

    /** 是否匹配指定维度的归因值 */
    public boolean matchesDimension(CostDimension dim, String value) {
        return value.equals(dimensions.get(dim));
    }

    // ---- Builder ----
    public static class Builder {
        private String entryId;
        private CostResourceType resourceType;
        private String resourceId;
        private double amount;
        private String currency = "CNY";
        private Instant timestamp = Instant.now();
        private Map<CostDimension, String> dimensions = Map.of();
        private String description = "";

        public Builder entryId(String entryId) { this.entryId = entryId; return this; }
        public Builder resourceType(CostResourceType type) { this.resourceType = type; return this; }
        public Builder resourceId(String resourceId) { this.resourceId = resourceId; return this; }
        public Builder amount(double amount) { this.amount = amount; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder dimensions(Map<CostDimension, String> dimensions) { this.dimensions = dimensions; return this; }
        public Builder description(String description) { this.description = description; return this; }

        public CostEntry build() {
            if (resourceType == null) throw new IllegalArgumentException("resourceType is required");
            if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
            return new CostEntry(this);
        }
    }

    public static Builder builder() { return new Builder(); }
}
