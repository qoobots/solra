package com.solra.grw.domain.model;

import java.time.Instant;

/**
 * Evangelist 聚合根 — 布道者体系。
 * GRW-003: 布道者申请/认证/权益/义务，访问≥1万+粉丝≥500，占DAU 0.5-1%。
 */
public class Evangelist {

    public enum ApplicationStatus {
        PENDING,        // 待审核
        APPROVED,       // 已通过
        REJECTED,       // 已拒绝
        SUSPENDED,      // 已暂停
        REVOKED         // 已撤销
    }

    public enum EvangelistTier {
        /** 学徒布道者 — 100+粉丝，500+访问 */
        APPRENTICE(100, 500, 3, "学徒布道者"),
        /** 正式布道者 — 500+粉丝，2000+访问 */
        JOURNEYMAN(500, 2000, 5, "正式布道者"),
        /** 资深布道者 — 1000+粉丝，5000+访问 */
        MASTER(1000, 5000, 8, "资深布道者"),
        /** 传奇布道者 — 5000+粉丝，10000+访问 */
        LEGEND(5000, 10000, 12, "传奇布道者");

        private final int minFollowers;
        private final int minVisits;
        private final int perks;         // 权益数量
        private final String displayName;

        EvangelistTier(int minFollowers, int minVisits, int perks, String displayName) {
            this.minFollowers = minFollowers;
            this.minVisits = minVisits;
            this.perks = perks;
            this.displayName = displayName;
        }

        public static EvangelistTier fromStats(int followers, int totalVisits) {
            EvangelistTier result = APPRENTICE;
            for (EvangelistTier tier : values()) {
                if (followers >= tier.minFollowers && totalVisits >= tier.minVisits) {
                    result = tier;
                }
            }
            return result;
        }

        public int getMinFollowers() { return minFollowers; }
        public int getMinVisits() { return minVisits; }
        public int getPerks() { return perks; }
        public String getDisplayName() { return displayName; }
    }

    private String applicationId;
    private String userId;
    private String displayName;        // 布道者展示名
    private String bio;                // 个人简介
    private EvangelistTier tier;
    private ApplicationStatus status;
    private int followersCount;        // 粉丝数
    private int totalVisits;           // 总访问数
    private int spacesCreated;         // 创建空间数
    private int sharesGenerated;       // 分享产生数
    private double contributionScore;  // 贡献分数 (0-1000)
    private String reviewerId;         // 审核人ID
    private String reviewComment;      // 审核意见
    private Instant appliedAt;
    private Instant reviewedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public Evangelist() {}

    public Evangelist(String applicationId, String userId, String displayName, String bio) {
        this.applicationId = applicationId;
        this.userId = userId;
        this.displayName = displayName;
        this.bio = bio;
        this.tier = EvangelistTier.APPRENTICE;
        this.status = ApplicationStatus.PENDING;
        this.followersCount = 0;
        this.totalVisits = 0;
        this.spacesCreated = 0;
        this.sharesGenerated = 0;
        this.contributionScore = 0.0;
        this.appliedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** 审批通过 */
    public void approve(String reviewerId, String comment) {
        this.status = ApplicationStatus.APPROVED;
        this.reviewerId = reviewerId;
        this.reviewComment = comment;
        this.reviewedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /** 审批拒绝 */
    public void reject(String reviewerId, String comment) {
        this.status = ApplicationStatus.REJECTED;
        this.reviewerId = reviewerId;
        this.reviewComment = comment;
        this.reviewedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /** 暂停布道者资格 */
    public void suspend(String reason) {
        this.status = ApplicationStatus.SUSPENDED;
        this.reviewComment = reason;
        this.updatedAt = Instant.now();
    }

    /** 撤销布道者资格 */
    public void revoke(String reason) {
        this.status = ApplicationStatus.REVOKED;
        this.reviewComment = reason;
        this.updatedAt = Instant.now();
    }

    /** 更新粉丝数并重新评估等级 */
    public void updateFollowers(int count) {
        this.followersCount = count;
        this.updatedAt = Instant.now();
        recalculateTier();
    }

    /** 更新访问数并重新评估等级 */
    public void updateVisits(int count) {
        this.totalVisits = count;
        this.updatedAt = Instant.now();
        recalculateTier();
    }

    /** 增加创建空间数 */
    public void incrementSpacesCreated() {
        this.spacesCreated++;
        this.contributionScore = Math.min(1000, this.contributionScore + 5);
        this.updatedAt = Instant.now();
    }

    /** 增加分享产生数 */
    public void incrementSharesGenerated() {
        this.sharesGenerated++;
        this.contributionScore = Math.min(1000, this.contributionScore + 3);
        this.updatedAt = Instant.now();
    }

    /** 更新贡献分数 */
    public void updateContributionScore(double score) {
        this.contributionScore = Math.min(1000, Math.max(0, score));
        this.updatedAt = Instant.now();
    }

    /** 判断是否达到布道者资格标准 */
    public boolean isEligible() {
        return followersCount >= EvangelistTier.APPRENTICE.getMinFollowers()
                && totalVisits >= EvangelistTier.APPRENTICE.getMinVisits();
    }

    /** 判断是否为活跃布道者 */
    public boolean isActive() {
        return status == ApplicationStatus.APPROVED;
    }

    private void recalculateTier() {
        this.tier = EvangelistTier.fromStats(followersCount, totalVisits);
    }

    // ---- getters/setters ----
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public EvangelistTier getTier() { return tier; }
    public void setTier(EvangelistTier tier) { this.tier = tier; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public int getFollowersCount() { return followersCount; }
    public void setFollowersCount(int followersCount) { this.followersCount = followersCount; }
    public int getTotalVisits() { return totalVisits; }
    public void setTotalVisits(int totalVisits) { this.totalVisits = totalVisits; }
    public int getSpacesCreated() { return spacesCreated; }
    public void setSpacesCreated(int spacesCreated) { this.spacesCreated = spacesCreated; }
    public int getSharesGenerated() { return sharesGenerated; }
    public void setSharesGenerated(int sharesGenerated) { this.sharesGenerated = sharesGenerated; }
    public double getContributionScore() { return contributionScore; }
    public void setContributionScore(double contributionScore) { this.contributionScore = contributionScore; }
    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant appliedAt) { this.appliedAt = appliedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
