package com.solra.mon.interfaces.rest;

import com.solra.mon.domain.entity.BrandSpace;
import com.solra.mon.domain.service.BrandCommercialService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 品牌空间商业化 REST 控制器 — MON-005。
 *
 * <p>端点一览：
 * <ul>
 *   <li>GET  /api/v1/admin/brands                    — 列出全部品牌</li>
 *   <li>POST /api/v1/admin/brands                    — 品牌入驻</li>
 *   <li>GET  /api/v1/admin/brands/{brandId}          — 品牌详情</li>
 *   <li>POST /api/v1/admin/brands/{brandId}/renew     — 品牌续费</li>
 *   <li>POST /api/v1/admin/brands/{brandId}/suspend   — 品牌暂停</li>
 *   <li>POST /api/v1/admin/brands/{brandId}/ads       — 投放广告</li>
 *   <li>GET  /api/v1/admin/brands/{brandId}/ads       — 品牌广告列表</li>
 *   <li>GET  /api/v1/admin/brands/arpu-report         — ARPU 报告</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/brands")
public class BrandCommercialController {

    private final BrandCommercialService brandService;

    public BrandCommercialController(BrandCommercialService brandService) {
        this.brandService = brandService;
    }

    /** GET /api/v1/admin/brands */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listBrands(
            @RequestParam(required = false) String tier) {
        List<BrandSpace> brands;
        if (tier != null) {
            brands = brandService.listBrandsByTier(BrandSpace.BrandTier.valueOf(tier));
        } else {
            brands = brandService.listBrands();
        }

        return ResponseEntity.ok(brands.stream().map(this::toMap).collect(Collectors.toList()));
    }

    /** POST /api/v1/admin/brands */
    @PostMapping
    public ResponseEntity<Map<String, Object>> onboardBrand(@RequestBody Map<String, String> request) {
        String brandId = request.get("brand_id");
        String brandName = request.get("brand_name");
        BrandSpace.BrandTier tier = BrandSpace.BrandTier.valueOf(
                request.getOrDefault("tier", "STANDARD"));

        BrandSpace brand = brandService.onboardBrand(brandId, brandName, tier);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(brand));
    }

    /** GET /api/v1/admin/brands/{brandId} */
    @GetMapping("/{brandId}")
    public ResponseEntity<Map<String, Object>> getBrand(@PathVariable String brandId) {
        return brandService.getBrand(brandId)
                .map(b -> ResponseEntity.ok(toMap(b)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/v1/admin/brands/{brandId}/renew */
    @PostMapping("/{brandId}/renew")
    public ResponseEntity<Map<String, Object>> renewBrand(@PathVariable String brandId) {
        BrandSpace brand = brandService.renewBrand(brandId);
        return ResponseEntity.ok(toMap(brand));
    }

    /** POST /api/v1/admin/brands/{brandId}/suspend */
    @PostMapping("/{brandId}/suspend")
    public ResponseEntity<Map<String, String>> suspendBrand(@PathVariable String brandId) {
        brandService.suspendBrand(brandId);
        return ResponseEntity.ok(Map.of("status", "suspended", "brand_id", brandId));
    }

    /** POST /api/v1/admin/brands/{brandId}/ads */
    @PostMapping("/{brandId}/ads")
    public ResponseEntity<Map<String, Object>> placeAd(
            @PathVariable String brandId,
            @RequestBody Map<String, Object> request) {
        BrandSpace.AdType adType = BrandSpace.AdType.valueOf(
                (String) request.get("ad_type"));
        String targetSpaceId = (String) request.get("target_space_id");
        long price = ((Number) request.get("price")).longValue();

        BrandCommercialService.AdPlacement placement =
                brandService.placeAd(brandId, adType, targetSpaceId, price);

        return ResponseEntity.ok(Map.of(
                "placement_id", placement.getPlacementId(),
                "brand_id", placement.getBrandId(),
                "ad_type", placement.getAdType().name(),
                "price", placement.getPrice()
        ));
    }

    /** GET /api/v1/admin/brands/{brandId}/ads */
    @GetMapping("/{brandId}/ads")
    public ResponseEntity<List<Map<String, Object>>> getAds(@PathVariable String brandId) {
        List<BrandCommercialService.AdPlacement> ads = brandService.getAdPlacements(brandId);
        return ResponseEntity.ok(ads.stream().map(a -> Map.<String, Object>of(
                "placement_id", a.getPlacementId(),
                "ad_type", a.getAdType().name(),
                "target_space_id", a.getTargetSpaceId(),
                "price", a.getPrice()
        )).collect(Collectors.toList()));
    }

    /** GET /api/v1/admin/brands/arpu-report */
    @GetMapping("/arpu-report")
    public ResponseEntity<Map<String, Object>> getARPUReport() {
        BrandCommercialService.BrandARPUReport report = brandService.getARPUReport();
        return ResponseEntity.ok(Map.of(
                "brand_count", report.getBrandCount(),
                "avg_arpu", report.getAvgARPU(),
                "above_target", report.getAboveTarget(),
                "target_rate", String.format("%.1f%%", report.getTargetRate()),
                "details", report.getDetails()
        ));
    }

    private Map<String, Object> toMap(BrandSpace brand) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("brand_space_id", brand.getBrandSpaceId());
        m.put("brand_id", brand.getBrandId());
        m.put("brand_name", brand.getBrandName());
        m.put("tier", brand.getTier().name());
        m.put("status", brand.getStatus().name());
        m.put("annual_fee", brand.getAnnualFee());
        m.put("ad_revenue", brand.getTotalAdRevenue());
        m.put("arpu", brand.getARPU());
        m.put("meets_target", brand.meetsARPUtarget());
        m.put("expire_at", brand.getExpireAt());
        return m;
    }
}
