package com.solra.common.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Solra 自定义健康检查指示器。
 * 聚合检查 Kafka 等依赖服务连通性。
 * INF-004: Kafka 健康检查实现。
 */
@Component
public class SolraHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(SolraHealthIndicator.class);

    private final String bootstrapServers;

    public SolraHealthIndicator() {
        this.bootstrapServers = System.getProperty("spring.kafka.bootstrap-servers", "localhost:9092");
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up()
                .withDetail("service", "solra-common")
                .withDetail("version", getClass().getPackage().getImplementationVersion());

        // Kafka 连通性检查
        try {
            checkKafkaConnection(builder);
        } catch (Exception e) {
            log.warn("Kafka health check failed: {}", e.getMessage());
            builder.withDetail("kafka", "unavailable")
                    .withDetail("kafka_error", e.getMessage());
        }

        return builder.build();
    }

    private void checkKafkaConnection(Health.Builder builder) throws Exception {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("request.timeout.ms", 3000);

        try (AdminClient adminClient = AdminClient.create(props)) {
            DescribeClusterResult result = adminClient.describeCluster(
                    new DescribeClusterOptions().timeoutMs(3000));
            String clusterId = result.clusterId().get(3, TimeUnit.SECONDS);
            int nodeCount = result.nodes().get(3, TimeUnit.SECONDS).size();

            builder.withDetail("kafka", "available")
                    .withDetail("kafka_brokers", bootstrapServers)
                    .withDetail("kafka_cluster_id", clusterId)
                    .withDetail("kafka_node_count", nodeCount);
        }
    }
}
