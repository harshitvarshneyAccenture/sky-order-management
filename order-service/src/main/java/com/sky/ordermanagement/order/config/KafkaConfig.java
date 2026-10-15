package com.sky.ordermanagement.order.config;

import com.sky.ordermanagement.common.kafka.KafkaTopics;
import com.sky.ordermanagement.order.exception.OrderNotFoundException;
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

    /** Order Service owns the order-created topic (auto-created on startup via KafkaAdmin). */
    @Bean
    public NewTopic orderCreatedTopic(@Value("${sky.oms.kafka.partitions:3}") int partitions) {
        return TopicBuilder.name(KafkaTopics.ORDER_CREATED).partitions(partitions).replicas(1).build();
    }

    /** DLT for the topic this service consumes (same partition count - the recoverer keeps the partition). */
    @Bean
    public NewTopic reservationResultDlt(@Value("${sky.oms.kafka.partitions:3}") int partitions) {
        return TopicBuilder.name(KafkaTopics.INVENTORY_RESERVATION_RESULT + KafkaTopics.DLT_SUFFIX)
                .partitions(partitions).replicas(1).build();
    }

    /**
     * Retry transient failures (e.g. DB blip) a few times, then park the record on a DLT instead of
     * blocking the partition forever. Business errors that will never succeed skip retries.
     * Picked up automatically by Spring Boot's listener container factory.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template,
                                                 @Value("${sky.oms.kafka.retry.interval-ms:1000}") long intervalMs,
                                                 @Value("${sky.oms.kafka.retry.max-attempts:3}") long maxAttempts) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(template), new FixedBackOff(intervalMs, maxAttempts));
        handler.addNotRetryableExceptions(OrderNotFoundException.class, IllegalArgumentException.class);
        return handler;
    }
}
