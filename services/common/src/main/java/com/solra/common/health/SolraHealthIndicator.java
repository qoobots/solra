package com.solra.common.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Solra 自定义健康检查指示器。
 * 聚合检查数据库连接、Redis、Kafka 等依赖服务状态。
 */
@Component
public class SolraHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // TODO: 检查 PostgreSQL / Redis / Kafka / gRPC 依赖连通性
        return Health.up()
                .withDetail("service", "solra-common")
                .withDetail("version", getClass().getPackage().getImplementationVersion())
                .build();
    }
}
