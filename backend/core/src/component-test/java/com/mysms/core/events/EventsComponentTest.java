package com.mysms.core.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.testkit.TestDatabase;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.json.JsonMapper;

/**
 * Real PostgreSQL: the outbox relay reads every tenant's pending events through the relay role and publishes them in
 * order; the inbox makes deliveries exactly-once per consuming module, inside the event's tenant.
 */
class EventsComponentTest {

    private static final UUID TENANT = UUID.fromString("0190b0a0-0000-7000-8000-00000000000a");
    private static final UUID OTHER = UUID.fromString("0190b0a0-0000-7000-8000-00000000000b");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static TestDatabase database;
    private static ModuleDatabase db;
    private static ModuleDatabase withoutRelay;

    private final List<EventEnvelope> received = new CopyOnWriteArrayList<>();
    private TestEventBus bus;

    @BeforeAll
    static void open() {
        database = TestDatabase.create();
        db = ModuleDatabase.open("coretest", environment(true), true);
        withoutRelay = ModuleDatabase.open("coretest", environment(false), true);
    }

    private static MockEnvironment environment(boolean relay) {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("mysms.coretest.datasource.url", database.jdbcUrl())
                .withProperty("mysms.coretest.datasource.username", database.username())
                .withProperty("mysms.coretest.datasource.password", database.password())
                .withProperty("mysms.coretest.datasource.migrate-username", database.migrateUsername())
                .withProperty("mysms.coretest.datasource.migrate-password", database.migratePassword());
        if (relay) {
            environment.withProperty("mysms.coretest.datasource.relay-username", database.relayUsername())
                    .withProperty("mysms.coretest.datasource.relay-password", database.relayPassword());
        }
        return environment;
    }

    @AfterAll
    static void close() {
        withoutRelay.close();
        db.close();
        database.close();
    }

    @BeforeEach
    void bus() {
        bus = new TestEventBus();
        bus.subscribe("coretest", Set.of("coretest.item.deleted"), received::add);
        drain();
        received.clear();
    }

    @AfterEach
    void clear() {
        CallContextHolder.clear();
        bus.close();
    }

    private static void as(UUID tenant) {
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(tenant, TENANT));
    }

    private static void write(UUID tenant, String type, UUID aggregate) {
        as(tenant);
        OutboxWriter outbox = new OutboxWriter("coretest", db.jdbc(), JSON);
        db.tx().write(() -> {
            outbox.publish(type, "coretest.item", aggregate, Map.of("id", aggregate.toString(),
                    "traceId", "4bf92f3577b34da6a3ce929d0e0e4736"));
            return null;
        });
        CallContextHolder.clear();
    }

    private void drain() {
        OutboxRelay relay = OutboxRelay.of(db, bus, JSON, Clock.systemUTC()).orElseThrow();
        while (relay.runOnce().busy()) {
            // keep going until nothing is pending
        }
    }

    @Test
    void theRelayPublishesEveryTenantsEventsInOrderOnceAndMarksThem() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        write(TENANT, "coretest.item.deleted", first);
        write(OTHER, "coretest.item.deleted", second);
        write(TENANT, "coretest.item.created", UUID.randomUUID()); // nobody subscribes: published, not delivered
        OutboxRelay relay = OutboxRelay.of(db, bus, JSON, Clock.systemUTC()).orElseThrow();
        assertThat(relay.runOnce().published()).isEqualTo(3);
        assertThat(received).extracting(EventEnvelope::aggregateId).containsExactly(first, second);
        assertThat(received).extracting(e -> e.tenantId().orElseThrow()).containsExactly(TENANT, OTHER);
        EventEnvelope event = received.get(0);
        assertThat(event.module()).isEqualTo("coretest");
        assertThat(event.traceId()).contains("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(relay.runOnce().published()).as("nothing is published twice").isZero();
        assertThat(received).hasSize(2);
    }

    @Test
    void aFailedPublishLeavesTheEventPendingAndTheSameIdComesAgain() {
        UUID aggregate = UUID.randomUUID();
        write(TENANT, "coretest.item.deleted", aggregate);
        AtomicInteger attempts = new AtomicInteger();
        List<UUID> ids = new CopyOnWriteArrayList<>();
        TestEventBus failing = new TestEventBus();
        failing.subscribe("coretest", Set.of("coretest.item.deleted"), e -> {
            ids.add(e.id());
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("broker down");
            }
        });
        OutboxRelay relay = OutboxRelay.of(db, failing, JSON, Clock.systemUTC())
                .orElseThrow();
        assertThatThrownBy(relay::runOnce).isInstanceOf(IllegalStateException.class);
        assertThat(relay.runOnce().published()).isEqualTo(1);
        assertThat(ids).hasSize(2).as("a redelivery carries the same id").containsOnly(ids.get(0));
    }

    @Test
    void withoutARelayRoleTheApplicationRoleCannotRelayAndSaysSo() {
        assertThat(OutboxRelay.of(withoutRelay, bus, JSON, Clock.systemUTC())).isEmpty();
    }

    @Test
    void publishedEventsArePrunedAfterTheRetention() {
        write(TENANT, "coretest.item.deleted", UUID.randomUUID());
        drain();
        Clock later = Clock.offset(Clock.systemUTC(), Duration.ofDays(8));
        OutboxRelay relay = OutboxRelay.of(db, bus, JSON, later).orElseThrow();
        assertThat(relay.runOnce().pruned()).isPositive();
        as(TENANT);
        assertThat(db.tx().read(() -> db.jdbc().sql("SELECT count(*) FROM coretest.outbox_event "
                + "WHERE tenant_id = :tenantId AND published_at IS NOT NULL").query(Integer.class).single())).isZero();
    }

    @Test
    void theInboxHandlesAnEventOnceInsideItsTenantAndKeepsFailuresForTheRetry() {
        InboxProcessor inbox = new InboxProcessor(db, Clock.systemUTC());
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), "other", "other.thing.deleted", Optional.of(TENANT),
                "other.thing", UUID.randomUUID(), Instant.now(), "{}", Optional.empty());
        List<Optional<UUID>> tenants = new CopyOnWriteArrayList<>();
        EventHandler handler = e -> {
            tenants.add(CallContextHolder.require().tenantId());
            db.jdbc().sql("INSERT INTO coretest.item (id, tenant_id, name) VALUES (:id, :tenantId, 'from-event')")
                    .param("id", UUID.randomUUID()).update();
        };
        assertThat(inbox.deliver(event, handler)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        assertThat(inbox.deliver(event, handler)).isEqualTo(InboxProcessor.Outcome.DUPLICATE);
        assertThat(tenants).containsExactly(Optional.of(TENANT));
        assertThat(CallContextHolder.current()).as("the caller's context is restored").isEmpty();
        as(TENANT);
        assertThat(db.tx().read(() -> db.jdbc().sql("SELECT count(*) FROM coretest.item WHERE tenant_id = :tenantId "
                + "AND name = 'from-event'").query(Integer.class).single())).isEqualTo(1);
        CallContextHolder.clear();

        EventEnvelope failing = new EventEnvelope(UUID.randomUUID(), "other", "other.thing.deleted",
                Optional.of(OTHER), "other.thing", UUID.randomUUID(), Instant.now(), "{}", Optional.empty());
        assertThatThrownBy(() -> inbox.deliver(failing, e -> {
            db.jdbc().sql("INSERT INTO coretest.item (id, tenant_id, name) VALUES (:id, :tenantId, 'half')")
                    .param("id", UUID.randomUUID()).update();
            throw new IllegalStateException("not yet");
        })).isInstanceOf(IllegalStateException.class);
        as(OTHER);
        assertThat(db.tx().read(() -> db.jdbc().sql("SELECT processing_error FROM coretest.inbox_event "
                + "WHERE tenant_id = :tenantId AND event_id = :id").param("id", failing.id()).query(String.class)
                .single())).contains("not yet");
        assertThat(db.tx().read(() -> db.jdbc().sql("SELECT count(*) FROM coretest.item WHERE tenant_id = :tenantId "
                + "AND name = 'half'").query(Integer.class).single())).as("the failed work rolled back").isZero();
        CallContextHolder.clear();
        assertThat(inbox.deliver(failing, handler)).as("the retry runs").isEqualTo(InboxProcessor.Outcome.PROCESSED);
        assertThat(inbox.deliver(failing, handler)).isEqualTo(InboxProcessor.Outcome.DUPLICATE);
    }

    @Test
    void theRuntimeSubscribesAndRelaysOnItsOwn() throws Exception {
        List<EventEnvelope> handled = new CopyOnWriteArrayList<>();
        EventSubscriptions subscriptions = new EventSubscriptions() {
            @Override
            public String module() {
                return "coretest";
            }

            @Override
            public Map<String, EventHandler> handlers() {
                return Map.of("coretest.item.deleted", handled::add);
            }
        };
        TestEventBus own = new TestEventBus();
        EventRuntime runtime = new EventRuntime(own,
                List.of(OutboxRelay.of(db, own, JSON, Clock.systemUTC()).orElseThrow()),
                List.of(subscriptions), List.of(new InboxProcessor(db, Clock.systemUTC())), Duration.ofMillis(50));
        runtime.start();
        try {
            UUID aggregate = UUID.randomUUID();
            write(OTHER, "coretest.item.deleted", aggregate);
            long until = System.currentTimeMillis() + 10_000;
            while (handled.stream().noneMatch(e -> e.aggregateId().equals(aggregate))
                    && System.currentTimeMillis() < until) {
                Thread.sleep(50);
            }
            assertThat(handled).extracting(EventEnvelope::aggregateId).contains(aggregate);
            assertThat(runtime.isRunning()).isTrue();
        } finally {
            runtime.stop();
        }
        assertThat(runtime.isRunning()).isFalse();
        EventRuntime off = EventRuntime.off();
        off.start();
        assertThat(off.isRunning()).isFalse();
        assertThat(off.relayOnce()).isZero();
        assertThatThrownBy(() -> new EventRuntime(own, List.of(), List.of(subscriptions), List.of(), Duration.ZERO))
                .isInstanceOf(IllegalStateException.class);
    }

    private static void insertRaw(UUID tenant, String type, String aggregateType, String payload) {
        as(tenant);
        db.tx().write(() -> db.jdbc().sql("INSERT INTO coretest.outbox_event (tenant_id, event_type, aggregate_type, "
                + "aggregate_id, payload) VALUES (:tenantId, :type, :aggregateType, :id, :payload)")
                .param("type", type).param("aggregateType", aggregateType).param("id", UUID.randomUUID())
                .param("payload", payload).update());
        CallContextHolder.clear();
    }

    @Test
    void aRowThatIsNotTheModulesOwnEventIsSetAsideAndTheRestGoesOn() {
        insertRaw(TENANT, "other.item.deleted", "other.item", "{}");
        insertRaw(TENANT, "coretest.item.deleted", "coretest.item", "plain text, not JSON");
        UUID after = UUID.randomUUID();
        write(OTHER, "coretest.item.deleted", after);
        OutboxRelay relay = OutboxRelay.of(db, bus, JSON, Clock.systemUTC()).orElseThrow();
        OutboxRelay.Pass pass = relay.runOnce();
        assertThat(pass.setAside()).isOne();
        assertThat(pass.published()).isEqualTo(2);
        assertThat(received).extracting(EventEnvelope::data).contains("plain text, not JSON");
        assertThat(received).extracting(EventEnvelope::aggregateId).contains(after);
        EventEnvelope text = received.stream().filter(e -> e.data().startsWith("plain")).findFirst().orElseThrow();
        assertThat(EventEnvelope.fromJson(JSON, text.toJson(JSON))).as("a text payload travels as text")
                .isEqualTo(text);
        as(TENANT);
        assertThat(db.tx().read(() -> db.jdbc().sql("SELECT publish_error FROM coretest.outbox_event "
                + "WHERE tenant_id = :tenantId AND event_type = 'other.item.deleted'").query(String.class).single()))
                .contains("not an event of module coretest");
        CallContextHolder.clear();
        assertThat(relay.runOnce().busy()).as("a set-aside row is not taken again").isFalse();
        assertThatThrownBy(() -> new OutboxWriter("coretest", db.jdbc(), JSON).publish("other.item.deleted",
                "coretest.item", UUID.randomUUID(), Map.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OutboxWriter("coretest", db.jdbc(), JSON).publish("coretest.item.deleted",
                "other.item", UUID.randomUUID(), Map.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aBatchStopsTakingRowsAfterItsTimeAndCommitsWhatWasConfirmed() {
        for (int i = 0; i < 3; i++) {
            write(TENANT, "coretest.item.deleted", UUID.randomUUID());
        }
        OutboxRelay hurried = OutboxRelay.of(db, bus, JSON, Clock.systemUTC(),
                new OutboxRelay.Settings(100, Duration.ofDays(7), Duration.ofDays(30), Duration.ZERO)).orElseThrow();
        assertThat(hurried.runOnce().published()).as("the first row always goes").isOne();
        assertThat(hurried.runOnce().published()).isOne();
        assertThat(received).hasSize(2);
        assertThatThrownBy(() -> new OutboxRelay.Settings(100, Duration.ofDays(7), Duration.ofDays(30),
                Duration.ofSeconds(31))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OutboxRelay.Settings(0, Duration.ofDays(7), Duration.ofDays(30),
                Duration.ofSeconds(1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theRelayRoleReachesTheOutboxAndInboxOnlyAndMustBeARoleOfItsOwn() {
        DataSource relay = db.relayDataSource().orElseThrow();
        JdbcClient jdbc = JdbcClient.create(relay);
        assertThat(jdbc.sql("SELECT count(*) FROM coretest.outbox_event").query(Integer.class).single()).isNotNull();
        assertThat(jdbc.sql("SELECT count(*) FROM coretest.inbox_event").query(Integer.class).single()).isNotNull();
        assertThatThrownBy(() -> jdbc.sql("SELECT count(*) FROM coretest.item").query(Integer.class).single())
                .as("no business table").hasStackTraceContaining("permission denied");
        assertThatThrownBy(() -> jdbc.sql("UPDATE coretest.outbox_event SET payload = '{}'").update())
                .as("only published_at and publish_error").hasStackTraceContaining("permission denied");
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO coretest.outbox_event (event_type, aggregate_type, "
                + "aggregate_id, payload) VALUES ('coretest.x.y', 'coretest.x', gen_random_uuid(), '{}')").update())
                .as("never writes events").hasStackTraceContaining("permission denied");
        MockEnvironment shared = environment(false)
                .withProperty("mysms.coretest.datasource.relay-username", database.username())
                .withProperty("mysms.coretest.datasource.relay-password", database.password());
        assertThatThrownBy(() -> ModuleDatabase.open("coretest", shared, true))
                .hasStackTraceContaining("the relay role must be a role of its own");
    }

    @Test
    void theInboxIsKeptPerTenantAndProcessedRowsArePrunedAfterTheirRetention() {
        InboxProcessor inbox = new InboxProcessor(db, Clock.systemUTC());
        UUID id = UUID.randomUUID();
        EventEnvelope mine = new EventEnvelope(id, "other", "other.thing.deleted", Optional.of(TENANT),
                "other.thing", UUID.randomUUID(), Instant.now(), "{}", Optional.empty());
        EventEnvelope theirs = new EventEnvelope(id, "other", "other.thing.deleted", Optional.of(OTHER),
                "other.thing", UUID.randomUUID(), Instant.now(), "{}", Optional.empty());
        AtomicInteger handled = new AtomicInteger();
        assertThat(inbox.deliver(theirs, e -> handled.incrementAndGet())).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        assertThat(inbox.deliver(mine, e -> handled.incrementAndGet()))
                .as("one tenant's event id never stands in for another tenant's")
                .isEqualTo(InboxProcessor.Outcome.PROCESSED);
        assertThat(handled.get()).isEqualTo(2);
        Clock later = Clock.offset(Clock.systemUTC(), Duration.ofDays(31));
        assertThat(OutboxRelay.of(db, bus, JSON, later).orElseThrow().runOnce().inboxPruned())
                .isGreaterThanOrEqualTo(2);
        as(TENANT);
        assertThat(db.tx().read(() -> db.jdbc().sql("SELECT count(*) FROM coretest.inbox_event "
                + "WHERE tenant_id = :tenantId AND event_id = :id").param("id", id).query(Integer.class).single()))
                .isZero();
        CallContextHolder.clear();
    }

    private static EventSubscriptions subscriptionsOf(String module, Map<String, EventHandler> handlers) {
        return new EventSubscriptions() {
            @Override
            public String module() {
                return module;
            }

            @Override
            public Map<String, EventHandler> handlers() {
                return handlers;
            }
        };
    }

    @Test
    void aModulesHandlersAreMergedOnceItRefusesForeignEventsAndItsHealthShowsAFailingRelay() {
        InboxProcessor inbox = new InboxProcessor(db, Clock.systemUTC());
        List<String> handled = new CopyOnWriteArrayList<>();
        TestEventBus own = new TestEventBus();
        EventRuntime runtime = new EventRuntime(own, List.of(), List.of(
                subscriptionsOf("coretest", Map.of("coretest.item.deleted", e -> handled.add("deleted"))),
                subscriptionsOf("coretest", Map.of("coretest.item.created", e -> handled.add("created")))),
                List.of(inbox), Duration.ofSeconds(1));
        runtime.start();
        try {
            own.publish(new EventEnvelope(UUID.randomUUID(), "coretest", "coretest.item.created", Optional.of(TENANT),
                    "coretest.item", UUID.randomUUID(), Instant.now(), "{}", Optional.empty()));
            own.publish(new EventEnvelope(UUID.randomUUID(), "coretest", "coretest.item.deleted", Optional.of(TENANT),
                    "coretest.item", UUID.randomUUID(), Instant.now(), "{}", Optional.empty()));
            // a module that claims another module's event: rejected and parked, never handled
            own.publish(new EventEnvelope(UUID.randomUUID(), "hr", "coretest.item.deleted", Optional.of(TENANT),
                    "coretest.item", UUID.randomUUID(), Instant.now(), "{}", Optional.empty()));
            assertThat(handled).containsExactly("created", "deleted");
            assertThat(runtime.health().up()).isTrue();
        } finally {
            runtime.stop();
        }
        assertThatThrownBy(() -> new EventRuntime(own, List.of(), List.of(
                subscriptionsOf("coretest", Map.of("coretest.item.deleted", e -> { })),
                subscriptionsOf("coretest", Map.of("coretest.item.deleted", e -> { }))),
                List.of(inbox), Duration.ofSeconds(1))).hasMessageContaining("two handlers");

        TestEventBus broken = new TestEventBus();
        broken.subscribe("coretest", Set.of("coretest.item.deleted"), e -> {
            throw new IllegalStateException("broker down");
        });
        write(TENANT, "coretest.item.deleted", UUID.randomUUID());
        EventRuntime failing = new EventRuntime(broken,
                List.of(OutboxRelay.of(db, broken, JSON, Clock.systemUTC()).orElseThrow()), List.of(), List.of(),
                Duration.ofSeconds(1));
        for (int i = 0; i < 3; i++) {
            assertThat(failing.relayOnce()).isZero();
        }
        EventRuntime.Health health = failing.health();
        assertThat(health.up()).isFalse();
        assertThat(health.details()).containsKey("relay.coretest").containsEntry("bus", "test-bus");
        assertThat(new EventsHealthIndicator(failing).health().getStatus().getCode()).isEqualTo("DOWN");
        assertThat(new EventsHealthIndicator(EventRuntime.off()).health().getStatus().getCode()).isEqualTo("UP");
    }

    @Test
    void theEnvelopeTravelsAsACloudEvent() {
        EventEnvelope event = new EventEnvelope(EventEnvelope.idOf("hr", 7), "hr", "hr.staff.deleted",
                Optional.of(TENANT), "hr.staff", UUID.randomUUID(), Instant.parse("2026-11-07T10:00:00Z"),
                "{\"id\":\"x\"}", Optional.of("4bf92f3577b34da6a3ce929d0e0e4736"));
        String text = event.toJson(JSON);
        assertThat(text).contains("\"specversion\":\"1.0\"", "\"source\":\"/mysms/hr\"", "\"tenantid\"");
        assertThat(EventEnvelope.fromJson(JSON, text)).isEqualTo(event);
        assertThat(EventEnvelope.idOf("hr", 7)).isEqualTo(event.id()).isNotEqualTo(EventEnvelope.idOf("sis", 7));
        EventEnvelope global = new EventEnvelope(UUID.randomUUID(), "master", "master.country.deleted",
                Optional.empty(), "master.country", UUID.randomUUID(), Instant.parse("2026-11-07T10:00:00Z"), null,
                null);
        assertThat(EventEnvelope.fromJson(JSON, global.toJson(JSON))).isEqualTo(global);
        assertThatThrownBy(() -> EventEnvelope.fromJson(JSON, "{\"specversion\":\"1.0\",\"source\":\"/other\"}"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EventEnvelope.fromJson(JSON, "{\"specversion\":\"0.3\"}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
