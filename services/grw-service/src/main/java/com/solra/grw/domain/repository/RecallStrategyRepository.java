package com.solra.grw.domain.repository;

import com.solra.grw.domain.model.RecallStrategy;

import java.util.List;
import java.util.Optional;

/**
 * 召回策略仓储接口。
 */
public interface RecallStrategyRepository {
    RecallStrategy save(RecallStrategy strategy);
    Optional<RecallStrategy> findById(String strategyId);
    List<RecallStrategy> findAll();
    List<RecallStrategy> findByRiskLevel(String riskLevel);
    List<RecallStrategy> findActive();
}
