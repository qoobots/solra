package com.solra.mon.domain.entity;

import java.util.UUID;

/**
 * 品牌空间领域实体 — MON-005。
 *
 * <p>品牌年框 + 空间广告商业化。
 * 验收标准：单品空间 ARPU > ¥100万。
 */
public class BrandSpace {

    public enum BrandTier {
        STANDARD,    // 标准品牌（年框 ¥10万）
        PREMIUM,     // 高级品牌（年框 ¥50万）
        ENTERPRISE   // 企业品牌（年框 ¥100万+）
    }

    public enum BrandStatus {
        ACTIVE, SUSPENDED, EXPIRED
    }

    public enum AdType {
        BANNER,       // 横幅广告
        SPACE_BRAND,  // 空间品牌植入
        SPONSORED,    // 赞助内容
        FEATURED      // 首页推荐
    }

    private String brandSpaceId;
    private String brandId;
    private String brandName;
    private BrandTier tier;
    private BrandStatus status;
    private long annualFee;          // 年框费用（分）
    private long startAt;
    private long expireAt;
    private String spaceTemplateId;  // 关联的品牌空间模板
    private long totalAdRevenue;     // 累计广告收入（分）
    private long createdAt;
    private long updatedAt;

    public BrandSpace(String brandId, String brandName, BrandTier tier) {
        this.brandSpaceId = "BRAND-" + UUID.randomUUID().toString().substring(0, 8);
        this.brandId = brandId;
        this.brandName = brandName;
        this.tier = tier;
        this.status = BrandStatus.ACTIVE;
        this.annualFee = calculateAnnualFee(tier);
        this.startAt = System.currentTimeMillis();
        this.expireAt = startAt + 365L * 24 * 3600 * 1000;
        this.totalAdRevenue = 0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    /** 根据等级计算年框费用。 */
    public static long calculateAnnualFee(BrandTier tier) {
        return switch (tier) {
            case STANDARD -> 10_000_000L;  // ¥10万
            case PREMIUM -> 50_000_000L;   // ¥50万
            case ENTERPRISE -> 100_000_000L; // ¥100万+
        };
    }

    /** 记录广告收入。 */
    public void addAdRevenue(long amount) {
        this.totalAdRevenue += amount;
        this.updatedAt = System.currentTimeMillis();
    }

    /** ARPU = (年框费 + 广告收入) / 品牌数（简化：单品牌ARPU）。 */
    public long getARPU() {
        return annualFee + totalAdRevenue;
    }

    /** 是否达到 ARPU 目标（> ¥100万）。 */
    public boolean meetsARPUtarget() {
        return getARPU() >= 100_000_000L;
    }

    public void suspend() {
        this.status = BrandStatus.SUSPENDED;
        this.updatedAt = System.currentTimeMillis();
    }

    public void expire() {
        this.status = BrandStatus.EXPIRED;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isActive() {
        return status == BrandStatus.ACTIVE
                && System.currentTimeMillis() < expireAt;
    }

    // Getters
    public String getBrandSpaceId() { return brandSpaceId; }
    public String getBrandId() { return brandId; }
    public String getBrandName() { return brandName; }
    public BrandTier getTier() { return tier; }
    public BrandStatus getStatus() { return status; }
    public long getAnnualFee() { return annualFee; }
    public long getStartAt() { return startAt; }
    public long getExpireAt() { return expireAt; }
    public String getSpaceTemplateId() { return spaceTemplateId; }
    public void setSpaceTemplateId(String spaceTemplateId) { this.spaceTemplateId = spaceTemplateId; }
    public long getTotalAdRevenue() { return totalAdRevenue; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
