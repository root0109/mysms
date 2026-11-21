package com.mysms.authclient;

import com.mysms.core.id.UuidV7;
import com.mysms.messaging.events.EventBus;
import com.mysms.messaging.events.EventEnvelope;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publishes the API-permission catalogue of every module this process serves to iam (JAVA-CODING-RULES §16.2), as
 * {@code <module>.api_permissions.published}: once the application is ready and again every
 * {@code mysms.auth.catalogue-interval}, so iam can tell an operation still served by a running release from one that
 * no release publishes any more. The catalogue is the module's operation table.
 *
 * <p>Straight to the {@link EventBus}, not through an outbox: auth-client has no database, and a publish is harmless
 * to repeat (iam applies it idempotently). It never blocks start-up: a bus that refuses is retried in the background
 * with a doubling delay up to {@link #MAX_DELAY}, after which the regular interval resumes; nothing is lost but time.
 */
public final class ApiPermissionPublisher implements ApplicationListener<ApplicationReadyEvent>, AutoCloseable {

    /** The first retry delay; doubled at each further failure. */
    static final Duration FIRST_DELAY = Duration.ofSeconds(1);
    /** The longest retry delay. */
    static final Duration MAX_DELAY = Duration.ofMinutes(5);

    private static final Logger LOG = LoggerFactory.getLogger(ApiPermissionPublisher.class);

    private final OperationTable operations;
    private final EventBus bus;
    private final JsonMapper json;
    private final Clock clock;
    private final String release;
    private final Duration interval;
    private final ScheduledExecutorService scheduler;

    public ApiPermissionPublisher(OperationTable operations, EventBus bus, JsonMapper json, Clock clock,
                                  String release, Duration interval) {
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("mysms.auth.catalogue-interval must be positive");
        }
        this.operations = operations;
        this.bus = bus;
        this.json = json;
        this.clock = clock;
        this.release = release;
        this.interval = interval;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "api-permission-publisher");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent ready) {
        operations.modules().stream().sorted().forEach(module -> scheduler.execute(() -> publish(module, FIRST_DELAY)));
    }

    /** One publish of the module's catalogue; schedules the next one: the interval after success, else a retry. */
    private void publish(String module, Duration retryDelay) {
        Duration next;
        Duration nextRetry;
        try {
            ApiPermissionCatalogue catalogue = ApiPermissionCatalogue.of(operations, module, release, json);
            bus.publish(new EventEnvelope(UuidV7.next(clock), module, catalogue.eventType(), Optional.empty(),
                    catalogue.aggregateType(), catalogue.aggregateId(), clock.instant(), catalogue.toJson(json),
                    Optional.empty()));
            LOG.debug("published the API-permission catalogue of {} ({} operations, release {}, sha256 {})", module,
                    catalogue.entries().size(), catalogue.release(), catalogue.sha256());
            next = interval;
            nextRetry = FIRST_DELAY;
        } catch (RuntimeException e) {
            LOG.warn("the API-permission catalogue of {} was not published; retrying in {}", module, retryDelay, e);
            next = retryDelay;
            nextRetry = retryDelay.multipliedBy(2).compareTo(MAX_DELAY) > 0 ? MAX_DELAY : retryDelay.multipliedBy(2);
        }
        Duration retry = nextRetry;
        try {
            scheduler.schedule(() -> publish(module, retry), next.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            LOG.debug("the API-permission publisher stopped; {} is not published again", module);
        }
    }

    /** Stops publishing (called by Spring when the context closes). */
    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
