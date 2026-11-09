package com.mysms.messaging.inprocess;

import com.mysms.core.events.EventBus;
import com.mysms.core.events.EventEnvelope;
import com.mysms.core.events.EventHandler;
import com.mysms.core.events.RejectedEventException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * `mysms.events.bus=in-process`: delivers events to the subscribers of the same JVM, synchronously on the publishing
 * thread (the relay). A failing handler makes the publish fail, so the outbox row stays pending and is published
 * again at the relay's next pass — the same at-least-once contract as a broker. After {@code deliveryLimit} failed
 * deliveries of one event to one consumer, that consumer is skipped for it (logged as an error; its inbox keeps the
 * failure in `processing_error`), so one poison event cannot hold back the module's later events. A
 * {@link RejectedEventException} skips at once. Suitable for the single-process monolith; services in separate
 * processes need a broker (RabbitMQ, the default, or Google Cloud Pub/Sub).
 */
public final class InProcessEventBus implements EventBus {

    /** Deliveries of one event to one consumer before it is skipped, as with the broker's delivery limit. */
    public static final int DEFAULT_DELIVERY_LIMIT = 6;

    private static final Logger LOG = LoggerFactory.getLogger(InProcessEventBus.class);

    private record Entry(String consumer, Set<String> types, EventHandler handler) {
    }

    private record Attempt(String consumer, UUID event) {
    }

    private final List<Entry> entries = new CopyOnWriteArrayList<>();
    private final Map<Attempt, Integer> failures = new ConcurrentHashMap<>();
    private final Set<Attempt> parked = ConcurrentHashMap.newKeySet();
    private final int deliveryLimit;

    public InProcessEventBus() {
        this(DEFAULT_DELIVERY_LIMIT);
    }

    public InProcessEventBus(int deliveryLimit) {
        if (deliveryLimit < 1) {
            throw new IllegalArgumentException("delivery limit must be positive");
        }
        this.deliveryLimit = deliveryLimit;
    }

    @Override
    public String name() {
        return "in-process";
    }

    @Override
    public void publish(EventEnvelope event) {
        RuntimeException failure = null;
        for (Entry entry : entries) {
            Attempt attempt = new Attempt(entry.consumer(), event.id());
            if (!entry.types().contains(event.type()) || parked.contains(attempt)) {
                continue;
            }
            try {
                entry.handler().handle(event);
                failures.remove(attempt);
            } catch (RejectedEventException e) {
                park(attempt, event, e.getMessage());
            } catch (RuntimeException e) {
                int count = failures.merge(attempt, 1, Integer::sum);
                if (count >= deliveryLimit) {
                    park(attempt, event, count + " failed deliveries");
                } else {
                    LOG.warn("event {} of type {} failed in consumer {}; it is delivered again", event.id(),
                            event.type(), entry.consumer());
                    failure = failure == null ? e : failure;
                }
            }
        }
        if (failure != null) {
            throw failure; // every consumer had its delivery; the failed ones get it again (the others' inbox drops it)
        }
    }

    private void park(Attempt attempt, EventEnvelope event, String why) {
        failures.remove(attempt);
        parked.add(attempt);
        LOG.error("event {} of type {} is parked for consumer {} ({}); it is not delivered to it again", event.id(),
                event.type(), attempt.consumer(), why);
    }

    @Override
    public Subscription subscribe(String consumer, Set<String> eventTypes, EventHandler handler) {
        Entry entry = new Entry(consumer, Set.copyOf(eventTypes), handler);
        entries.add(entry);
        return () -> entries.remove(entry);
    }

    @Override
    public void close() {
        entries.clear();
    }
}
