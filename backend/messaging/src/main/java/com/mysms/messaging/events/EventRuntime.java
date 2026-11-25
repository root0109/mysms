package com.mysms.messaging.events;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Starts this process's event machinery once the application is ready: one subscription per consuming module and one
 * relay loop over the module databases.
 *
 * <p>A consumer handles only events their source owns; anything else is rejected and parked. A relay pass gives each
 * module at most {@value #MAX_BATCHES_PER_PASS} batches, so a backlog in one module cannot starve the others.
 */
public final class EventRuntime implements SmartLifecycle {

    /** Batches one module may relay in a pass before the next module's turn. */
    public static final int MAX_BATCHES_PER_PASS = 10;

    /** The readiness of the event machinery: whether it is up, and why not. */
    public record Health(boolean up, Map<String, String> details) {

        public Health {
            details = Map.copyOf(details);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(EventRuntime.class);
    private static final int FAILURES_BEFORE_DOWN = 3;

    private final EventBus bus;
    private final List<OutboxRelay> relays;
    private final Map<String, Map<String, EventHandler>> handlersByModule;
    private final Map<String, InboxProcessor> processors;
    private final Duration interval;
    private final Clock clock;
    private final List<EventBus.Subscription> running = new ArrayList<>();
    private final Map<String, Integer> failures = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastSuccess = new ConcurrentHashMap<>();
    private ScheduledExecutorService executor;
    private final Object lock = new Object();

    public EventRuntime(EventBus bus, List<OutboxRelay> relays, List<EventSubscriptions> subscriptions,
                        List<InboxProcessor> processors, Duration interval) {
        this(bus, relays, subscriptions, processors, interval, Clock.systemUTC());
    }

    public EventRuntime(EventBus bus, List<OutboxRelay> relays, List<EventSubscriptions> subscriptions,
                        List<InboxProcessor> processors, Duration interval, Clock clock) {
        this.bus = bus;
        this.relays = List.copyOf(relays);
        this.clock = clock;
        Map<String, InboxProcessor> byModule = new HashMap<>();
        processors.forEach(p -> byModule.put(p.module(), p));
        this.processors = Map.copyOf(byModule);
        this.interval = interval;
        Map<String, Map<String, EventHandler>> merged = new LinkedHashMap<>();
        for (EventSubscriptions s : subscriptions) {
            if (!this.processors.containsKey(s.module())) {
                throw new IllegalStateException("module " + s.module() + " subscribes to events but has no database "
                        + "here for its inbox");
            }
            Map<String, EventHandler> module = merged.computeIfAbsent(s.module(), m -> new LinkedHashMap<>());
            s.handlers().forEach((type, handler) -> {
                if (module.putIfAbsent(type, handler) != null) {
                    throw new IllegalStateException("module " + s.module() + " has two handlers for event type "
                            + type + "; one module handles an event type once");
                }
            });
        }
        Map<String, Map<String, EventHandler>> frozen = new LinkedHashMap<>();
        merged.forEach((module, handlers) -> frozen.put(module, Map.copyOf(handlers)));
        this.handlersByModule = Map.copyOf(frozen);
    }

    /** `mysms.events.bus=none`: nothing is relayed or consumed (events stay in the outboxes). */
    public static EventRuntime off() {
        return new EventRuntime(null, List.of(), List.of(), List.of(), Duration.ofSeconds(1));
    }

    /** One relay pass over every module; returns the number of events published (tests and the scheduler). */
    public int relayOnce() {
        int published = 0;
        for (OutboxRelay relay : relays) {
            try {
                OutboxRelay.Pass pass;
                int batches = 0;
                do {
                    pass = relay.runOnce();
                    published += pass.published();
                    batches++;
                } while (pass.busy() && batches < MAX_BATCHES_PER_PASS);
                failures.remove(relay.module());
                lastSuccess.put(relay.module(), clock.instant());
            } catch (RuntimeException e) {
                if (stopping(e)) {
                    // shutdown interrupted the pass (stop() ends the executor): not a failure; the events wait in the
                    // outbox for the next start
                    LOG.debug("relay of module {} stopped by shutdown", relay.module());
                    Thread.currentThread().interrupt();
                    return published;
                }
                int count = failures.merge(relay.module(), 1, Integer::sum);
                if (count == FAILURES_BEFORE_DOWN) {
                    LOG.error("relay of module {} failed {} passes in a row; its events wait in its outbox",
                            relay.module(), count, e);
                } else {
                    LOG.warn("relay of module {} failed; it retries at its next pass", relay.module(), e);
                }
            }
        }
        return published;
    }

    /** Whether the failure is the relay thread being interrupted, which only shutdown does. */
    private static boolean stopping(Throwable failure) {
        if (Thread.currentThread().isInterrupted()) {
            return true;
        }
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof InterruptedException) {
                return true;
            }
        }
        return false;
    }

    /** Down when the bus is disconnected or a relay failed several passes in a row. */
    public Health health() {
        if (bus == null) {
            return new Health(true, Map.of("bus", "none"));
        }
        Map<String, String> details = new LinkedHashMap<>();
        details.put("bus", bus.name());
        boolean up = bus.connected();
        if (!up) {
            details.put("connection", "lost");
        }
        for (OutboxRelay relay : relays) {
            int count = failures.getOrDefault(relay.module(), 0);
            if (count >= FAILURES_BEFORE_DOWN) {
                up = false;
                Instant last = lastSuccess.get(relay.module());
                details.put("relay." + relay.module(), count + " failed passes"
                        + (last == null ? "" : ", last success " + last));
            }
        }
        return new Health(up, Map.copyOf(details));
    }

    @Override
    public void start() {
        synchronized (lock) {
            if (executor != null || bus == null) {
                if (bus == null) {
                    LOG.warn("events are off (mysms.events.bus=none): nothing is relayed or consumed");
                }
                return;
            }
            handlersByModule.forEach((module, handlers) -> {
                if (handlers.isEmpty()) {
                    return;
                }
                InboxProcessor processor = processors.get(module);
                running.add(bus.subscribe(module, handlers.keySet(), event -> {
                    if (!event.ownedBySource()) {
                        throw new RejectedEventException("module " + event.module() + " does not own event "
                                + event.type() + " of " + event.aggregateType());
                    }
                    EventHandler handler = handlers.get(event.type());
                    if (handler == null) {
                        // a type the module no longer handles, still bound on the broker: acknowledged and dropped
                        LOG.info("module {} ignores event type {}", module, event.type());
                        return;
                    }
                    processor.deliver(event, handler);
                }));
            });
            executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "outbox-relay");
                thread.setDaemon(true);
                return thread;
            });
            executor.scheduleWithFixedDelay(this::relayOnce, interval.toMillis(), interval.toMillis(),
                    TimeUnit.MILLISECONDS);
            LOG.info("events: bus {}, {} relays, {} consuming modules", bus.name(), relays.size(), running.size());
        }
    }

    @Override
    public void stop() {
        synchronized (lock) {
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
            running.forEach(EventBus.Subscription::close);
            running.clear();
        }
    }

    @Override
    public boolean isRunning() {
        synchronized (lock) {
            return executor != null;
        }
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100; // after the web server and every repository; stops first
    }
}
