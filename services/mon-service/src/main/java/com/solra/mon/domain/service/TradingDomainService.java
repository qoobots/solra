package com.solra.mon.domain.service;

import com.solra.mon.domain.entity.*;
import com.solra.mon.domain.repository.*;

/**
 * 交易领域服务。
 * 封装跨实体的购买交易流程：扣款→创建订单→发放物品→更新背包。
 */
public class TradingDomainService {

    private final WalletRepository walletRepository;
    private final OrderRepository orderRepository;
    private final VirtualItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;

    public TradingDomainService(WalletRepository walletRepository,
                                 OrderRepository orderRepository,
                                 VirtualItemRepository itemRepository,
                                 InventoryRepository inventoryRepository) {
        this.walletRepository = walletRepository;
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * 购买虚拟物品的完整交易流程。
     */
    public Order purchaseItem(String userId, String itemId, int quantity,
                               VirtualWallet.CurrencyType currency) {
        VirtualItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        if (!item.isAvailable()) {
            throw new IllegalStateException("Item is no longer available: " + itemId);
        }

        VirtualWallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(new VirtualWallet(userId)));

        long totalCost = item.getPrice() * quantity;

        // 1. 创建订单
        Order order = new Order(userId, currency);
        order.addItem(item.getItemId(), item.getName(), quantity, item.getPrice());

        // 2. 扣除余额
        wallet.deductBalance(currency, totalCost, "Purchase item: " + item.getName());
        walletRepository.save(wallet);

        // 3. 完成订单
        order.complete("internal", "txn-" + order.getOrderId());
        orderRepository.save(order);

        // 4. 发放物品到背包
        UserInventory inventory = inventoryRepository.findByUserId(userId)
                .orElseGet(() -> new UserInventory(userId));
        inventory.addItem(itemId, quantity, 0); // 0=永久
        inventoryRepository.save(inventory);

        return order;
    }

    /**
     * 退款流程。
     */
    public void refundOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        VirtualWallet wallet = walletRepository.findByUserId(order.getUserId())
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));

        order.refund();
        wallet.addBalance(order.getCurrency(), order.getTotalAmount(), "Refund for order: " + orderId);

        walletRepository.save(wallet);
        orderRepository.save(order);
    }
}
