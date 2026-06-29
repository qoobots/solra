package com.solra.mon.interfaces.rest;

import com.solra.mon.application.dto.OrderDTO;
import com.solra.mon.application.service.MonApplicationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单 REST 控制器。
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final MonApplicationService monService;

    public OrderController(MonApplicationService monService) {
        this.monService = monService;
    }

    /**
     * POST /api/v1/orders
     */
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("user_id");
        String currency = (String) request.getOrDefault("currency", "DIAMOND");
        OrderDTO order = monService.createOrder(userId, currency);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * GET /api/v1/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(monService.getOrder(orderId));
    }

    /**
     * POST /api/v1/orders/{orderId}/refund
     */
    @PostMapping("/{orderId}/refund")
    public ResponseEntity<Map<String, String>> refundOrder(@PathVariable String orderId) {
        monService.refundOrder(orderId);
        return ResponseEntity.ok(Map.of("status", "refunded", "order_id", orderId));
    }
}
