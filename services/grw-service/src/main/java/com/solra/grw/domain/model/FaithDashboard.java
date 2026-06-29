package com.solra.grw.domain.model;

import java.time.Instant;
import java.util.*;

/**
 * FaithDashboard 视图模型 — 信仰体系可视化。
 * GRW-008: 展示用户"存在深度"的多维度证据。
 * "你属于索拉的证据" — 数据驱动的用户身份认同。
 */
public class FaithDashboard {

    /**
     * 存在深度维度 — 组成信仰体系的各维度。
     */
    public enum DepthDimension {
        TIME_INVESTED("时间投入", "累计在线时长", "hours"),
        SPACES_EXPLORED("空间足迹", "探索过的空间数量", "count"),
        CONVERSATIONS_HAD("对话深度", "与虚拟人的对话次数", "count"),
        FRIENDS_MADE("社交联结", "好友数量", "count"),
        CREATIONS_BUILT("创造贡献", "创建的空间数量", "count"),
        SHARES_SPREAD("分享传播", "分享次数", "count"),
        ACHIEVEMENTS_UNLOCKED("成就解锁", "解锁的成就数量", "count"),
        AVATARS_COLLECTED("虚拟人羁绊", "收集的虚拟人数量", "count"),
        EVENTS_PARTICIPATED("活动参与", "参与的活动次数", "count"),
        CONSECUTIVE_DAYS("连续活跃", "连续登录天数", "days");
        
        private final String label;
        private final String description;
        private final String unit;

        DepthDimension(String label, String description, String unit) {
            this.label = label;
            this.description = description;
            this.unit = unit;
        }

        public String getLabel() { return label; }
        public String getDescription() { return description; }
        public String getUnit() { return unit; }
    }

    /**
     * 单个维度的度量值。
     */
    public static class DimensionMetric {
        private DepthDimension dimension;
        private double value;
        private double percentile;      // 在全平台中的百分位 (0-100)
        private double trend;           // 趋势：正数为增长，负数为下降
        private String highlight;       // 高亮文案

        public DimensionMetric() {}

        public DimensionMetric(DepthDimension dimension, double value, double percentile, double trend) {
            this.dimension = dimension;
            this.value = value;
            this.percentile = percentile;
            this.trend = trend;
        }

        public DepthDimension getDimension() { return dimension; }
        public void setDimension(DepthDimension dimension) { this.dimension = dimension; }
        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }
        public double getPercentile() { return percentile; }
        public void setPercentile(double percentile) { this.percentile = percentile; }
        public double getTrend() { return trend; }
        public void setTrend(double trend) { this.trend = trend; }
        public String getHighlight() { return highlight; }
        public void setHighlight(String highlight) { this.highlight = highlight; }
    }

    /**
     * 里程碑事件 — 用户成长中的关键时刻。
     */
    public static class Milestone {
        private String milestoneId;
        private String title;
        private String description;
        private Instant achievedAt;
        private String iconUrl;
        private boolean isHighlight;    // 是否为重点里程碑

        public Milestone() {}
        public Milestone(String milestoneId, String title, String description, Instant achievedAt) {
            this.milestoneId = milestoneId;
            this.title = title;
            this.description = description;
            this.achievedAt = achievedAt;
            this.isHighlight = false;
        }

        public String getMilestoneId() { return milestoneId; }
        public void setMilestoneId(String milestoneId) { this.milestoneId = milestoneId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Instant getAchievedAt() { return achievedAt; }
        public void setAchievedAt(Instant achievedAt) { this.achievedAt = achievedAt; }
        public String getIconUrl() { return iconUrl; }
        public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
        public boolean isHighlight() { return isHighlight; }
        public void setHighlight(boolean highlight) { isHighlight = highlight; }
    }

    // ---- FaithDashboard ----
    private String userId;
    private Map<DepthDimension, DimensionMetric> dimensions;
    private List<Milestone> milestones;
    private double overallFaithScore;      // 综合信仰分数 (0-1000)
    private FaithLevel faithLevel;
    private PresenceLevel presenceLevel;
    private String personalNarrative;      // 个性化叙述文案
    private Instant generatedAt;

    public FaithDashboard() {
        this.dimensions = new LinkedHashMap<>();
        this.milestones = new ArrayList<>();
    }

    public FaithDashboard(String userId) {
        this();
        this.userId = userId;
        this.overallFaithScore = 0.0;
        this.generatedAt = Instant.now();
    }

    /** 设置维度度量 */
    public void setDimension(DimensionMetric metric) {
        dimensions.put(metric.getDimension(), metric);
    }

    /** 添加里程碑 */
    public void addMilestone(Milestone milestone) {
        milestones.add(milestone);
    }

    /** 计算综合信仰分数 (所有维度加权平均) */
    public double calculateOverallFaithScore() {
        if (dimensions.isEmpty()) return 0.0;
        double sum = 0.0;
        int count = 0;
        for (DimensionMetric m : dimensions.values()) {
            sum += m.getPercentile();
            count++;
        }
        this.overallFaithScore = Math.min(1000, sum / count * 10);
        return overallFaithScore;
    }

    /** 获取最强维度 */
    public Optional<DimensionMetric> getStrongestDimension() {
        return dimensions.values().stream()
                .max(Comparator.comparingDouble(DimensionMetric::getPercentile));
    }

    /** 获取最弱维度 */
    public Optional<DimensionMetric> getWeakestDimension() {
        return dimensions.values().stream()
                .min(Comparator.comparingDouble(DimensionMetric::getPercentile));
    }

    /** 生成个性化叙述 */
    public String generateNarrative() {
        StringBuilder sb = new StringBuilder();
        sb.append("你在索拉的存在是真实的——");
        int milestoneCount = (int) milestones.stream().filter(Milestone::isHighlight).count();
        if (milestoneCount >= 5) {
            sb.append("你已经创造了 ").append(milestoneCount).append(" 个重要时刻。");
        } else if (milestoneCount > 0) {
            sb.append("你已经有 ").append(milestoneCount).append(" 个里程碑，每个都是你存在的证明。");
        } else {
            sb.append("每一个互动都在构建你与索拉的联结。");
        }
        getStrongestDimension().ifPresent(d -> 
            sb.append(" 你在「").append(d.getDimension().getLabel()).append("」上特别闪耀。"));
        this.personalNarrative = sb.toString();
        return personalNarrative;
    }

    /** 获取总里程碑数 */
    public int getMilestoneCount() {
        return milestones.size();
    }

    /** 获取重点里程碑数 */
    public long getHighlightMilestoneCount() {
        return milestones.stream().filter(Milestone::isHighlight).count();
    }

    // ---- getters/setters ----
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Map<DepthDimension, DimensionMetric> getDimensions() { return dimensions; }
    public void setDimensions(Map<DepthDimension, DimensionMetric> dimensions) { this.dimensions = dimensions; }
    public List<Milestone> getMilestones() { return milestones; }
    public void setMilestones(List<Milestone> milestones) { this.milestones = milestones; }
    public double getOverallFaithScore() { return overallFaithScore; }
    public void setOverallFaithScore(double overallFaithScore) { this.overallFaithScore = overallFaithScore; }
    public FaithLevel getFaithLevel() { return faithLevel; }
    public void setFaithLevel(FaithLevel faithLevel) { this.faithLevel = faithLevel; }
    public PresenceLevel getPresenceLevel() { return presenceLevel; }
    public void setPresenceLevel(PresenceLevel presenceLevel) { this.presenceLevel = presenceLevel; }
    public String getPersonalNarrative() { return personalNarrative; }
    public void setPersonalNarrative(String personalNarrative) { this.personalNarrative = personalNarrative; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
