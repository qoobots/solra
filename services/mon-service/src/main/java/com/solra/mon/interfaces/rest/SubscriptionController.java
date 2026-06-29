package com.solra.mon.interfaces.rest;

import com.solra.mon.application.dto.SubscriptionDTO;
import com.solra.mon.application.dto.SubscriptionPlanDTO;
import com.solra.mon.application.service.MonApplicationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订阅 REST 控制器。
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final MonApplicationService monService;

    public SubscriptionController(MonApplicationService monService) {
        this.monService = monService;
    }

    /**
     * GET /api/v1/subscriptions/plans
     */
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlanDTO>> listPlans() {
        return ResponseEntity.ok(monService.listSubscriptionPlans());
    }

    /**
     * POST /api/v1/subscriptions
     */
    @PostMapping
    public ResponseEntity<SubscriptionDTO> subscribe(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("user_id");
        String tier = (String) request.get("tier");
        boolean yearly = (boolean) request.getOrDefault("yearly", false);
        SubscriptionDTO sub = monService.subscribe(userId, tier, yearly);
        return ResponseEntity.status(HttpStatus.CREATED).body(sub);
    }

    /**
     * POST /api/v1/subscriptions/{userId}/cancel
     */
    @PostMapping("/{userId}/cancel")
    public ResponseEntity<SubscriptionDTO> cancelSubscription(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        String reason = request.getOrDefault("reason", "user_request");
        return ResponseEntity.ok(monService.cancelSubscription(userId, reason));
    }
}
