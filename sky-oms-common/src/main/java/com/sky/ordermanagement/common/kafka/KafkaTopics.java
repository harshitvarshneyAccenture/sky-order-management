package com.sky.ordermanagement.common.kafka;

/**
 * Topic names are part of the public contract between services, so they live next to the event records.
 * Naming convention: {client}.{domain}.{event}.{version}
 */
public final class KafkaTopics {

    public static final String ORDER_CREATED = "sky.oms.order-created.v1";
    public static final String INVENTORY_RESERVATION_RESULT = "sky.oms.inventory-reservation-result.v1";

    /** Suffix used by Spring Kafka's DeadLetterPublishingRecoverer. */
    public static final String DLT_SUFFIX = ".DLT";

    private KafkaTopics() {
    }
}
