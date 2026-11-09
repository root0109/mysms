package com.mysms.core.events;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/** Synchronous event transport for core component tests; production transports live in the messaging module. */
final class TestEventBus implements EventBus {

    private record Entry(Set<String> eventTypes, EventHandler handler) {
    }

    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    @Override
    public String name() {
        return "test-bus";
    }

    @Override
    public void publish(EventEnvelope event) {
        for (Entry entry : entries) {
            if (entry.eventTypes().contains(event.type())) {
                try {
                    entry.handler().handle(event);
                } catch (RejectedEventException ignored) {
                    // The core tests only need to observe that a rejected delivery does not fail the publisher.
                }
            }
        }
    }

    @Override
    public Subscription subscribe(String consumer, Set<String> eventTypes, EventHandler handler) {
        Entry entry = new Entry(Set.copyOf(eventTypes), handler);
        entries.add(entry);
        return () -> entries.remove(entry);
    }

    @Override
    public void close() {
        entries.clear();
    }
}
