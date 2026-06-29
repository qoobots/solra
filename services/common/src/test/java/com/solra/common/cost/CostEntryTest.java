package com.solra.common.cost;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CostEntryTest {

    @Test
    void shouldCreateCostEntryWithBuilder() {
        Instant now = Instant.now();
        Map<CostDimension, String> dims = Map.of(
                CostDimension.FUNCTION, "avt-service",
                CostDimension.USER, "user-001",
                CostDimension.SPACE, "space-abc");

        CostEntry entry = CostEntry.builder()
                .entryId("entry-001")
                .resourceType(CostResourceType.COMPUTE)
                .resourceId("i-12345")
                .amount(15000)  // ¥150.00
                .currency("CNY")
                .timestamp(now)
                .dimensions(dims)
                .description("GPU instance hourly cost")
                .build();

        assertEquals("entry-001", entry.getEntryId());
        assertEquals(CostResourceType.COMPUTE, entry.getResourceType());
        assertEquals("i-12345", entry.getResourceId());
        assertEquals(15000, entry.getAmount());
        assertEquals(150.0, entry.getAmountYuan());
        assertEquals("CNY", entry.getCurrency());
        assertEquals(now, entry.getTimestamp());
        assertEquals("GPU instance hourly cost", entry.getDescription());
    }

    @Test
    void shouldGetDimensionValue() {
        CostEntry entry = CostEntry.builder()
                .entryId("entry-002")
                .resourceType(CostResourceType.API)
                .resourceId("llm-call")
                .amount(500)
                .dimensions(Map.of(
                        CostDimension.FUNCTION, "avt-service",
                        CostDimension.USER, "user-001"))
                .build();

        assertEquals("avt-service", entry.getDimensionValue(CostDimension.FUNCTION));
        assertEquals("user-001", entry.getDimensionValue(CostDimension.USER));
        assertEquals("unknown", entry.getDimensionValue(CostDimension.SPACE));
    }

    @Test
    void shouldMatchDimension() {
        CostEntry entry = CostEntry.builder()
                .entryId("entry-003")
                .resourceType(CostResourceType.STORAGE)
                .resourceId("bucket-1")
                .amount(1000)
                .dimensions(Map.of(CostDimension.FUNCTION, "spc-service"))
                .build();

        assertTrue(entry.matchesDimension(CostDimension.FUNCTION, "spc-service"));
        assertFalse(entry.matchesDimension(CostDimension.FUNCTION, "avt-service"));
    }

    @Test
    void shouldDefaultCurrencyToCNY() {
        CostEntry entry = CostEntry.builder()
                .entryId("entry-004")
                .resourceType(CostResourceType.NETWORK)
                .resourceId("cdn-1")
                .amount(200)
                .build();

        assertEquals("CNY", entry.getCurrency());
    }

    @Test
    void shouldDefaultTimestampToNow() {
        CostEntry entry = CostEntry.builder()
                .entryId("entry-005")
                .resourceType(CostResourceType.MISCELLANEOUS)
                .resourceId("ssl-cert")
                .amount(100)
                .build();

        assertNotNull(entry.getTimestamp());
        assertTrue(entry.getTimestamp().isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void shouldThrowOnNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                CostEntry.builder()
                        .entryId("entry-006")
                        .resourceType(CostResourceType.COMPUTE)
                        .resourceId("i-1")
                        .amount(-100)
                        .build());
    }

    @Test
    void shouldThrowOnMissingResourceType() {
        assertThrows(IllegalArgumentException.class, () ->
                CostEntry.builder()
                        .entryId("entry-007")
                        .resourceId("i-1")
                        .amount(100)
                        .build());
    }

    @Test
    void shouldHandleZeroAmount() {
        CostEntry entry = CostEntry.builder()
                .entryId("entry-008")
                .resourceType(CostResourceType.OBSERVABILITY)
                .resourceId("free-tier")
                .amount(0)
                .build();

        assertEquals(0.0, entry.getAmountYuan());
    }

    @Test
    void shouldSupportLargeAmountWithoutOverflow() {
        CostEntry entry = CostEntry.builder()
                .entryId("entry-009")
                .resourceType(CostResourceType.COMPUTE)
                .resourceId("cluster")
                .amount(5000000)  // ¥50,000
                .build();

        assertEquals(50000.0, entry.getAmountYuan());
    }
}
