package com.solra.mon.interfaces.rest;

import com.solra.mon.application.dto.SubscriptionDTO;
import com.solra.mon.application.dto.SubscriptionPlanDTO;
import com.solra.mon.application.dto.UpgradePreviewDTO;
import com.solra.mon.application.service.MonApplicationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订阅 REST 控制器 — MON-001 Solra Plus 四档订阅。
 *
 * <p>端点一览：
 * <ul>
 *   <li>GET  /api/v1/subscriptions/plans              — 列出全部订阅计划</li>
 *   <li>GET  /api/v1/subscriptions/{userId}            — 查询用户当前订阅</li>
 *   <li>POST /api/v1/subscriptions                     — 新订阅</li>
 *   <li>POST /api/v1/subscriptions/{userId}/upgrade     — 升级</li>
 *   <li>POST /api/v1/subscriptions/{userId}/downgrade   — 降级</li>
 *   <li>POST /api/v1/subscriptions/{userId}/renew       — 续费</li>
 *   <li>POST /api/v1/subscriptions/{userId}/cancel      — 取消</li>
 *   <li>GET  /api/v1/subscriptions/{userId}/upgrade-preview — 升级差价预览</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final MonApplicationService monService;

    public SubscriptionController(MonApplicationService monService) {
        this.monService = monService;
    }

    /** GET /api/v1/subscriptions/plans */
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlanDTO>> listPlans() {
        return ResponseEntity.ok(monService.listSubscriptionPlans());
    }

    /** GET /api/v1/subscriptions/{userId} */
    @GetMapping("/{userId}")
    public ResponseEntity<SubscriptionDTO> getUserSubscription(@PathVariable String userId) {
        SubscriptionDTO sub = monService.getUserSubscription(userId);
        if (sub == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sub);
    }

    /** POST /api/v1/subscriptions */
    @PostMapping
    public ResponseEntity<SubscriptionDTO> subscribe(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("user_id");
        String tier = (String) request.get("tier");
        String cycle = (String) request.getOrDefault("cycle", "MONTHLY");
        SubscriptionDTO sub = monService.subscribe(userId, tier, cycle);
        return ResponseEntity.status(HttpStatus.CREATED).body(sub);
    }

    /** POST /api/v1/subscriptions/{userId}/upgrade */
    @PostMapping("/{userId}/upgrade")
    public ResponseEntity<SubscriptionDTO> upgradeSubscription(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        String newTier = request.get("tier");
        String cycle = request.getOrDefault("cycle", null);
        return ResponseEntity.ok(monService.upgradeSubscription(userId, newTier, cycle));
    }

    /** POST /api/v1/subscriptions/{userId}/downgrade */
    @PostMapping("/{userId}/downgrade")
    public ResponseEntity<SubscriptionDTO> downgradeSubscription(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        String newTier = request.get("tier");
        return ResponseEntity.ok(monService.downgradeSubscription(userId, newTier));
    }

    /** POST /api/v1/subscriptions/{userId}/renew */
    @PostMapping("/{userId}/renew")
    public ResponseEntity<SubscriptionDTO> renewSubscription(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        String cycle = request.getOrDefault("cycle", null);
        return ResponseEntity.ok(monService.renewSubscription(userId, cycle));
    }

    /** POST /api/v1/subscriptions/{userId}/cancel */
    @PostMapping("/{userId}/cancel")
    public ResponseEntity<SubscriptionDTO> cancelSubscription(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        String reason = request.getOrDefault("reason", "user_request");
        return ResponseEntity.ok(monService.cancelSubscription(userId, reason));
    }

    /** GET /api/v1/subscriptions/{userId}/upgrade-preview */
    @GetMapping("/{userId}/upgrade-preview")
    public ResponseEntity<UpgradePreviewDTO> previewUpgrade(
            @PathVariable String userId,
            @RequestParam String tier,
            @RequestParam(defaultValue = "MONTHLY") String cycle) {
        return ResponseEntity.ok(monService.previewUpgrade(userId, tier, cycle));
    }
}
