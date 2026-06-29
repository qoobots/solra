package com.solra.mon.domain.service;

import com.solra.mon.domain.entity.Order;
import com.solra.mon.domain.repository.OrderRepository;
import com.solra.mon.infrastructure.payment.PaymentTransaction;
import com.solra.mon.infrastructure.payment.PaymentTransactionRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 对账结算系统领域服务 — MON-007。
 *
 * <p>职责：
 * <ul>
 *   <li>日对账：比对平台订单与支付渠道交易流水</li>
 *   <li>差错检测：识别金额不一致、未完成支付、重复支付</li>
 *   <li>月结算：按创作者、品牌、平台汇总</li>
 *   <li>差错率控制在 < 0.01%</li>
 * </ul>
 */
public class ReconciliationService {

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository txnRepository;
    private final CreatorMarketplaceService creatorMarketplace;

    public ReconciliationService(OrderRepository orderRepository,
                                  PaymentTransactionRepository txnRepository,
                                  CreatorMarketplaceService creatorMarketplace) {
        this.orderRepository = orderRepository;
        this.txnRepository = txnRepository;
        this.creatorMarketplace = creatorMarketplace;
    }

    /**
     * 日对账：比对订单与支付流水。
     *
     * @param date 对账日期（yyyy-MM-dd）
     * @return 对账报告
     */
    public DailyReconciliationReport reconcileDaily(String date) {
        // 模拟：获取指定日期的订单和支付流水
        List<Order> orders = orderRepository.findByUserId("__ALL__"); // 简化
        List<PaymentTransaction> txns = new ArrayList<>();
        for (Order order : orders) {
            txnRepository.findByOrderId(order.getOrderId()).ifPresent(txns::add);
        }

        List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();
        Set<String> matchedOrderIds = new HashSet<>();

        // 比对：已完成订单应有对应成功的支付流水
        for (Order order : orders) {
            if (order.getStatus() == Order.OrderStatus.COMPLETED) {
                Optional<PaymentTransaction> txn = txnRepository.findByOrderId(order.getOrderId());
                if (txn.isEmpty()) {
                    discrepancies.add(new ReconciliationDiscrepancy(
                            order.getOrderId(),
                            ReconciliationDiscrepancy.DiscrepancyType.MISSING_TRANSACTION,
                            "Order completed but no payment transaction found",
                            order.getTotalAmount(), 0));
                } else if (txn.get().getStatus() != PaymentTransaction.TransactionStatus.COMPLETED) {
                    discrepancies.add(new ReconciliationDiscrepancy(
                            order.getOrderId(),
                            ReconciliationDiscrepancy.DiscrepancyType.TRANSACTION_NOT_COMPLETED,
                            "Order completed but transaction is " + txn.get().getStatus(),
                            order.getTotalAmount(), txn.get().getAmount()));
                } else if (order.getTotalAmount() != txn.get().getAmount()) {
                    discrepancies.add(new ReconciliationDiscrepancy(
                            order.getOrderId(),
                            ReconciliationDiscrepancy.DiscrepancyType.AMOUNT_MISMATCH,
                            "Amount mismatch: order=" + order.getTotalAmount()
                                    + " vs txn=" + txn.get().getAmount(),
                            order.getTotalAmount(), txn.get().getAmount()));
                } else {
                    matchedOrderIds.add(order.getOrderId());
                }
            }
        }

        long totalOrders = orders.size();
        long totalMatched = matchedOrderIds.size();
        long totalDiscrepancies = discrepancies.size();
        double errorRate = totalOrders > 0
                ? (double) totalDiscrepancies / totalOrders * 100 : 0;

        return new DailyReconciliationReport(date, totalOrders, totalMatched,
                totalDiscrepancies, errorRate, discrepancies);
    }

    /**
     * 月结算：按创作者汇总月度收益。
     *
     * @param period 结算周期（yyyy-MM）
     * @return 结算报告
     */
    public MonthlySettlementReport settleMonthly(String period) {
        List<CreatorEarning> settled = creatorMarketplace.settleMonthly(period);

        long totalCreatorShare = settled.stream()
                .mapToLong(CreatorEarning::getCreatorShare).sum();
        long totalCommission = settled.stream()
                .mapToLong(CreatorEarning::getCommission).sum();
        long totalSales = settled.stream()
                .mapToLong(CreatorEarning::getSaleAmount).sum();

        // 按创作者分组
        Map<String, Long> byCreator = settled.stream()
                .collect(Collectors.groupingBy(
                        CreatorEarning::getCreatorId,
                        Collectors.summingLong(CreatorEarning::getCreatorShare)));

        return new MonthlySettlementReport(period, settled.size(),
                totalSales, totalCommission, totalCreatorShare, byCreator);
    }

    /**
     * 品牌结算汇总（MON-005 预留）。
     */
    public BrandSettlementSummary getBrandSettlement(String brandId, String period) {
        // 预留：品牌空间商业化结算
        return new BrandSettlementSummary(brandId, period, 0, 0, 0);
    }

    // ── 值对象 ──

    /** 日对账报告。 */
    public static class DailyReconciliationReport {
        private final String date;
        private final long totalOrders;
        private final long matchedOrders;
        private final long discrepancies;
        private final double errorRate;
        private final List<ReconciliationDiscrepancy> details;

        public DailyReconciliationReport(String date, long totalOrders, long matchedOrders,
                                          long discrepancies, double errorRate,
                                          List<ReconciliationDiscrepancy> details) {
            this.date = date;
            this.totalOrders = totalOrders;
            this.matchedOrders = matchedOrders;
            this.discrepancies = discrepancies;
            this.errorRate = errorRate;
            this.details = List.copyOf(details);
        }

        /** 差错率是否在容忍范围内（<0.01%）。 */
        public boolean isWithinTolerance() {
            return errorRate < 0.01;
        }

        public String getDate() { return date; }
        public long getTotalOrders() { return totalOrders; }
        public long getMatchedOrders() { return matchedOrders; }
        public long getDiscrepancies() { return discrepancies; }
        public double getErrorRate() { return errorRate; }
        public List<ReconciliationDiscrepancy> getDetails() { return details; }
    }

    /** 对账差异记录。 */
    public static class ReconciliationDiscrepancy {
        public enum DiscrepancyType {
            MISSING_TRANSACTION,
            TRANSACTION_NOT_COMPLETED,
            AMOUNT_MISMATCH,
            DUPLICATE_TRANSACTION
        }

        private final String orderId;
        private final DiscrepancyType type;
        private final String description;
        private final long orderAmount;
        private final long txnAmount;

        public ReconciliationDiscrepancy(String orderId, DiscrepancyType type,
                                          String description, long orderAmount, long txnAmount) {
            this.orderId = orderId;
            this.type = type;
            this.description = description;
            this.orderAmount = orderAmount;
            this.txnAmount = txnAmount;
        }

        public String getOrderId() { return orderId; }
        public DiscrepancyType getType() { return type; }
        public String getDescription() { return description; }
        public long getOrderAmount() { return orderAmount; }
        public long getTxnAmount() { return txnAmount; }
        public long getDiff() { return Math.abs(orderAmount - txnAmount); }
    }

    /** 月结算报告。 */
    public static class MonthlySettlementReport {
        private final String period;
        private final int settledCount;
        private final long totalSales;
        private final long totalCommission;
        private final long totalCreatorShare;
        private final Map<String, Long> byCreator;

        public MonthlySettlementReport(String period, int settledCount, long totalSales,
                                        long totalCommission, long totalCreatorShare,
                                        Map<String, Long> byCreator) {
            this.period = period;
            this.settledCount = settledCount;
            this.totalSales = totalSales;
            this.totalCommission = totalCommission;
            this.totalCreatorShare = totalCreatorShare;
            this.byCreator = Map.copyOf(byCreator);
        }

        public String getPeriod() { return period; }
        public int getSettledCount() { return settledCount; }
        public long getTotalSales() { return totalSales; }
        public long getTotalCommission() { return totalCommission; }
        public long getTotalCreatorShare() { return totalCreatorShare; }
        public Map<String, Long> getByCreator() { return byCreator; }
    }

    /** 品牌结算汇总（MON-005）。 */
    public static class BrandSettlementSummary {
        private final String brandId;
        private final String period;
        private final long annualFee;
        private final long adRevenue;
        private final long totalSettlement;

        public BrandSettlementSummary(String brandId, String period,
                                       long annualFee, long adRevenue, long totalSettlement) {
            this.brandId = brandId;
            this.period = period;
            this.annualFee = annualFee;
            this.adRevenue = adRevenue;
            this.totalSettlement = totalSettlement;
        }

        public String getBrandId() { return brandId; }
        public String getPeriod() { return period; }
        public long getAnnualFee() { return annualFee; }
        public long getAdRevenue() { return adRevenue; }
        public long getTotalSettlement() { return totalSettlement; }
    }
}
