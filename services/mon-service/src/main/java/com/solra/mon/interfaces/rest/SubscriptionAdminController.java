package com.solra.mon.interfaces.rest;

import com.solra.mon.application.dto.SubscriptionDTO;
import com.solra.mon.application.service.MonApplicationService;
import com.solra.mon.domain.entity.Subscription;
import com.solra.mon.domain.service.SubscriptionAdminService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 订阅管理后台 REST 控制器 — MON-006。
 *
 * <p>端点一览（管理后台，需 ADMIN 权限）：
 * <ul>
 *   <li>GET  /api/v1/admin/subscriptions/stats         — 订阅统计概览</li>
 *   <li>GET  /api/v1/admin/subscriptions/expiring      — 即将到期列表</li>
 *   <li>GET  /api/v1/admin/subscriptions?tier=&status=  — 筛选订阅列表</li>
 *   <li>POST /api/v1/admin/subscriptions/upgrade        — 管理员升级</li>
 *   <li>POST /api/v1/admin/subscriptions/downgrade      — 管理员降级</li>
 *   <li>POST /api/v1/admin/subscriptions/extend         — 管理员延长</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/subscriptions")
public class SubscriptionAdminController {

    private final SubscriptionAdminService adminService;
    private final MonApplicationService monService;

    public SubscriptionAdminController(SubscriptionAdminService adminService,
                                        MonApplicationService monService) {
        this.adminService = adminService;
        this.monService = monService;
    }

    /** GET /api/v1/admin/subscriptions/stats */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        SubscriptionAdminService.SubscriptionStats stats = adminService.getStats();

        Map<String, Long> byTier = new LinkedHashMap<>();
        stats.getByTier().forEach((k, v) -> byTier.put(k.name(), v));

        return ResponseEntity.ok(Map.of(
                "total", stats.getTotal(),
                "active", stats.getActive(),
                "cancelled", stats.getCancelled(),
                "expired", stats.getExpired(),
                "by_tier", byTier,
                "renewal_rate", String.format("%.1f%%", stats.getRenewalRate()),
                "churn_rate", String.format("%.1f%%", stats.getChurnRate())
        ));
    }

    /** GET /api/v1/admin/subscriptions/expiring?days=7 */
    @GetMapping("/expiring")
    public ResponseEntity<List<SubscriptionDTO>> getExpiringSoon(
            @RequestParam(defaultValue = "7") int days) {
        List<Subscription> subs = adminService.getExpiringSoon(days);
        return ResponseEntity.ok(subs.stream().map(SubscriptionDTO::from).collect(Collectors.toList()));
    }

    /** GET /api/v1/admin/subscriptions?tier=PLUS&status=ACTIVE */
    @GetMapping
    public ResponseEntity<List<SubscriptionDTO>> listSubscriptions(
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) String status) {

        List<Subscription> subs;
        if (tier != null) {
            subs = adminService.listByTier(Subscription.SubscriptionTier.valueOf(tier));
        } else if (status != null) {
            subs = adminService.listByStatus(Subscription.SubscriptionStatus.valueOf(status));
        } else {
            subs = adminService.listByStatus(null);
        }

        return ResponseEntity.ok(subs.stream().map(SubscriptionDTO::from).collect(Collectors.toList()));
    }

    /** POST /api/v1/admin/subscriptions/upgrade */
    @PostMapping("/upgrade")
    public ResponseEntity<SubscriptionDTO> adminUpgrade(@RequestBody Map<String, String> request) {
        String userId = request.get("user_id");
        Subscription.SubscriptionTier newTier = Subscription.SubscriptionTier.valueOf(request.get("tier"));
        Subscription.BillingCycle cycle = request.containsKey("cycle")
                ? Subscription.BillingCycle.valueOf(request.get("cycle"))
                : Subscription.BillingCycle.MONTHLY;

        Subscription sub = adminService.adminUpgrade(userId, newTier, cycle);
        return ResponseEntity.ok(SubscriptionDTO.from(sub));
    }

    /** POST /api/v1/admin/subscriptions/downgrade */
    @PostMapping("/downgrade")
    public ResponseEntity<SubscriptionDTO> adminDowngrade(@RequestBody Map<String, String> request) {
        String userId = request.get("user_id");
        Subscription.SubscriptionTier newTier = Subscription.SubscriptionTier.valueOf(request.get("tier"));

        Subscription sub = adminService.adminDowngrade(userId, newTier);
        return ResponseEntity.ok(SubscriptionDTO.from(sub));
    }

    /** POST /api/v1/admin/subscriptions/extend */
    @PostMapping("/extend")
    public ResponseEntity<SubscriptionDTO> adminExtend(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("user_id");
        int days = ((Number) request.getOrDefault("days", 30)).intValue();

        Subscription sub = adminService.adminExtend(userId, days);
        return ResponseEntity.ok(SubscriptionDTO.from(sub));
    }
}
