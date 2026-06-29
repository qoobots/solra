package com.solra.mon.interfaces.rest;

import com.solra.mon.application.dto.OrderDTO;
import com.solra.mon.application.dto.UserInventoryDTO;
import com.solra.mon.application.dto.VirtualItemDTO;
import com.solra.mon.application.service.MonApplicationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 虚拟物品 REST 控制器。
 */
@RestController
@RequestMapping("/api/v1/items")
public class VirtualItemController {

    private final MonApplicationService monService;

    public VirtualItemController(MonApplicationService monService) {
        this.monService = monService;
    }

    /**
     * GET /api/v1/items?type=AVATAR_SKIN&page=1&page_size=20
     */
    @GetMapping
    public ResponseEntity<List<VirtualItemDTO>> listItems(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(monService.listVirtualItems(type, page, pageSize));
    }

    /**
     * POST /api/v1/items/purchase
     */
    @PostMapping("/purchase")
    public ResponseEntity<OrderDTO> purchaseItem(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("user_id");
        String itemId = (String) request.get("item_id");
        int quantity = ((Number) request.getOrDefault("quantity", 1)).intValue();
        String currency = (String) request.getOrDefault("currency", "DIAMOND");
        return ResponseEntity.ok(monService.purchaseItem(userId, itemId, quantity, currency));
    }

    /**
     * GET /api/v1/items/inventory/{userId}
     */
    @GetMapping("/inventory/{userId}")
    public ResponseEntity<UserInventoryDTO> getInventory(@PathVariable String userId) {
        return ResponseEntity.ok(monService.getUserInventory(userId));
    }
}
