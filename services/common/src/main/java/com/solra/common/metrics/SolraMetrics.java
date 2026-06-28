package com.solra.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Solra 公共指标收集器。
 * 注册业务自定义 Counter/Gauge/Timer。
 */
@Component
public class SolraMetrics {

    public SolraMetrics(MeterRegistry registry) {
        // 注册公共业务指标
        registry.counter("solra.requests.total", "component", "common");
        registry.gauge("solra.uptime.seconds", System::currentTimeMillis);
    }
}
