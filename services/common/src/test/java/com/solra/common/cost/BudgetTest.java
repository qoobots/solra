package com.solra.common.cost;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BudgetTest {

    @Test
    void shouldCalculateConsumptionPercent() {
        Budget budget = new Budget("test-budget", 10000, "CNY", null,
                List.of(50, 75, 90, 100), List.of("email"));

        assertEquals(50.0, budget.getConsumptionPercent(5000), 0.01);
        assertEquals(75.0, budget.getConsumptionPercent(7500), 0.01);
        assertEquals(100.0, budget.getConsumptionPercent(10000), 0.01);
        assertEquals(150.0, budget.getConsumptionPercent(15000), 0.01);
        assertEquals(0.0, budget.getConsumptionPercent(0), 0.01);
    }

    @Test
    void shouldDetectThreshold() {
        Budget budget = new Budget("test-budget", 10000, "CNY", null,
                List.of(50, 75, 90, 100), List.of("email"));

        assertTrue(budget.isOverThreshold(5000, 50));
        assertTrue(budget.isOverThreshold(7500, 75));
        assertFalse(budget.isOverThreshold(4900, 50));
        assertTrue(budget.isOverThreshold(10000, 100));
    }

    @Test
    void shouldGetHighestTriggeredThreshold() {
        Budget budget = new Budget("test-budget", 10000, "CNY", null,
                List.of(50, 75, 90, 100), List.of("email"));

        assertEquals(0, budget.getHighestTriggeredThreshold(1000));
        assertEquals(50, budget.getHighestTriggeredThreshold(5000));
        assertEquals(75, budget.getHighestTriggeredThreshold(7500));
        assertEquals(100, budget.getHighestTriggeredThreshold(12000));
    }

    @Test
    void shouldIdentifyTotalBudget() {
        Budget total = new Budget("total", 50000, "CNY", null,
                List.of(50, 75, 90, 100), List.of("email"));
        Budget module = new Budget("compute", 20000, "CNY", CostResourceType.COMPUTE,
                List.of(50, 75, 90, 100), List.of("email"));

        assertTrue(total.isTotalBudget());
        assertFalse(module.isTotalBudget());
    }

    @Test
    void shouldCalculateRemaining() {
        Budget budget = new Budget("test-budget", 10000, "CNY", null,
                List.of(50, 75, 90, 100), List.of("email"));

        assertEquals(7000.0, budget.getRemaining(3000), 0.01);
        assertEquals(0.0, budget.getRemaining(10000), 0.01);
        assertEquals(0.0, budget.getRemaining(15000), 0.01);
    }

    @Test
    void shouldHandleZeroBudget() {
        Budget budget = new Budget("zero-budget", 0, "CNY", null,
                List.of(50, 75), List.of("email"));

        assertEquals(0.0, budget.getConsumptionPercent(100));
        assertFalse(budget.isOverThreshold(100, 50));
    }

    @Test
    void shouldDefaultThresholds() {
        Budget budget = new Budget("default-thresholds", 10000, "CNY",
                CostResourceType.STORAGE, null, null);

        assertEquals(List.of(50, 75, 90, 100), budget.getThresholdPercentages());
        assertEquals(List.of("email"), budget.getAlertChannels());
    }

    @Test
    void shouldGetResourceTypeForModuleBudget() {
        Budget budget = new Budget("compute", 20000, "CNY", CostResourceType.COMPUTE,
                List.of(50, 75, 90, 100), List.of("email", "slack"));

        assertEquals(CostResourceType.COMPUTE, budget.getResourceType());
        assertEquals("compute", budget.getResourceType().getConfigKey());
    }
}
