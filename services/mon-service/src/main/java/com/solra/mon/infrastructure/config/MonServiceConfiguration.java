package com.solra.mon.infrastructure.config;

import com.solra.mon.application.service.MonApplicationService;
import com.solra.mon.domain.repository.*;
import com.solra.mon.domain.service.BrandCommercialService;
import com.solra.mon.domain.service.CreatorMarketplaceService;
import com.solra.mon.domain.service.ReconciliationService;
import com.solra.mon.domain.service.SubscriptionAdminService;
import com.solra.mon.domain.service.SubscriptionDomainService;
import com.solra.mon.infrastructure.payment.*;
import com.solra.mon.infrastructure.persistence.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MON 服务 Spring 配置。
 */
@Configuration
public class MonServiceConfiguration {

    // ── 仓储 ──

    @Bean
    public WalletRepository walletRepository() {
        return new InMemoryWalletRepository();
    }

    @Bean
    public OrderRepository orderRepository() {
        return new InMemoryOrderRepository();
    }

    @Bean
    public SubscriptionRepository subscriptionRepository() {
        return new InMemorySubscriptionRepository();
    }

    @Bean
    public VirtualItemRepository virtualItemRepository() {
        return new InMemoryVirtualItemRepository();
    }

    @Bean
    public InventoryRepository inventoryRepository() {
        return new InMemoryInventoryRepository();
    }

    @Bean
    public PaymentTransactionRepository paymentTransactionRepository() {
        return new InMemoryPaymentTransactionRepository();
    }

    // ── 领域服务 — MON-001 / MON-003 / MON-006 ──

    @Bean
    public SubscriptionDomainService subscriptionDomainService(
            SubscriptionRepository subscriptionRepository) {
        return new SubscriptionDomainService(subscriptionRepository);
    }

    @Bean
    public SubscriptionAdminService subscriptionAdminService(
            SubscriptionRepository subscriptionRepository,
            SubscriptionDomainService subscriptionDomainService) {
        return new SubscriptionAdminService(subscriptionRepository, subscriptionDomainService);
    }

    @Bean
    public CreatorMarketplaceService creatorMarketplaceService(
            VirtualItemRepository virtualItemRepository) {
        return new CreatorMarketplaceService(virtualItemRepository);
    }

    @Bean
    public ReconciliationService reconciliationService(
            OrderRepository orderRepository,
            PaymentTransactionRepository txnRepository,
            CreatorMarketplaceService creatorMarketplaceService) {
        return new ReconciliationService(orderRepository, txnRepository, creatorMarketplaceService);
    }

    @Bean
    public BrandCommercialService brandCommercialService() {
        return new BrandCommercialService();
    }

    // ── 支付网关 — MON-004 ──

    @Bean
    public PaymentGateway paymentGateway(OrderRepository orderRepository,
                                          PaymentTransactionRepository txnRepository) {
        PaymentGateway gateway = new PaymentGateway(orderRepository, txnRepository);

        // 注册四渠道路由
        gateway.registerChannel(new AppleIAPProvider());
        gateway.registerChannel(new GooglePlayProvider());
        gateway.registerChannel(new WeChatPayProvider());
        gateway.registerChannel(new AlipayProvider());

        return gateway;
    }

    // ── 应用服务 ──

    @Bean
    public MonApplicationService monApplicationService(
            WalletRepository walletRepository,
            OrderRepository orderRepository,
            SubscriptionRepository subscriptionRepository,
            VirtualItemRepository virtualItemRepository,
            InventoryRepository inventoryRepository) {
        return new MonApplicationService(walletRepository, orderRepository,
                subscriptionRepository, virtualItemRepository, inventoryRepository);
    }
}
