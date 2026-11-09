package com.mysms.core.events;

import java.util.Set;

/**
 * The message bus between modules, behind MySMS's own interface so the broker can change by configuration
 * (`mysms.events.bus`). Implementations are supplied together by the `messaging` module: RabbitMQ (the default),
 * Google Cloud Pub/Sub, and an in-process adapter for one JVM. Delivery is at least once; consumers make it
 * exactly-once with their inbox ({@link InboxProcessor}).
 */
public interface EventBus extends AutoCloseable {

    /** The implementation's name, as configured in `mysms.events.bus`. */
    String name();

    /**
     * Publishes one event and returns once the broker has accepted it (a publisher confirm), so the relay marks an
     * outbox row published only when the event is safe. Throws when it was not accepted.
     */
    void publish(EventEnvelope event);

    /**
     * Delivers every event of the given types to the consumer module's handler. Each consumer module has its own
     * durable queue or subscription: replicas of one module share its deliveries, different modules each get every
     * event. A handler that throws gets the event again later, after a growing delay; after repeated failures the
     * event is parked for an operator (the consumer's dead-letter queue or topic). A
     * {@link RejectedEventException} parks it at once.
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
