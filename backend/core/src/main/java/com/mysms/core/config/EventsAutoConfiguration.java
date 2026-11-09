package com.mysms.core.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.events.EventBus;
import com.mysms.core.events.EventRuntime;
import com.mysms.core.events.EventSubscriptions;
import com.mysms.core.events.EventsHealthIndicator;
import com.mysms.core.events.InboxProcessor;
import com.mysms.core.events.OutboxRelay;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

/**
 * The event bus of this process (`mysms.events.bus`): `rabbitmq` (default), `google-pubsub`, `in-process` (one JVM),
 * or `none` (no relay and no consumers: events stay in the outboxes — for tests that do not exercise events). The
 * adapters live together in the `messaging` module. This configuration starts a relay per module database and an
 * inbox subscription per consuming module.
 */
@AutoConfiguration(after = CoreAutoConfiguration.class,
        afterName = {"com.mysms.messaging.rabbitmq.RabbitEventsAutoConfiguration",
                "com.mysms.messaging.pubsub.GooglePubSubEventsAutoConfiguration",
                "com.mysms.messaging.inprocess.InProcessEventsAutoConfiguration"})
public class EventsAutoConfiguration {

    public static final String BUS = "mysms.events.bus";

    @Bean
    public EventRuntime eventRuntime(ObjectProvider<EventBus> bus, ObjectProvider<ModuleDatabase> databases,
                                               ObjectProvider<EventSubscriptions> subscriptions, JsonMapper json,
                                               Clock clock, Environment environment) {
        String mode = environment.getProperty(BUS, "rabbitmq");
        if ("none".equals(mode)) {
            return EventRuntime.off();
        }
        EventBus chosen = bus.getIfAvailable();
        if (chosen == null) {
            throw new IllegalStateException(BUS + "=" + mode
                    + " has no bus: add the messaging module and its settings (DEPLOYMENT.md \"Events\"), "
                    + "or set " + BUS + "=in-process|none");
        }
        List<ModuleDatabase> all = databases.orderedStream().toList();
        OutboxRelay.Settings defaults = OutboxRelay.Settings.defaults();
        OutboxRelay.Settings settings = new OutboxRelay.Settings(
                environment.getProperty("mysms.events.relay.batch-size", Integer.class, defaults.batchSize()),
                environment.getProperty("mysms.events.outbox-retention", Duration.class, defaults.outboxRetention()),
                environment.getProperty("mysms.events.inbox-retention", Duration.class, defaults.inboxRetention()),
                environment.getProperty("mysms.events.relay.max-batch-time", Duration.class, defaults.maxBatchTime()));
        boolean relaying = environment.getProperty("mysms.events.relay.enabled", Boolean.class, true);
        List<OutboxRelay> relays = relaying
                ? all.stream().map(d -> OutboxRelay.of(d, chosen, json, clock, settings))
                        .flatMap(Optional::stream).toList()
                : List.of();
        List<EventSubscriptions> consumers = subscriptions.orderedStream().toList();
        Set<String> consuming = consumers.stream().map(EventSubscriptions::module).collect(Collectors.toSet());
        List<InboxProcessor> processors = all.stream().filter(d -> consuming.contains(d.module()))
                .map(d -> new InboxProcessor(d, clock)).toList();
        return new EventRuntime(chosen, relays, consumers, processors,
                environment.getProperty("mysms.events.relay.interval", Duration.class, Duration.ofMillis(500)), clock);
    }

    /** `/actuator/health` (readiness): down when the bus is disconnected or a relay keeps failing. */
    @Bean
    public EventsHealthIndicator eventsHealthIndicator(EventRuntime runtime) {
        return new EventsHealthIndicator(runtime);
    }
}
