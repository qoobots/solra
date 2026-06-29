package com.solra.common.cost;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BudgetAlertTest {

    @Test
    void shouldCreateAlert() {
        Instant now = Instant.now();
        BudgetAlert alert = new BudgetAlert(
                "alert-001", "solra-monthly-total", 75,
                40000.0, 50000.0,
                BudgetAlert.Severity.WARNING, now,
                "预算消耗已达75%");

        assertEquals("alert-001", alert.getAlertId());
        assertEquals("solra-monthly-total", alert.getBudgetName());
        assertEquals(75, alert.getTriggeredThreshold());
        assertEquals(40000.0, alert.getCurrentCost());
        assertEquals(50000.0, alert.getBudgetAmount());
        assertEquals(80.0, alert.getConsumptionPercent(), 0.01);
        assertEquals(BudgetAlert.Severity.WARNING, alert.getSeverity());
        assertEquals(now, alert.getAlertTime());
    }

    @Test
    void shouldDetermineSeverityFromThreshold() {
        assertEquals(BudgetAlert.Severity.INFO,
                BudgetAlert.severityFromThreshold(50));
        assertEquals(BudgetAlert.Severity.WARNING,
                BudgetAlert.severityFromThreshold(75));
        assertEquals(BudgetAlert.Severity.CRITICAL,
                BudgetAlert.severityFromThreshold(90));
        assertEquals(BudgetAlert.Severity.CRITICAL,
                BudgetAlert.severityFromThreshold(100));
    }

    @Test
    void shouldHandleZeroBudget() {
        BudgetAlert alert = new BudgetAlert(
                "alert-002", "zero-budget", 100,
                0, 0, BudgetAlert.Severity.CRITICAL, null, "empty");

        assertEquals(0.0, alert.getConsumptionPercent());
    }

    @Test
    void shouldDefaultAlertTimeToNow() {
        BudgetAlert alert = new BudgetAlert(
                "alert-003", "test", 50,
                100.0, 200.0, BudgetAlert.Severity.INFO, null, "test");

        assertNotNull(alert.getAlertTime());
    }

    @Test
    void shouldFormatToString() {
        BudgetAlert alert = new BudgetAlert(
                "alert-004", "compute-budget", 90,
                18000.0, 20000.0, BudgetAlert.Severity.CRITICAL,
                Instant.now(), "接近预算上限");

        String str = alert.toString();
        assertTrue(str.contains("CRITICAL"));
        assertTrue(str.contains("compute-budget"));
        assertTrue(str.contains("90.0%"));
    }
}
