package com.solra.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SolraMetrics 单元测试")
class SolraMetricsTest {

    @Test
    @DisplayName("应成功注册 solra.requests.total Counter")
    void shouldRegisterRequestsTotalCounter() {
        MeterRegistry registry = new SimpleMeterRegistry();
        new SolraMetrics(registry);

        // 验证 Counter 已注册
        assertNotNull(registry.find("solra.requests.total").counter(),
                "solra.requests.total Counter 应已注册");
    }

    @Test
    @DisplayName("应成功注册 solra.uptime.seconds Gauge")
    void shouldRegisterUptimeGauge() {
        MeterRegistry registry = new SimpleMeterRegistry();
        new SolraMetrics(registry);

        // 验证 Gauge 已注册
        assertNotNull(registry.find("solra.uptime.seconds").gauge(),
                "solra.uptime.seconds Gauge 应已注册");
    }

    @Test
    @DisplayName("Counter 应可递增")
    void counterShouldBeIncrementable() {
        MeterRegistry registry = new SimpleMeterRegistry();
        new SolraMetrics(registry);

        registry.counter("solra.requests.total", "component", "common").increment();
        assertEquals(1.0, registry.find("solra.requests.total").counter().count());
    }

    @Test
    @DisplayName("构造不应抛出异常")
    void constructorShouldNotThrow() {
        assertDoesNotThrow(() -> new SolraMetrics(new SimpleMeterRegistry()));
    }
}
