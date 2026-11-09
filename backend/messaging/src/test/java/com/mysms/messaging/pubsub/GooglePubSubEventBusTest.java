package com.mysms.messaging.pubsub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.events.EventBus;
import com.mysms.core.events.EventEnvelope;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.json.JsonMapper;

class GooglePubSubEventBusTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void settingsRejectInvalidResourcesRetentionAndRetryLimits() {
        assertThatThrownBy(() -> settings("", "mysms-", Set.of("hr"), Duration.ofDays(3), Duration.ofSeconds(1),
                Duration.ofSeconds(5), 5, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings("project", "1bad-", Set.of("hr"), Duration.ofDays(3),
                Duration.ofSeconds(1), Duration.ofSeconds(5), 5, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings("project", "mysms-", Set.of(), Duration.ofDays(3), Duration.ofSeconds(1),
                Duration.ofSeconds(5), 5, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings("project", "mysms-", Set.of("HR"), Duration.ofDays(3),
                Duration.ofSeconds(1), Duration.ofSeconds(5), 5, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings("project", "mysms-", Set.of("hr"), Duration.ofMinutes(9),
                Duration.ofSeconds(1), Duration.ofSeconds(5), 5, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings("project", "mysms-", Set.of("hr"), Duration.ofDays(32),
                Duration.ofSeconds(1), Duration.ofSeconds(5), 5, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings("project", "mysms-", Set.of("hr"), Duration.ofDays(3), Duration.ZERO,
                Duration.ofSeconds(5), 5, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings("project", "mysms-", Set.of("hr"), Duration.ofDays(3),
                Duration.ofSeconds(5), Duration.ofSeconds(1), 5, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings("project", "mysms-", Set.of("hr"), Duration.ofDays(3),
                Duration.ofSeconds(1), Duration.ofMinutes(11), 5, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings("project", "mysms-", Set.of("hr"), Duration.ofDays(3),
                Duration.ofSeconds(1), Duration.ofSeconds(5), 4, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings("project", "mysms-", Set.of("hr"), Duration.ofDays(3),
                Duration.ofSeconds(1), Duration.ofSeconds(5), 5, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void derivesResourceNamesAndRejectsInvalidSubscriptionsAndEventsWithoutContactingCloud() {
        GooglePubSubEventBus bus = new GooglePubSubEventBus(settings("project", "mysms-", Set.of("hr"),
                Duration.ofDays(3), Duration.ofSeconds(1), Duration.ofSeconds(5), 5, 10), JSON);
        assertThat(bus.name()).isEqualTo("google-pubsub");
        assertThat(bus.topicId("hr")).isEqualTo("mysms-hr");
        assertThat(bus.subscriptionId("sis", "hr")).isEqualTo("mysms-sis-from-hr");
        assertThat(bus.deadLetterTopicId("sis")).isEqualTo("mysms-sis-dead");
        assertThat(bus.deadLetterSubscriptionId("sis")).isEqualTo("mysms-sis-dead");
        assertThat(bus.connected()).isTrue();
        EventBus.Subscription empty = bus.subscribe("sis", Set.of(), event -> { });
        empty.close();
        assertThatThrownBy(() -> bus.subscribe("bad consumer", Set.of(), event -> { }))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bus.subscribe("sis", Set.of("not-an-event"), event -> { }))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bus.subscribe("sis", Set.of("sis.student.created"), event -> { }))
                .isInstanceOf(IllegalArgumentException.class);
        EventEnvelope foreignSource = event("sis", "sis.student.created", "sis.student");
        assertThatThrownBy(() -> bus.publish(foreignSource)).isInstanceOf(IllegalArgumentException.class);
        EventEnvelope mismatchedType = event("hr", "sis.student.created", "hr.student");
        assertThatThrownBy(() -> bus.publish(mismatchedType)).isInstanceOf(IllegalArgumentException.class);
        bus.close();
        assertThat(bus.connected()).isFalse();
    }

    @Test
    void readsProjectIdFromEnvironmentAndUsesLocalProjectForTheEmulator() {
        GooglePubSubEventsAutoConfiguration configuration = new GooglePubSubEventsAutoConfiguration();
        MockEnvironment envProject = new MockEnvironment().withProperty("GOOGLE_CLOUD_PROJECT", "env-project")
                .withProperty("mysms.events.google-pubsub.create-resources", "false");
        EventBus fromEnvironment = configuration.googlePubSubEventBus(envProject, JSON);
        assertThat(((GooglePubSubEventBus) fromEnvironment).topicId("hr")).isEqualTo("mysms-events-hr");
        fromEnvironment.close();

        MockEnvironment emulator = new MockEnvironment().withProperty("PUBSUB_EMULATOR_HOST", "localhost:8085")
                .withProperty("mysms.events.google-pubsub.create-resources", "false");
        EventBus local = configuration.googlePubSubEventBus(emulator, JSON);
        assertThat(((GooglePubSubEventBus) local).topicId("hr")).isEqualTo("mysms-events-hr");
        local.close();

        assertThatThrownBy(() -> configuration.googlePubSubEventBus(new MockEnvironment(), JSON))
                .isInstanceOf(IllegalStateException.class);
    }

    private static GooglePubSubEventBus.Settings settings(String project, String topicPrefix, Set<String> sources,
                                                          Duration retention, Duration retry, Duration maxRetry,
                                                          int attempts, long outstanding) {
        return new GooglePubSubEventBus.Settings(project, topicPrefix, "mysms-", sources, false, "", retention,
                Duration.ofSeconds(10), retry, maxRetry, attempts, outstanding);
    }

    private static EventEnvelope event(String module, String type, String aggregateType) {
        return new EventEnvelope(UUID.randomUUID(), module, type, Optional.empty(), aggregateType, UUID.randomUUID(),
                Instant.now(), "{}", Optional.empty());
    }
}
