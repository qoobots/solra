package com.solra.mon.infrastructure.config;

import com.solra.mon.application.service.MonApplicationService;
import com.solra.mon.domain.repository.*;
import com.solra.mon.infrastructure.persistence.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MON 服务 Spring 配置。
 */
@Configuration
public class MonServiceConfiguration {

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
