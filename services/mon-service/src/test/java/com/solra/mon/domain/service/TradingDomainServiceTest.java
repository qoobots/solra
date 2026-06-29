package com.solra.mon.domain.service;

import com.solra.mon.domain.entity.*;
import com.solra.mon.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TradingDomainService 单元测试。
 * MON-001 购买交易 + MON-002 退款流程核心业务逻辑。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TradingDomainService — 交易领域服务测试")
class TradingDomainServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private VirtualItemRepository itemRepository;
    @Mock private InventoryRepository inventoryRepository;

    private TradingDomainService service;

    @BeforeEach
    void setUp() {
        service = new TradingDomainService(walletRepository, orderRepository,
                itemRepository, inventoryRepository);
    }

    @Nested
    @DisplayName("purchaseItem — 购买物品")
    class PurchaseItem {

        @Test
        @DisplayName("正常购买流程：扣款→创建订单→完成→发放背包")
        void normalPurchase() {
            VirtualItem item = new VirtualItem("item-1", "Sword Skin",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 100L,
                    VirtualWallet.CurrencyType.GOLD);
            VirtualWallet wallet = new VirtualWallet("user-1");
            wallet.addBalance(VirtualWallet.CurrencyType.GOLD, 500L, "init");
            UserInventory inventory = new UserInventory("user-1");

            when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));
            when(walletRepository.findByUserId("user-1")).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any(VirtualWallet.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryRepository.findByUserId("user-1")).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(UserInventory.class))).thenAnswer(inv -> inv.getArgument(0));

            Order order = service.purchaseItem("user-1", "item-1", 1,
                    VirtualWallet.CurrencyType.GOLD);

            assertNotNull(order);
            assertEquals(Order.OrderStatus.COMPLETED, order.getStatus());
            assertEquals(100L, order.getTotalAmount());
            assertEquals(400L, wallet.getBalance(VirtualWallet.CurrencyType.GOLD));
            assertTrue(inventory.hasItem("item-1"));
        }

        @Test
        @DisplayName("购买多个数量")
        void purchaseMultipleQuantity() {
            VirtualItem item = new VirtualItem("item-1", "Gift Box",
                    VirtualItem.VirtualItemType.GIFT, 50L,
                    VirtualWallet.CurrencyType.DIAMOND);
            VirtualWallet wallet = new VirtualWallet("user-1");
            wallet.addBalance(VirtualWallet.CurrencyType.DIAMOND, 300L, "init");
            UserInventory inventory = new UserInventory("user-1");

            when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));
            when(walletRepository.findByUserId("user-1")).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any(VirtualWallet.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryRepository.findByUserId("user-1")).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(UserInventory.class))).thenAnswer(inv -> inv.getArgument(0));

            Order order = service.purchaseItem("user-1", "item-1", 3,
                    VirtualWallet.CurrencyType.DIAMOND);

            assertEquals(150L, order.getTotalAmount()); // 50*3
            assertEquals(150L, wallet.getBalance(VirtualWallet.CurrencyType.DIAMOND));
        }

        @Test
        @DisplayName("用户无钱包时自动创建")
        void autoCreateWallet() {
            VirtualItem item = new VirtualItem("item-1", "Badge",
                    VirtualItem.VirtualItemType.BADGE, 50L,
                    VirtualWallet.CurrencyType.GOLD);
            UserInventory inventory = new UserInventory("user-1");

            when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));
            when(walletRepository.findByUserId("user-1")).thenReturn(Optional.empty());
            // 钱包不存在时应自动创建（orElseGet 路径），但余额不足会抛异常
            // 这里验证的是自动创建逻辑被触发
            when(walletRepository.save(any(VirtualWallet.class))).thenAnswer(inv -> inv.getArgument(0));

            // 自动创建的钱包余额为0，购买50 GOLD会失败
            assertThrows(IllegalStateException.class,
                    () -> service.purchaseItem("user-1", "item-1", 1,
                            VirtualWallet.CurrencyType.GOLD));
        }

        @Test
        @DisplayName("物品不存在时抛 IllegalArgumentException")
        void itemNotFoundThrows() {
            when(itemRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.purchaseItem("user-1", "non-existent", 1,
                            VirtualWallet.CurrencyType.GOLD));
        }

        @Test
        @DisplayName("限量物品已过期时抛 IllegalStateException")
        void limitedItemExpiredThrows() {
            VirtualItem item = new VirtualItem("item-1", "Limited Skin",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 100L,
                    VirtualWallet.CurrencyType.GOLD);
            item.setLimited(true);
            item.setAvailableUntil(System.currentTimeMillis() - 86400000L); // 1天前过期

            when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));

            assertThrows(IllegalStateException.class,
                    () -> service.purchaseItem("user-1", "item-1", 1,
                            VirtualWallet.CurrencyType.GOLD));
        }

        @Test
        @DisplayName("余额不足时抛 IllegalStateException")
        void insufficientBalanceThrows() {
            VirtualItem item = new VirtualItem("item-1", "Expensive Skin",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 1000L,
                    VirtualWallet.CurrencyType.GOLD);
            VirtualWallet wallet = new VirtualWallet("user-1");
            wallet.addBalance(VirtualWallet.CurrencyType.GOLD, 100L, "init");

            when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));
            when(walletRepository.findByUserId("user-1")).thenReturn(Optional.of(wallet));

            assertThrows(IllegalStateException.class,
                    () -> service.purchaseItem("user-1", "item-1", 1,
                            VirtualWallet.CurrencyType.GOLD));
        }

        @Test
        @DisplayName("背包不存在时自动创建")
        void autoCreateInventory() {
            VirtualItem item = new VirtualItem("item-1", "Badge",
                    VirtualItem.VirtualItemType.BADGE, 10L,
                    VirtualWallet.CurrencyType.FAITH_POINT);
            VirtualWallet wallet = new VirtualWallet("user-1");
            wallet.addBalance(VirtualWallet.CurrencyType.FAITH_POINT, 100L, "init");

            when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));
            when(walletRepository.findByUserId("user-1")).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any(VirtualWallet.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryRepository.findByUserId("user-1")).thenReturn(Optional.empty());
            when(inventoryRepository.save(any(UserInventory.class))).thenAnswer(inv -> inv.getArgument(0));

            Order order = service.purchaseItem("user-1", "item-1", 1,
                    VirtualWallet.CurrencyType.FAITH_POINT);

            assertEquals(Order.OrderStatus.COMPLETED, order.getStatus());
            verify(inventoryRepository).save(any(UserInventory.class));
        }

        @Test
        @DisplayName("使用不同币种购买")
        void purchaseWithDifferentCurrency() {
            VirtualItem item = new VirtualItem("item-1", "Diamond Item",
                    VirtualItem.VirtualItemType.GIFT, 50L,
                    VirtualWallet.CurrencyType.DIAMOND);
            VirtualWallet wallet = new VirtualWallet("user-1");
            wallet.addBalance(VirtualWallet.CurrencyType.DIAMOND, 200L, "init");
            UserInventory inventory = new UserInventory("user-1");

            when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));
            when(walletRepository.findByUserId("user-1")).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any(VirtualWallet.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryRepository.findByUserId("user-1")).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(UserInventory.class))).thenAnswer(inv -> inv.getArgument(0));

            Order order = service.purchaseItem("user-1", "item-1", 1,
                    VirtualWallet.CurrencyType.DIAMOND);

            assertEquals(VirtualWallet.CurrencyType.DIAMOND, order.getCurrency());
            assertEquals(150L, wallet.getBalance(VirtualWallet.CurrencyType.DIAMOND));
        }
    }

    @Nested
    @DisplayName("refundOrder — 退款")
    class RefundOrder {

        @Test
        @DisplayName("正常退款：COMPLETED → REFUNDED")
        void normalRefund() {
            VirtualWallet wallet = new VirtualWallet("user-1");
            wallet.addBalance(VirtualWallet.CurrencyType.GOLD, 100L, "init");

            Order order = new Order("user-1", VirtualWallet.CurrencyType.GOLD);
            order.addItem("item-1", "Test", 1, 100L);
            order.complete("internal", "txn-001");

            when(orderRepository.findById(order.getOrderId())).thenReturn(Optional.of(order));
            when(walletRepository.findByUserId("user-1")).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any(VirtualWallet.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            service.refundOrder(order.getOrderId());

            assertEquals(Order.OrderStatus.REFUNDED, order.getStatus());
            assertEquals(200L, wallet.getBalance(VirtualWallet.CurrencyType.GOLD));
        }

        @Test
        @DisplayName("订单不存在时抛 IllegalArgumentException")
        void orderNotFoundThrows() {
            when(orderRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.refundOrder("non-existent"));
        }

        @Test
        @DisplayName("钱包不存在时抛 IllegalStateException")
        void walletNotFoundThrows() {
            Order order = new Order("user-1", VirtualWallet.CurrencyType.GOLD);
            order.complete("internal", "txn-001");

            when(orderRepository.findById(order.getOrderId())).thenReturn(Optional.of(order));
            when(walletRepository.findByUserId("user-1")).thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class,
                    () -> service.refundOrder(order.getOrderId()));
        }

        @Test
        @DisplayName("非 COMPLETED 状态退款抛异常")
        void nonCompletedRefundThrows() {
            VirtualWallet wallet = new VirtualWallet("user-1");
            Order order = new Order("user-1", VirtualWallet.CurrencyType.GOLD);
            // order is PENDING

            when(orderRepository.findById(order.getOrderId())).thenReturn(Optional.of(order));
            when(walletRepository.findByUserId("user-1")).thenReturn(Optional.of(wallet));

            assertThrows(IllegalStateException.class,
                    () -> service.refundOrder(order.getOrderId()));
        }
    }
}
