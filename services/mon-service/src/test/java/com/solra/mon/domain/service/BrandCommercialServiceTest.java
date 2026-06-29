package com.solra.mon.domain.service;

import com.solra.mon.domain.entity.BrandSpace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BrandCommercialService 单元测试 — MON-005。
 * 验证品牌入驻、广告投放、ARPU统计。
 */
@DisplayName("BrandCommercialService — 品牌空间商业化测试")
class BrandCommercialServiceTest {

    private BrandCommercialService service;

    @BeforeEach
    void setUp() {
        service = new BrandCommercialService();
    }

    @Nested
    @DisplayName("品牌入驻")
    class Onboard {

        @Test
        @DisplayName("STANDARD 品牌入驻年框 ¥10万")
        void standardOnboard() {
            BrandSpace brand = service.onboardBrand("brand-1", "Nike", BrandSpace.BrandTier.STANDARD);

            assertEquals("brand-1", brand.getBrandId());
            assertEquals("Nike", brand.getBrandName());
            assertEquals(BrandSpace.BrandTier.STANDARD, brand.getTier());
            assertEquals(10_000_000L, brand.getAnnualFee());
            assertTrue(brand.isActive());
        }

        @Test
        @DisplayName("PREMIUM 品牌入驻年框 ¥50万")
        void premiumOnboard() {
            BrandSpace brand = service.onboardBrand("brand-2", "Adidas", BrandSpace.BrandTier.PREMIUM);
            assertEquals(50_000_000L, brand.getAnnualFee());
        }

        @Test
        @DisplayName("ENTERPRISE 品牌入驻年框 ¥100万")
        void enterpriseOnboard() {
            BrandSpace brand = service.onboardBrand("brand-3", "Apple", BrandSpace.BrandTier.ENTERPRISE);
            assertEquals(100_000_000L, brand.getAnnualFee());
        }

        @Test
        @DisplayName("重复入驻抛出异常")
        void duplicateOnboardThrows() {
            service.onboardBrand("brand-1", "Nike", BrandSpace.BrandTier.STANDARD);
            assertThrows(IllegalStateException.class, () ->
                    service.onboardBrand("brand-1", "Nike Again", BrandSpace.BrandTier.PREMIUM));
        }
    }

    @Nested
    @DisplayName("广告投放")
    class AdPlacement {

        @Test
        @DisplayName("投放广告后品牌广告收入增加")
        void adIncreasesRevenue() {
            BrandSpace brand = service.onboardBrand("brand-1", "Nike", BrandSpace.BrandTier.STANDARD);

            service.placeAd("brand-1", BrandSpace.AdType.SPACE_BRAND, "space-001", 5_000_000L);

            BrandSpace updated = service.getBrand("brand-1").orElseThrow();
            assertEquals(5_000_000L, updated.getTotalAdRevenue());
            assertEquals(15_000_000L, updated.getARPU()); // 10万 + 5万
        }

        @Test
        @DisplayName("多条广告累计收入")
        void multipleAdsAccumulate() {
            service.onboardBrand("brand-1", "Nike", BrandSpace.BrandTier.STANDARD);
            service.placeAd("brand-1", BrandSpace.AdType.BANNER, "space-1", 1_000_000L);
            service.placeAd("brand-1", BrandSpace.AdType.FEATURED, "space-2", 3_000_000L);

            List<BrandCommercialService.AdPlacement> ads = service.getAdPlacements("brand-1");
            assertEquals(2, ads.size());
        }

        @Test
        @DisplayName("非活跃品牌不能投放广告")
        void inactiveBrandCannotPlaceAd() {
            service.onboardBrand("brand-1", "Nike", BrandSpace.BrandTier.STANDARD);
            service.suspendBrand("brand-1");

            assertThrows(IllegalStateException.class, () ->
                    service.placeAd("brand-1", BrandSpace.AdType.BANNER, "space-1", 1_000_000L));
        }
    }

    @Nested
    @DisplayName("ARPU 报告")
    class ARPUReport {

        @Test
        @DisplayName("ENTERPRISE 品牌入驻即达 ARPU 目标")
        void enterpriseMeetsTarget() {
            BrandSpace brand = service.onboardBrand("brand-1", "Apple", BrandSpace.BrandTier.ENTERPRISE);
            assertTrue(brand.meetsARPUtarget());
            assertEquals(100_000_000L, brand.getARPU());
        }

        @Test
        @DisplayName("STANDARD 品牌需广告收入达标")
        void standardNeedsAdRevenue() {
            BrandSpace brand = service.onboardBrand("brand-1", "Nike", BrandSpace.BrandTier.STANDARD);
            assertFalse(brand.meetsARPUtarget());

            // 投放足够广告
            service.placeAd("brand-1", BrandSpace.AdType.SPACE_BRAND, "space-1", 90_000_000L);

            brand = service.getBrand("brand-1").orElseThrow();
            assertTrue(brand.meetsARPUtarget());
            assertEquals(100_000_000L, brand.getARPU());
        }

        @Test
        @DisplayName("ARPU 报告汇总所有品牌")
        void arpuReportAggregates() {
            service.onboardBrand("b1", "Nike", BrandSpace.BrandTier.STANDARD);
            service.onboardBrand("b2", "Apple", BrandSpace.BrandTier.ENTERPRISE);

            BrandCommercialService.BrandARPUReport report = service.getARPUReport();
            assertEquals(2, report.getBrandCount());
            assertEquals(1, report.getAboveTarget());
            assertEquals(50.0, report.getTargetRate(), 0.1);
        }
    }

    @Nested
    @DisplayName("品牌生命周期")
    class Lifecycle {

        @Test
        @DisplayName("暂停后不可用")
        void suspendMakesInactive() {
            BrandSpace brand = service.onboardBrand("brand-1", "Nike", BrandSpace.BrandTier.STANDARD);
            service.suspendBrand("brand-1");

            brand = service.getBrand("brand-1").orElseThrow();
            assertFalse(brand.isActive());
            assertEquals(BrandSpace.BrandStatus.SUSPENDED, brand.getStatus());
        }

        @Test
        @DisplayName("续费后重新激活")
        void renewReactivates() {
            service.onboardBrand("brand-1", "Nike", BrandSpace.BrandTier.STANDARD);
            BrandSpace renewed = service.renewBrand("brand-1");

            assertTrue(renewed.isActive());
            assertEquals(BrandSpace.BrandStatus.ACTIVE, renewed.getStatus());
        }
    }
}
