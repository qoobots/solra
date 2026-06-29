package com.solra.mon.interfaces.rest;

import com.solra.mon.application.dto.WalletDTO;
import com.solra.mon.application.service.MonApplicationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 钱包 REST 控制器。
 */
@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final MonApplicationService monService;

    public WalletController(MonApplicationService monService) {
        this.monService = monService;
    }

    /**
     * GET /api/v1/wallet/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<WalletDTO> getWallet(@PathVariable String userId) {
        return ResponseEntity.ok(monService.getWallet(userId));
    }

    /**
     * POST /api/v1/wallet/{userId}/add
     */
    @PostMapping("/{userId}/add")
    public ResponseEntity<WalletDTO> addBalance(@PathVariable String userId,
                                                 @RequestBody Map<String, Object> request) {
        String currency = (String) request.get("currency");
        long amount = ((Number) request.get("amount")).longValue();
        String reason = (String) request.getOrDefault("reason", "manual");
        return ResponseEntity.ok(monService.addBalance(userId, currency, amount, reason));
    }

    /**
     * POST /api/v1/wallet/{userId}/deduct
     */
    @PostMapping("/{userId}/deduct")
    public ResponseEntity<WalletDTO> deductBalance(@PathVariable String userId,
                                                    @RequestBody Map<String, Object> request) {
        String currency = (String) request.get("currency");
        long amount = ((Number) request.get("amount")).longValue();
        String reason = (String) request.getOrDefault("reason", "purchase");
        return ResponseEntity.ok(monService.deductBalance(userId, currency, amount, reason));
    }
}
