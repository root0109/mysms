package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.json.JsonMappers;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.inprocess.InProcessEventBus;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The catalogue each served module publishes at start-up, on a real in-process bus. */
class ApiPermissionPublisherTest {

    private static final JsonMapper JSON = JsonMappers.create();
    private static final Set<String> TYPES = Set.of("master.api_permissions.published",
            "authtest.api_permissions.published");

    @Test
    void publishesOneCatalogueEventPerServedModuleOnceReady() throws InterruptedException {
        OperationTable table = OperationTable.load(List.of("master", "authtest"));
        InProcessEventBus bus = new InProcessEventBus();
        List<EventEnvelope> received = new CopyOnWriteArrayList<>();
        bus.subscribe("iam", TYPES, received::add);

        try (ApiPermissionPublisher publisher = new ApiPermissionPublisher(table, bus, JSON, Clock.systemUTC(),
                "1.2.3", Duration.ofHours(1))) {
            publisher.onApplicationEvent(ready());
            await(() -> received.size() == 2);
        }

        EventEnvelope master = received.stream().filter(e -> "master".equals(e.module())).findFirst().orElseThrow();
        assertThat(master.type()).isEqualTo("master.api_permissions.published");
        assertThat(master.aggregateType()).isEqualTo("master.api_permissions");
        assertThat(master.tenantId()).isEmpty();
        assertThat(master.ownedBySource()).isTrue();
        JsonNode data = JSON.readTree(master.data());
        assertThat(data.get("module").asString()).isEqualTo("master");
        assertThat(data.get("release").asString()).isEqualTo("1.2.3");
        assertThat(data.get("entries").size()).isEqualTo(table.rulesOf("master").size());
        ApiPermissionCatalogue expected = ApiPermissionCatalogue.of(table, "master", "1.2.3", JSON);
        assertThat(data.get("sha256").asString()).isEqualTo(expected.sha256());
    }

    @Test
    void retriesInTheBackgroundUntilTheBusAcceptsTheCatalogue() throws InterruptedException {
        OperationTable table = OperationTable.load(List.of("authtest"));
        InProcessEventBus bus = new InProcessEventBus();
        AtomicInteger deliveries = new AtomicInteger();
        bus.subscribe("iam", TYPES, event -> {
            if (deliveries.incrementAndGet() == 1) {
                throw new IllegalStateException("iam is not ready yet");
            }
        });

        try (ApiPermissionPublisher publisher = new ApiPermissionPublisher(table, bus, JSON, Clock.systemUTC(),
                "1.2.3", Duration.ofHours(1))) {
            publisher.onApplicationEvent(ready());
            await(() -> deliveries.get() == 2);
        }

        assertThat(deliveries.get()).isEqualTo(2);
    }

    @Test
    void theHashDependsOnTheContractOnlyNotOnTheRelease() {
        OperationTable table = OperationTable.load(List.of("master", "authtest"));

        ApiPermissionCatalogue first = ApiPermissionCatalogue.of(table, "master", "1.0.0", JSON);
        ApiPermissionCatalogue again = ApiPermissionCatalogue.of(table, "master", "2.0.0", JSON);
        ApiPermissionCatalogue other = ApiPermissionCatalogue.of(table, "authtest", "1.0.0", JSON);

        assertThat(again.sha256()).isEqualTo(first.sha256());
        assertThat(other.sha256()).isNotEqualTo(first.sha256());
        assertThat(other.aggregateId()).isNotEqualTo(first.aggregateId());
        assertThat(first.entries()).isSortedAccordingTo((a, b) -> a.operationId().compareTo(b.operationId()));
    }

    private static ApplicationReadyEvent ready() {
        return new ApplicationReadyEvent(new SpringApplication(), new String[0], null, Duration.ZERO);
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long until = System.currentTimeMillis() + 10_000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < until) {
            Thread.sleep(50);
        }
    }
}
