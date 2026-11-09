package com.mysms.messaging.events;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/** Readiness of the event machinery ({@link EventRuntime#health()}): the bus connection and the relays. */
public final class EventsHealthIndicator implements HealthIndicator {

    private final EventRuntime runtime;

    public EventsHealthIndicator(EventRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public Health health() {
        EventRuntime.Health health = runtime.health();
        return (health.up() ? Health.up() : Health.down()).withDetails(health.details()).build();
    }
}
