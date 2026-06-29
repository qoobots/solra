package com.solra.mon.application.service;

import com.solra.mon.application.dto.*;
import com.solra.mon.domain.entity.*;
import com.solra.mon.domain.repository.*;
import com.solra.mon.domain.service.TradingDomainService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商业化应用服务。
 * 协调钱包、订单、订阅、虚拟物品的业务流程。
 */
public class MonApplicationService {

    private final WalletRepository walletRepository;
    private final OrderRepository orderRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final VirtualItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;
    private final TradingDomainService tradingService;

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
    }

    // ── 钱包 ──

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

    // ── 订单 ──

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

    // ── 订阅 ──

    public List<SubscriptionPlanDTO> listSubscriptionPlans() {
        List<SubscriptionPlanDTO> plans = new ArrayList<>();
        plans.add(createPlan(Subscription.SubscriptionTier.PREMIUM, "Premium",
                "高级会员，解锁全部功能", 2999, 29900, VirtualWallet.CurrencyType.DIAMOND,
                List.of("全部空间模板", "高级虚拟人定制", "优先客服支持", "无广告体验")));
        plans.add(createPlan(Subscription.SubscriptionTier.CREATOR, "Creator",
                "创作者会员，获得收益分成", 4999, 49900, VirtualWallet.CurrencyType.DIAMOND,
                List.of("Premium全部权益", "创作收益分成", "资产商店上架", "专属创作者徽章")));
        plans.add(createPlan(Subscription.SubscriptionTier.ENTERPRISE, "Enterprise",
                "企业版，团队协作与品牌空间", 9999, 99900, VirtualWallet.CurrencyType.DIAMOND,
                List.of("Creator全部权益", "品牌定制空间", "团队协作管理", "API接入", "专属技术支持")));
        return plans;
    }

    public SubscriptionDTO subscribe(String userId, String tierName, boolean yearly) {
        Subscription.SubscriptionTier tier = Subscription.SubscriptionTier.valueOf(tierName);
        long duration = yearly ? 365L * 24 * 3600 * 1000 : 30L * 24 * 3600 * 1000;
        long expireAt = System.currentTimeMillis() + duration;

        Subscription sub = new Subscription(userId, tier, expireAt, true);
        sub = subscriptionRepository.save(sub);
        return SubscriptionDTO.from(sub);
    }

    public SubscriptionDTO cancelSubscription(String userId, String reason) {
        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No active subscription"));
        sub.cancel(reason);
        sub = subscriptionRepository.save(sub);
        return SubscriptionDTO.from(sub);
    }

    // ── 虚拟物品 ──

    public List<VirtualItemDTO> listVirtualItems(String typeFilter, int page, int pageSize) {
        VirtualItem.VirtualItemType type = typeFilter != null ?
                VirtualItem.VirtualItemType.valueOf(typeFilter) : null;
        int offset = (page - 1) * pageSize;
        List<VirtualItem> items = itemRepository.findByType(type, offset, pageSize);
        return items.stream().map(VirtualItemDTO::from).collect(Collectors.toList());
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

    // ── 退款 ──

    public void refundOrder(String orderId) {
        tradingService.refundOrder(orderId);
    }

    private SubscriptionPlanDTO createPlan(Subscription.SubscriptionTier tier, String name,
                                            String description, long priceMonth, long priceYear,
                                            VirtualWallet.CurrencyType currency, List<String> benefits) {
        SubscriptionPlanDTO plan = new SubscriptionPlanDTO();
        plan.setTier(tier.name());
        plan.setName(name);
        plan.setDescription(description);
        plan.setPricePerMonth(priceMonth);
        plan.setPricePerYear(priceYear);
        plan.setCurrency(currency.name());
        plan.setBenefits(benefits);
        return plan;
    }
}
