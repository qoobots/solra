package com.solra.mon.application.service;

import com.solra.mon.application.dto.*;
import com.solra.mon.domain.entity.*;
import com.solra.mon.domain.repository.*;
import com.solra.mon.domain.service.ItemShopService;
import com.solra.mon.domain.service.SubscriptionDomainService;
import com.solra.mon.domain.service.TradingDomainService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 商业化应用服务。
 * 协调钱包、订单、订阅、虚拟物品的业务流程。
 *
 * <p>MON-001: Solra Plus 四档订阅 — Free/Plus(¥19)/Pro(¥49)/Ultra(¥99)，年付8折。
 * <p>MON-002: 虚拟物品商店 — 分类浏览、搜索、热门推荐、7天退款。
 */
public class MonApplicationService {

    private final WalletRepository walletRepository;
    private final OrderRepository orderRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final VirtualItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;
    private final TradingDomainService tradingService;
    private final SubscriptionDomainService subscriptionService;
    private final ItemShopService itemShopService;

    public MonApplicationService(WalletRepository walletRepository,
                                  OrderRepository orderRepository,
                                  SubscriptionRepository subscriptionRepository,
                                  VirtualItemRepository itemRepository,
                                  InventoryRepository inventoryRepository) {
        this.walletRepository = walletRepository;
        this.orderRepository = orderRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.itemRepository = itemRepository;
        this.inventoryRepository = inventoryRepository;
        this.tradingService = new TradingDomainService(
                walletRepository, orderRepository, itemRepository, inventoryRepository);
        this.subscriptionService = new SubscriptionDomainService(subscriptionRepository);
        this.itemShopService = new ItemShopService(
                itemRepository, inventoryRepository, orderRepository);
    }

    // ═══════════════════════════════════════════════════════════════
    // 钱包
    // ═══════════════════════════════════════════════════════════════

    public WalletDTO getWallet(String userId) {
        VirtualWallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(new VirtualWallet(userId)));
        return WalletDTO.from(wallet);
    }

    public WalletDTO addBalance(String userId, String currency, long amount, String reason) {
        VirtualWallet.CurrencyType ct = VirtualWallet.CurrencyType.valueOf(currency);
        VirtualWallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(new VirtualWallet(userId)));
        wallet.addBalance(ct, amount, reason);
        wallet = walletRepository.save(wallet);
        return WalletDTO.from(wallet);
    }

    public WalletDTO deductBalance(String userId, String currency, long amount, String reason) {
        VirtualWallet.CurrencyType ct = VirtualWallet.CurrencyType.valueOf(currency);
        VirtualWallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        wallet.deductBalance(ct, amount, reason);
        wallet = walletRepository.save(wallet);
        return WalletDTO.from(wallet);
    }

    // ═══════════════════════════════════════════════════════════════
    // 订单
    // ═══════════════════════════════════════════════════════════════

    public OrderDTO createOrder(String userId, String currency) {
        VirtualWallet.CurrencyType ct = VirtualWallet.CurrencyType.valueOf(currency);
        Order order = new Order(userId, ct);
        order = orderRepository.save(order);
        return OrderDTO.from(order);
    }

    public OrderDTO getOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return OrderDTO.from(order);
    }

    // ═══════════════════════════════════════════════════════════════
    // 订阅 — MON-001 Solra Plus 四档分档
    // ═══════════════════════════════════════════════════════════════

    /** 列出全部订阅计划（Free/Plus/Pro/Ultra）。 */
    public List<SubscriptionPlanDTO> listSubscriptionPlans() {
        return subscriptionService.listPlans().stream()
                .map(this::toPlanDTO)
                .collect(Collectors.toList());
    }

    /** 查看用户当前订阅。 */
    public SubscriptionDTO getUserSubscription(String userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(SubscriptionDTO::from)
                .orElse(null);
    }

    /** 新订阅或重新订阅。 */
    public SubscriptionDTO subscribe(String userId, String tierName, String cycleName) {
        Subscription.SubscriptionTier tier = Subscription.SubscriptionTier.valueOf(tierName);
        Subscription.BillingCycle cycle = cycleName != null
                ? Subscription.BillingCycle.valueOf(cycleName)
                : Subscription.BillingCycle.MONTHLY;

        // 检查是否已有活跃订阅
        Optional<Subscription> existing = subscriptionRepository.findByUserId(userId);
        if (existing.isPresent() && existing.get().isActive()) {
            throw new IllegalStateException("User already has an active subscription. Use upgrade/downgrade instead.");
        }

        Subscription sub = subscriptionService.createSubscription(userId, tier, cycle);
        return SubscriptionDTO.from(sub);
    }

    /** 升级订阅（立即生效，按比例折算差价）。 */
    public SubscriptionDTO upgradeSubscription(String userId, String newTierName, String cycleName) {
        Subscription current = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No subscription found"));

        Subscription.SubscriptionTier newTier = Subscription.SubscriptionTier.valueOf(newTierName);
        Subscription.BillingCycle cycle = cycleName != null
                ? Subscription.BillingCycle.valueOf(cycleName)
                : current.getBillingCycle();

        long upgradePrice = subscriptionService.calculateUpgradePrice(current, newTier, cycle);
        Subscription upgraded = subscriptionService.upgradeSubscription(current, newTier, cycle);
        return SubscriptionDTO.from(upgraded);
    }

    /** 降级订阅（下一周期生效）。 */
    public SubscriptionDTO downgradeSubscription(String userId, String newTierName) {
        Subscription current = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No subscription found"));

        Subscription.SubscriptionTier newTier = Subscription.SubscriptionTier.valueOf(newTierName);
        Subscription downgraded = subscriptionService.downgradeSubscription(current, newTier);
        return SubscriptionDTO.from(downgraded);
    }

    /** 续费订阅。 */
    public SubscriptionDTO renewSubscription(String userId, String cycleName) {
        Subscription current = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No subscription found"));

        Subscription.BillingCycle cycle = cycleName != null
                ? Subscription.BillingCycle.valueOf(cycleName)
                : current.getBillingCycle();

        Subscription renewed = subscriptionService.renewSubscription(current, cycle);
        return SubscriptionDTO.from(renewed);
    }

    /** 取消订阅。 */
    public SubscriptionDTO cancelSubscription(String userId, String reason) {
        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No active subscription"));
        sub.cancel(reason);
        sub = subscriptionRepository.save(sub);
        return SubscriptionDTO.from(sub);
    }

    /** 计算升级差价（用于前端展示）。 */
    public UpgradePreviewDTO previewUpgrade(String userId, String newTierName, String cycleName) {
        Subscription current = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No subscription found"));

        Subscription.SubscriptionTier newTier = Subscription.SubscriptionTier.valueOf(newTierName);
        Subscription.BillingCycle cycle = cycleName != null
                ? Subscription.BillingCycle.valueOf(cycleName)
                : current.getBillingCycle();

        long upgradePrice = subscriptionService.calculateUpgradePrice(current, newTier, cycle);

        UpgradePreviewDTO preview = new UpgradePreviewDTO();
        preview.setCurrentTier(current.getTier().name());
        preview.setTargetTier(newTier.name());
        preview.setUpgradePrice(upgradePrice);
        preview.setCurrency("DIAMOND");
        return preview;
    }

    // ═══════════════════════════════════════════════════════════════
    // 虚拟物品 — MON-002 虚拟物品商店
    // ═══════════════════════════════════════════════════════════════

    /** 分类浏览上架商品，支持排序。 */
    public List<VirtualItemDTO> browseItems(String typeFilter, String sortBy, int page, int pageSize) {
        VirtualItem.VirtualItemType type = typeFilter != null ?
                VirtualItem.VirtualItemType.valueOf(typeFilter) : null;
        int offset = (page - 1) * pageSize;
        List<VirtualItem> items = itemShopService.browseItems(type, sortBy, offset, pageSize);
        return items.stream().map(VirtualItemDTO::from).collect(Collectors.toList());
    }

    /** 关键词搜索。 */
    public List<VirtualItemDTO> searchItems(String keyword, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<VirtualItem> items = itemShopService.searchItems(keyword, offset, pageSize);
        return items.stream().map(VirtualItemDTO::from).collect(Collectors.toList());
    }

    /** 热门推荐 Top N。 */
    public List<VirtualItemDTO> getHotItems(int limit) {
        return itemShopService.getHotItems(limit).stream()
                .map(VirtualItemDTO::from).collect(Collectors.toList());
    }

    /** 商品详情。 */
    public VirtualItemDTO getItemDetail(String itemId) {
        return itemShopService.getItemDetail(itemId)
                .map(VirtualItemDTO::from).orElse(null);
    }

    /** 兼容旧接口：列出虚拟物品。 */
    public List<VirtualItemDTO> listVirtualItems(String typeFilter, int page, int pageSize) {
        return browseItems(typeFilter, "newest", page, pageSize);
    }

    public OrderDTO purchaseItem(String userId, String itemId, int quantity, String currency) {
        VirtualWallet.CurrencyType ct = VirtualWallet.CurrencyType.valueOf(currency);
        Order order = tradingService.purchaseItem(userId, itemId, quantity, ct);
        return OrderDTO.from(order);
    }

    public UserInventoryDTO getUserInventory(String userId) {
        UserInventory inventory = inventoryRepository.findByUserId(userId)
                .orElseGet(() -> new UserInventory(userId));
        return UserInventoryDTO.from(inventory);
    }

    /** 物品退款（7天未使用可退）。 */
    public void refundItem(String userId, String itemId) {
        if (!itemShopService.canRefund(userId, itemId)) {
            throw new IllegalStateException("Item not eligible for refund");
        }

        VirtualItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        // 退款：退还余额 + 从背包移除
        VirtualWallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        wallet.addBalance(item.getCurrency(), item.getPrice(), "Refund for item: " + itemId);
        walletRepository.save(wallet);

        itemShopService.refundItem(userId, itemId);
    }

    // ═══════════════════════════════════════════════════════════════
    // 退款（订单级）
    // ═══════════════════════════════════════════════════════════════

    public void refundOrder(String orderId) {
        tradingService.refundOrder(orderId);
    }

    // ═══════════════════════════════════════════════════════════════
    // 私有辅助
    // ═══════════════════════════════════════════════════════════════

    private SubscriptionPlanDTO toPlanDTO(SubscriptionPlan plan) {
        SubscriptionPlanDTO dto = new SubscriptionPlanDTO();
        dto.setTier(plan.getTier().name());
        dto.setName(plan.getName());
        dto.setDescription(plan.getDescription());
        dto.setPricePerMonth(plan.getPriceMonthly());
        dto.setPricePerYear(plan.getPriceYearly());
        dto.setCurrency(plan.getCurrency().name());
        dto.setBenefits(plan.getBenefits());
        dto.setYearlyAvailable(plan.isYearlyAvailable());
        return dto;
    }
}
