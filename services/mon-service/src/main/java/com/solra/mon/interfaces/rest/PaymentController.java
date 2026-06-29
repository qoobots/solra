package com.solra.mon.interfaces.rest;

import com.solra.mon.application.service.MonApplicationService;
import com.solra.mon.infrastructure.payment.PaymentChannel;
import com.solra.mon.infrastructure.payment.PaymentGateway;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 支付 REST 控制器 — MON-004 支付集成。
 *
 * <p>端点一览：
 * <ul>
 *   <li>GET  /api/v1/payments/channels                    — 列出可用支付渠道</li>
 *   <li>POST /api/v1/payments/create                       — 创建支付（获取预支付凭证）</li>
 *   <li>POST /api/v1/payments/verify                       — 验证支付回执</li>
 *   <li>POST /api/v1/payments/{orderId}/refund              — 退款</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentGateway paymentGateway;
    private final MonApplicationService monService;

    public PaymentController(PaymentGateway paymentGateway,
                              MonApplicationService monService) {
        this.paymentGateway = paymentGateway;
        this.monService = monService;
    }

    /** GET /api/v1/payments/channels */
    @GetMapping("/channels")
    public ResponseEntity<List<Map<String, String>>> listChannels() {
        return ResponseEntity.ok(paymentGateway.listAvailableChannels());
    }

    /** POST /api/v1/payments/create */
    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("order_id");
        String channelId = (String) request.get("channel_id");
        @SuppressWarnings("unchecked")
        Map<String, String> metadata = (Map<String, String>) request.getOrDefault("metadata", Map.of());

        PaymentChannel.PaymentResult result = paymentGateway.createPayment(orderId, channelId, metadata);
        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "platform_order_id", result.getPlatformOrderId(),
                    "prepay_data", result.getPrepayData()
            ));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", result.getErrorMessage()
        ));
    }

    /** POST /api/v1/payments/verify */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> request) {
        String orderId = request.get("order_id");
        String channelId = request.get("channel_id");
        String receipt = request.get("receipt");

        PaymentChannel.VerifyResult result = paymentGateway.verifyAndComplete(orderId, channelId, receipt);
        if (result.isVerified()) {
            return ResponseEntity.ok(Map.of(
                    "verified", true,
                    "transaction_id", result.getTransactionId()
            ));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "verified", false,
                "error", "Payment verification failed"
        ));
    }

    /** POST /api/v1/payments/{orderId}/refund */
    @PostMapping("/{orderId}/refund")
    public ResponseEntity<?> refundPayment(
            @PathVariable String orderId,
            @RequestBody Map<String, Object> request) {
        long amount = ((Number) request.getOrDefault("amount", 0)).longValue();
        String reason = (String) request.getOrDefault("reason", "user_request");

        PaymentChannel.RefundResult result = paymentGateway.processRefund(orderId, amount, reason);
        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "refund_id", result.getRefundId()
            ));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", result.getErrorMessage()
        ));
    }
}
