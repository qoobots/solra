package com.solra.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * EventPublisher — Kafka 领域事件发布器。
 * 各微服务通过此组件将领域事件发布到 Kafka，实现跨服务事件驱动通信。
 * INF-004: 消息队列事件驱动架构底座。
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 发布领域事件到指定 Kafka topic。
     *
     * @param topic  Kafka topic 名称
     * @param event  领域事件对象
     * @return 异步发送结果
     */
    public CompletableFuture<SendResult<String, String>> publish(String topic, DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String key = event.getPartitionKey();

            log.debug("Publishing event: topic={} type={} key={} eventId={}",
                    topic, event.getEventType(), key, event.getEventId());

            return kafkaTemplate.send(topic, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event: topic={} eventId={} error={}",
                                    topic, event.getEventId(), ex.getMessage(), ex);
                        } else {
                            log.debug("Event published: topic={} eventId={} offset={}",
                                    topic, event.getEventId(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: eventId={} error={}", event.getEventId(), e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 同步发布事件（阻塞等待结果）。
     *
     * @param topic  Kafka topic 名称
     * @param event  领域事件对象
     */
    public void publishSync(String topic, DomainEvent event) {
        try {
            publish(topic, event).get();
        } catch (Exception e) {
            log.error("Sync publish failed: topic={} eventId={} error={}",
                    topic, event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish event synchronously", e);
        }
    }
}
