package com.sky.ordermanagement.inventory.config;

import com.sky.ordermanagement.common.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    /** Inventory Service owns the reservation-result topic (auto-created on startup via KafkaAdmin). */
    @Bean
    public NewTopic reservationResultTopic(@Value("${sky.oms.kafka.partitions:3}") int partitions) {
        return TopicBuilder.name(KafkaTopics.INVENTORY_RESERVATION_RESULT).partitions(partitions).replicas(1).build();
    }

    @Bean
    public NewTopic orderCreatedDlt(@Value("${sky.oms.kafka.partitions:3}") int partitions) {
        return TopicBuilder.name(KafkaTopics.ORDER_CREATED + KafkaTopics.DLT_SUFFIX)
                .partitions(partitions).replicas(1).build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template,
                                                 @Value("${sky.oms.kafka.retry.interval-ms:1000}") long intervalMs,
                                                 @Value("${sky.oms.kafka.retry.max-attempts:3}") long maxAttempts) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(template), new FixedBackOff(intervalMs, maxAttempts));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }
}
