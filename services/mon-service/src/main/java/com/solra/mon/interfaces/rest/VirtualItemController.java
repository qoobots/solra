package com.solra.mon.interfaces.rest;

import com.solra.mon.application.dto.OrderDTO;
import com.solra.mon.application.dto.UserInventoryDTO;
import com.solra.mon.application.dto.VirtualItemDTO;
import com.solra.mon.application.service.MonApplicationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 虚拟物品 REST 控制器 — MON-002 虚拟物品商店。
 *
 * <p>端点一览：
 * <ul>
 *   <li>GET  /api/v1/items?type=&sort=&page=&page_size=  — 分类浏览</li>
 *   <li>GET  /api/v1/items/search?q=&page=&page_size=     — 关键词搜索</li>
 *   <li>GET  /api/v1/items/hot?limit=                     — 热门推荐</li>
 *   <li>GET  /api/v1/items/{itemId}                        — 商品详情</li>
 *   <li>POST /api/v1/items/purchase                        — 购买</li>
 *   <li>POST /api/v1/items/{itemId}/refund                  — 退款</li>
 *   <li>GET  /api/v1/items/inventory/{userId}               — 我的背包</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/items")
public class VirtualItemController {

    private final MonApplicationService monService;

    public VirtualItemController(MonApplicationService monService) {
        this.monService = monService;
    }

    /** GET /api/v1/items?type=AVATAR_SKIN&sort=popular&page=1&page_size=20 */
    @GetMapping
    public ResponseEntity<List<VirtualItemDTO>> listItems(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "sort", defaultValue = "newest") String sort,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(monService.browseItems(type, sort, page, pageSize));
    }

    /** GET /api/v1/items/search?q=关键词&page=1&page_size=20 */
    @GetMapping("/search")
    public ResponseEntity<List<VirtualItemDTO>> searchItems(
            @RequestParam(value = "q") String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(monService.searchItems(keyword, page, pageSize));
    }

    /** GET /api/v1/items/hot?limit=10 */
    @GetMapping("/hot")
    public ResponseEntity<List<VirtualItemDTO>> hotItems(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(monService.getHotItems(limit));
    }

    /** GET /api/v1/items/{itemId} */
    @GetMapping("/{itemId}")
    public ResponseEntity<VirtualItemDTO> getItemDetail(@PathVariable String itemId) {
        VirtualItemDTO item = monService.getItemDetail(itemId);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    /** POST /api/v1/items/purchase */
    @PostMapping("/purchase")
    public ResponseEntity<OrderDTO> purchaseItem(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("user_id");
        String itemId = (String) request.get("item_id");
        int quantity = ((Number) request.getOrDefault("quantity", 1)).intValue();
        String currency = (String) request.getOrDefault("currency", "DIAMOND");
        return ResponseEntity.ok(monService.purchaseItem(userId, itemId, quantity, currency));
    }

    /** POST /api/v1/items/{itemId}/refund */
    @PostMapping("/{itemId}/refund")
    public ResponseEntity<Map<String, Object>> refundItem(
            @PathVariable String itemId,
            @RequestBody Map<String, String> request) {
        String userId = request.get("user_id");
        monService.refundItem(userId, itemId);
        return ResponseEntity.ok(Map.of("status", "refunded", "item_id", itemId));
    }

    /** GET /api/v1/items/inventory/{userId} */
    @GetMapping("/inventory/{userId}")
    public ResponseEntity<UserInventoryDTO> getInventory(@PathVariable String userId) {
        return ResponseEntity.ok(monService.getUserInventory(userId));
    }
}
