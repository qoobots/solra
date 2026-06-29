package com.solra.mon.domain.service;

import com.solra.mon.domain.entity.BrandSpace;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 品牌空间商业化领域服务 — MON-005。
 *
 * <p>职责：
 * <ul>
 *   <li>品牌入驻：三种年框等级</li>
 *   <li>空间广告管理：横幅/品牌植入/赞助/推荐</li>
 *   <li>ARPU 监控：单品空间目标 > ¥100万</li>
 *   <li>品牌结算</li>
 * </ul>
 */
public class BrandCommercialService {

    private final Map<String, BrandSpace> brands = new ConcurrentHashMap<>();
    private final Map<String, List<AdPlacement>> adPlacements = new ConcurrentHashMap<>();

    // ── 品牌管理 ──

    /** 品牌入驻。 */
    public BrandSpace onboardBrand(String brandId, String brandName, BrandSpace.BrandTier tier) {
        if (brands.containsKey(brandId)) {
            throw new IllegalStateException("Brand already onboard: " + brandId);
        }
        BrandSpace brand = new BrandSpace(brandId, brandName, tier);
        brands.put(brandId, brand);
        return brand;
    }

    /** 获取品牌信息。 */
    public Optional<BrandSpace> getBrand(String brandId) {
        return Optional.ofNullable(brands.get(brandId));
    }

    /** 列出所有品牌。 */
    public List<BrandSpace> listBrands() {
        return new ArrayList<>(brands.values());
    }

    /** 按等级筛选品牌。 */
    public List<BrandSpace> listBrandsByTier(BrandSpace.BrandTier tier) {
        return brands.values().stream()
                .filter(b -> b.getTier() == tier)
                .collect(Collectors.toList());
    }

    /** 品牌续费。 */
    public BrandSpace renewBrand(String brandId) {
        BrandSpace brand = brands.get(brandId);
        if (brand == null) throw new IllegalArgumentException("Brand not found: " + brandId);

        BrandSpace renewed = new BrandSpace(brandId, brand.getBrandName(), brand.getTier());
        brands.put(brandId, renewed);
        return renewed;
    }

    /** 品牌暂停。 */
    public BrandSpace suspendBrand(String brandId) {
        BrandSpace brand = brands.get(brandId);
        if (brand == null) throw new IllegalArgumentException("Brand not found: " + brandId);
        brand.suspend();
        return brand;
    }

    // ── 广告管理 ──

    /** 投放广告。 */
    public AdPlacement placeAd(String brandId, BrandSpace.AdType adType,
                                String targetSpaceId, long price) {
        BrandSpace brand = brands.get(brandId);
        if (brand == null) throw new IllegalArgumentException("Brand not found: " + brandId);
        if (!brand.isActive()) throw new IllegalStateException("Brand is not active");

        AdPlacement placement = new AdPlacement(brandId, adType, targetSpaceId, price);
        adPlacements.computeIfAbsent(brandId, k -> new ArrayList<>()).add(placement);

        brand.addAdRevenue(price);
        return placement;
    }

    /** 获取品牌广告投放列表。 */
    public List<AdPlacement> getAdPlacements(String brandId) {
        return adPlacements.getOrDefault(brandId, List.of());
    }

    // ── ARPU 统计 ──

    /** 全部品牌 ARPU 汇总。 */
    public BrandARPUReport getARPUReport() {
        long totalARPU = 0;
        int count = 0;
        int aboveTarget = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (BrandSpace brand : brands.values()) {
            long arpu = brand.getARPU();
            totalARPU += arpu;
            count++;
            if (brand.meetsARPUtarget()) aboveTarget++;

            details.add(Map.of(
                    "brand_id", brand.getBrandId(),
                    "brand_name", brand.getBrandName(),
                    "tier", brand.getTier().name(),
                    "annual_fee", brand.getAnnualFee(),
                    "ad_revenue", brand.getTotalAdRevenue(),
                    "arpu", arpu,
                    "meets_target", brand.meetsARPUtarget()
            ));
        }

        long avgARPU = count > 0 ? totalARPU / count : 0;
        return new BrandARPUReport(count, avgARPU, aboveTarget, details);
    }

    // ── 值对象 ──

    /** 广告投放记录。 */
    public static class AdPlacement {
        private final String placementId;
        private final String brandId;
        private final BrandSpace.AdType adType;
        private final String targetSpaceId;
        private final long price;
        private final long placedAt;

        public AdPlacement(String brandId, BrandSpace.AdType adType,
                            String targetSpaceId, long price) {
            this.placementId = "AD-" + UUID.randomUUID().toString().substring(0, 8);
            this.brandId = brandId;
            this.adType = adType;
            this.targetSpaceId = targetSpaceId;
            this.price = price;
            this.placedAt = System.currentTimeMillis();
        }

        public String getPlacementId() { return placementId; }
        public String getBrandId() { return brandId; }
        public BrandSpace.AdType getAdType() { return adType; }
        public String getTargetSpaceId() { return targetSpaceId; }
        public long getPrice() { return price; }
        public long getPlacedAt() { return placedAt; }
    }

    /** 品牌 ARPU 报告。 */
    public static class BrandARPUReport {
        private final int brandCount;
        private final long avgARPU;
        private final int aboveTarget;
        private final List<Map<String, Object>> details;

        public BrandARPUReport(int brandCount, long avgARPU, int aboveTarget,
                                List<Map<String, Object>> details) {
            this.brandCount = brandCount;
            this.avgARPU = avgARPU;
            this.aboveTarget = aboveTarget;
            this.details = List.copyOf(details);
        }

        public int getBrandCount() { return brandCount; }
        public long getAvgARPU() { return avgARPU; }
        public int getAboveTarget() { return aboveTarget; }
        public List<Map<String, Object>> getDetails() { return details; }

        /** 达成率（ARPU > ¥100万的品牌占比）。 */
        public double getTargetRate() {
            return brandCount > 0 ? (double) aboveTarget / brandCount * 100 : 0;
        }
    }
}
