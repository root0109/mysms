package com.mysms.messaging.events;

import java.util.Set;

/**
 * The message bus between modules, chosen by {@code mysms.events.bus}: RabbitMQ (default), Google Cloud Pub/Sub or
 * in-process. Delivery is at least once; consumers make it exactly-once with their {@link InboxProcessor}.
 */
public interface EventBus extends AutoCloseable {

    String name();

    /**
     * Publishes one event and returns once the broker has accepted it (a publisher confirm), so the relay marks an
     * outbox row published only when the event is safe. Throws when it was not accepted.
     */
    void publish(EventEnvelope event);

    /**
     * Delivers every event of the given types to the consumer module's handler. Replicas of one module share its
     * deliveries; different modules each get every event. A throwing handler gets the event again after a growing delay
     * and it is parked after repeated failures; a {@link RejectedEventException} parks it at once.
     */
    Subscription subscribe(String consumer, Set<String> eventTypes, EventHandler handler);

    /** Whether the bus can publish and deliver right now (the readiness check). */
    default boolean connected() {
        return true;
    }

    @Override
    void close();

    /** A running subscription; closing it stops deliveries. */
    interface Subscription extends AutoCloseable {

        @Override
        void close();
    }
}
