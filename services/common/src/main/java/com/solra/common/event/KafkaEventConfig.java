package com.solra.common.event;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KafkaEventConfig — Kafka 事件基础设施自动配置。
 * 提供 Producer/Consumer/Admin 工厂及核心 Topic 定义。
 * INF-004: 消息队列事件驱动架构底座。
 */
@Configuration
@ConditionalOnProperty(value = "solra.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${solra.kafka.consumer-group-prefix:solra}")
    private String consumerGroupPrefix;

    // ===== Producer =====

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ===== Consumer =====

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        // 错误处理：重试3次，间隔1秒
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new FixedBackOff(1000L, 3L)));
        return factory;
    }

    // ===== Admin (Topic 自动创建) =====

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /** 核心事件 Topic 定义（分区数3，副本数1用于开发环境） */
    @Bean
    public List<NewTopic> solraTopics() {
        return List.of(
                new NewTopic(EventTopics.SPACE_ENTERED, 3, (short) 1),
                new NewTopic(EventTopics.SPACE_LEFT, 3, (short) 1),
                new NewTopic(EventTopics.SPACE_PUBLISHED, 3, (short) 1),
                new NewTopic(EventTopics.AVATAR_STATE_CHANGED, 3, (short) 1),
                new NewTopic(EventTopics.CONVERSATION_MESSAGE, 3, (short) 1),
                new NewTopic(EventTopics.AVATAR_GREETING, 3, (short) 1),
                new NewTopic(EventTopics.AVATAR_SURPRISE, 3, (short) 1),
                new NewTopic(EventTopics.USER_PRESENCE_CHANGED, 3, (short) 1),
                new NewTopic(EventTopics.SOCIAL_INTERACTION, 3, (short) 1),
                new NewTopic(EventTopics.FRIEND_REQUEST, 3, (short) 1),
                new NewTopic(EventTopics.SPACE_SHARED, 3, (short) 1),
                new NewTopic(EventTopics.ACHIEVEMENT_UNLOCKED, 3, (short) 1),
                new NewTopic(EventTopics.FAITH_LEVEL_CHANGED, 3, (short) 1),
                new NewTopic(EventTopics.DECISIVE_MOMENT, 3, (short) 1),
                new NewTopic(EventTopics.USER_REENGAGED, 3, (short) 1),
                new NewTopic(EventTopics.NOTIFICATION_SENT, 3, (short) 1),
                new NewTopic(EventTopics.PUSH_DELIVERED, 3, (short) 1),
                new NewTopic(EventTopics.INBOX_MESSAGE, 3, (short) 1),
                new NewTopic(EventTopics.CONTENT_FLAGGED, 3, (short) 1),
                new NewTopic(EventTopics.REVIEW_COMPLETED, 3, (short) 1),
                new NewTopic(EventTopics.FEATURE_FLAG_CHANGED, 3, (short) 1)
        );
    }
}
