package com.mysms.messaging.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.messaging.events.EventBus;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.RejectedEventException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.json.JsonMapper;

/**
 * Real RabbitMQ: confirms, per-module queues, competing replicas, delayed redelivery, dead-lettering and replay,
 * events no queue was bound to, publisher authenticity, and recovery of the publishing channel and of consumers.
 */
class RabbitEventBusComponentTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID TENANT = UUID.fromString("0190b0a0-0000-7000-8000-00000000000a");
    private static final Duration RETRY = Duration.ofMillis(100);

    private String exchange;
    private RabbitEventBus bus;

    @BeforeEach
    void open() {
        exchange = "test." + UUID.randomUUID().toString().substring(0, 8);
        bus = new RabbitEventBus(settings(exchange, 3, RabbitEventBus.SELF), JSON, "mysms-test");
    }

    @AfterEach
    void close() {
        bus.close();
    }

    private static RabbitEventBus.Settings settings(String exchange, int deliveryLimit, String publisherUser) {
        return new RabbitEventBus.Settings(TestBroker.uri(), exchange, deliveryLimit, 10, Duration.ofSeconds(10),
                RETRY, Duration.ofMillis(400), publisherUser, Duration.ofHours(1));
    }

    /** A fresh consumer name of 16 random letters (consumer names allow letters only): no clash on a shared broker. */
    private static String consumer() {
        StringBuilder name = new StringBuilder("c");
        for (char c : UUID.randomUUID().toString().replace("-", "").substring(0, 16).toCharArray()) {
            name.append(Character.isDigit(c) ? (char) ('g' + (c - '0')) : c);
        }
        return name.toString();
    }

    private static EventEnvelope event(String type) {
        return new EventEnvelope(UUID.randomUUID(), "hr", type, Optional.of(TENANT), "hr.staff", UUID.randomUUID(),
                Instant.now(), "{\"id\":\"x\"}", Optional.of("4bf92f3577b34da6a3ce929d0e0e4736"));
    }

    /** Waits until the condition holds (it is evaluated once per poll: a dead-letter read consumes the message). */
    private static void await(BooleanSupplier condition) throws InterruptedException {
        long until = System.currentTimeMillis() + 20_000;
        boolean reached = condition.getAsBoolean();
        while (!reached && System.currentTimeMillis() < until) {
            Thread.sleep(50);
            reached = condition.getAsBoolean();
        }
        assertThat(reached).as("condition reached in time").isTrue();
    }

    @Test
    void everyModuleGetsEveryEventItHandlesAndReplicasShareTheirModulesDeliveries() throws Exception {
        String sis = consumer();
        String academics = consumer();
        List<EventEnvelope> toSis = new CopyOnWriteArrayList<>();
        List<EventEnvelope> toSisReplica = new CopyOnWriteArrayList<>();
        List<EventEnvelope> toAcademics = new CopyOnWriteArrayList<>();
        try (EventBus.Subscription a = bus.subscribe(sis, Set.of("hr.staff.deleted"), toSis::add);
             EventBus.Subscription b = bus.subscribe(sis, Set.of("hr.staff.deleted"), toSisReplica::add);
             EventBus.Subscription c = bus.subscribe(academics, Set.of("hr.staff.deleted", "hr.staff.created"),
                     toAcademics::add)) {
            EventEnvelope deleted = event("hr.staff.deleted");
            EventEnvelope created = event("hr.staff.created");
            for (int i = 0; i < 9; i++) {
                bus.publish(event("hr.staff.deleted"));
            }
            bus.publish(deleted);
            bus.publish(created);
            await(() -> toSis.size() + toSisReplica.size() == 10 && toAcademics.size() == 11);
            Thread.sleep(300);
            assertThat(toSis.size() + toSisReplica.size()).as("each event once per module").isEqualTo(10);
            assertThat(toAcademics).contains(deleted, created).hasSize(11);
            assertThat(toAcademics.stream().filter(e -> e.equals(deleted)).findFirst().orElseThrow())
                    .as("the envelope travels intact").isEqualTo(deleted);
            assertThat(bus.connected()).isTrue();
        }
    }

    @Test
    void aFailingHandlerGetsTheEventAgainAfterAGrowingDelayAndAPoisonEventIsDeadLetteredThenReplayed()
            throws Exception {
        String flaky = consumer();
        List<Long> at = new CopyOnWriteArrayList<>();
        List<EventEnvelope> done = new CopyOnWriteArrayList<>();
        try (EventBus.Subscription s = bus.subscribe(flaky, Set.of("hr.staff.deleted"), e -> {
            at.add(System.nanoTime());
            if (at.size() < 3) {
                throw new IllegalStateException("database busy");
            }
            done.add(e);
        })) {
            EventEnvelope event = event("hr.staff.deleted");
            bus.publish(event);
            await(() -> done.size() == 1);
            assertThat(at).hasSize(3);
            assertThat(done).containsExactly(event);
            long first = Duration.ofNanos(at.get(1) - at.get(0)).toMillis();
            long second = Duration.ofNanos(at.get(2) - at.get(1)).toMillis();
            assertThat(first).as("returned after the retry delay").isGreaterThanOrEqualTo(RETRY.toMillis());
            assertThat(second).as("the delay grows").isGreaterThanOrEqualTo(2 * RETRY.toMillis());
        }

        String poisoned = consumer();
        AtomicInteger tries = new AtomicInteger();
        AtomicInteger healed = new AtomicInteger();
        EventEnvelope poison = event("hr.staff.updated");
        try (EventBus.Subscription s = bus.subscribe(poisoned, Set.of("hr.staff.updated"), e -> {
            tries.incrementAndGet();
            throw new IllegalStateException("always");
        })) {
            bus.publish(poison);
            await(() -> waiting(RabbitEventBus.deadQueueOf(poisoned)) == 1);
            assertThat(tries.get()).as("delivered once more than the delivery limit, then parked").isEqualTo(4);
        }
        List<EventEnvelope> replayed = new CopyOnWriteArrayList<>();
        try (EventBus.Subscription s = bus.subscribe(poisoned, Set.of("hr.staff.updated"), e -> {
            healed.incrementAndGet();
            replayed.add(e);
        })) {
            assertThat(bus.replayDead(poisoned, 100)).as("the fixed consumer gets its parked event again").isOne();
            await(() -> replayed.size() == 1);
            assertThat(replayed).containsExactly(poison);
            assertThat(waiting(RabbitEventBus.deadQueueOf(poisoned))).isZero();
        }
    }

    @Test
    void unreadableUnauthenticAndRejectedEventsAreDeadLetteredAtOnce() throws Exception {
        String reader = consumer();
        AtomicInteger handled = new AtomicInteger();
        try (EventBus.Subscription s = bus.subscribe(reader, Set.of("hr.staff.deleted"), e -> {
            handled.incrementAndGet();
            throw new RejectedEventException("not ours");
        });
             Connection raw = factory().newConnection();
             Channel channel = raw.createChannel()) {
            channel.basicPublish(exchange, "hr.staff.deleted", null, "not json".getBytes(StandardCharsets.UTF_8));
            await(() -> waiting(RabbitEventBus.deadQueueOf(reader)) == 1);
            // a well-formed event without the broker-verified user-id of its module's publisher
            channel.basicPublish(exchange, "hr.staff.deleted", null,
                    event("hr.staff.deleted").toJson(JSON).getBytes(StandardCharsets.UTF_8));
            await(() -> waiting(RabbitEventBus.deadQueueOf(reader)) == 2);
            assertThat(handled.get()).isZero();
            bus.publish(event("hr.staff.deleted"));
            await(() -> waiting(RabbitEventBus.deadQueueOf(reader)) == 3);
            assertThat(handled.get()).as("a rejected event is not delivered again").isOne();
        }
    }

    @Test
    void theBrokerRefusesAForgedPublisherAndAModuleAcceptsOnlyItsSourcesBrokerUser() throws Exception {
        try (Connection raw = factory().newConnection()) {
            Channel channel = raw.createChannel();
            channel.confirmSelect();
            AMQP.BasicProperties forged = new AMQP.BasicProperties.Builder().userId("mysms-hr").build();
            channel.basicPublish(exchange, "hr.staff.deleted", forged, "{}".getBytes(StandardCharsets.UTF_8));
            assertThatThrownBy(() -> channel.waitForConfirmsOrDie(5_000))
                    .as("the broker verifies user-id against the connection").isInstanceOf(Exception.class);
        }
        String strict = consumer();
        AtomicInteger handled = new AtomicInteger();
        try (RabbitEventBus perModule = new RabbitEventBus(settings(exchange, 3, "mysms-{module}"), JSON, "t");
             EventBus.Subscription s = perModule.subscribe(strict, Set.of("hr.staff.deleted"),
                     e -> handled.incrementAndGet())) {
            bus.publish(event("hr.staff.deleted")); // published as the test's broker user, not mysms-hrms
            await(() -> waiting(RabbitEventBus.deadQueueOf(strict)) == 1);
            assertThat(handled.get()).isZero();
        }
    }

    @Test
    void anEventNoQueueIsBoundToYetWaitsAndIsDeliveredOnceItsConsumerExists() throws Exception {
        String late = consumer();
        EventEnvelope early = event("hr.staff.archived");
        bus.publish(early);
        await(() -> waiting(exchange + ".unrouted") == 1);
        List<EventEnvelope> received = new CopyOnWriteArrayList<>();
        try (EventBus.Subscription s = bus.subscribe(late, Set.of("hr.staff.archived"), received::add)) {
            assertThat(bus.replayUnrouted(100)).isOne();
            await(() -> received.size() == 1);
            assertThat(received).containsExactly(early);
            assertThat(waiting(exchange + ".unrouted")).isZero();
        }
        bus.publish(event("hr.staff.nobody"));
        await(() -> waiting(exchange + ".unrouted") == 1);
        assertThat(bus.replayUnrouted(100)).as("still unroutable: it waits again").isOne();
        await(() -> waiting(exchange + ".unrouted") == 1);
    }

    @Test
    void aFailedPublishOpensANewChannelAndACancelledConsumerSubscribesAgain() throws Exception {
        String module = consumer();
        List<EventEnvelope> received = new CopyOnWriteArrayList<>();
        try (EventBus.Subscription s = bus.subscribe(module, Set.of("hr.staff.deleted"), received::add);
             Connection raw = factory().newConnection();
             Channel admin = raw.createChannel()) {
            admin.exchangeDelete(exchange);
            assertThatThrownBy(() -> bus.publish(event("hr.staff.deleted")))
                    .as("the broker closes the channel: publishing to a missing exchange")
                    .isInstanceOf(IllegalStateException.class);
            // the next publish opens a new channel, which declares the exchange again (so does the consumer)
            admin.queueDelete(RabbitEventBus.queueOf(module)); // the broker cancels the consumer
            long until = System.currentTimeMillis() + 20_000;
            while (waitingOrMinus(RabbitEventBus.queueOf(module)) < 0 && System.currentTimeMillis() < until) {
                Thread.sleep(50);
            }
            assertThat(waitingOrMinus(RabbitEventBus.queueOf(module))).as("the consumer declared its queue again")
                    .isGreaterThanOrEqualTo(0);
            // each poll publishes once more until one arrives: the consumer binds its queue again meanwhile
            await(() -> {
                try {
                    bus.publish(event("hr.staff.deleted"));
                } catch (IllegalStateException e) {
                    return false;
                }
                return !received.isEmpty();
            });
        }
    }

    @Test
    void aQueueWithOtherArgumentsIsReportedAndNamesAndTheAddressAreChecked() {
        String module = consumer();
        try (EventBus.Subscription s = bus.subscribe(module, Set.of("hr.staff.deleted"), e -> { });
             RabbitEventBus other = new RabbitEventBus(settings(exchange, 7, RabbitEventBus.SELF), JSON, "t")) {
            assertThatThrownBy(() -> other.subscribe(module, Set.of("hr.staff.deleted"), e -> { }))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("other arguments");
        }
        assertThatThrownBy(() -> bus.subscribe("Bad-Name", Set.of("hr.staff.deleted"), e -> { }))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bus.subscribe("sis", Set.of("#"), e -> { }))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bus.replayDead("Bad-Name", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(bus.name()).isEqualTo("rabbitmq");
        assertThat(RabbitEventBus.queueOf("sis")).isEqualTo("mysms.sis.events");
        assertThat(RabbitEventBus.deadQueueOf("sis")).isEqualTo("mysms.sis.dead");
        assertThat(RabbitEventBus.Settings.of(TestBroker.uri(), "x", "").publisherUser())
                .isEqualTo(RabbitEventBus.DEFAULT_PUBLISHER_USER);
        assertThatThrownBy(() -> settings(exchange, 0, RabbitEventBus.SELF))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RabbitEventsAutoConfiguration().rabbitEventBus(new MockEnvironment(), JSON))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("MYSMS_RABBITMQ_URI");
        EventBus configured = new RabbitEventsAutoConfiguration().rabbitEventBus(new MockEnvironment()
                .withProperty("mysms.events.rabbitmq.uri", TestBroker.uri().toString())
                .withProperty("mysms.events.rabbitmq.exchange", exchange)
                .withProperty("mysms.events.rabbitmq.publisher-user", RabbitEventBus.SELF), JSON);
        configured.publish(event("hr.staff.created"));
        configured.close();
        assertThat(configured.connected()).isFalse();
        assertThatThrownBy(() -> new RabbitEventBus(RabbitEventBus.Settings.of(
                URI.create("amqp://guest:guest@localhost:1"), "x", ""), JSON, "t"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void theOperatorCommandReplaysAndExplainsItsUsage() throws Exception {
        String module = consumer();
        try (EventBus.Subscription s = bus.subscribe(module, Set.of("hr.staff.updated"), e -> {
            throw new RejectedEventException("parked");
        })) {
            bus.publish(event("hr.staff.updated"));
            await(() -> waiting(RabbitEventBus.deadQueueOf(module)) == 1);
        }
        Map<String, String> env = Map.of("MYSMS_RABBITMQ_URI", TestBroker.uri().toString(),
                "MYSMS_RABBITMQ_EXCHANGE", exchange);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream o = new PrintStream(out, true, StandardCharsets.UTF_8);
        PrintStream e = new PrintStream(err, true, StandardCharsets.UTF_8);
        assertThat(EventsAdmin.run(new String[] {"replay-dead", module}, env, o, e)).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8)).contains("returned 1 events");
        assertThat(waiting(RabbitEventBus.deadQueueOf(module))).isZero();
        assertThat(waiting(RabbitEventBus.queueOf(module))).isOne();
        assertThat(EventsAdmin.run(new String[] {"replay-unrouted", "5"}, env, o, e)).isZero();
        assertThat(EventsAdmin.run(new String[] {}, env, o, e)).isEqualTo(2);
        assertThat(EventsAdmin.run(new String[] {"replay-dead"}, env, o, e)).isEqualTo(2);
        assertThat(EventsAdmin.run(new String[] {"purge"}, env, o, e)).isEqualTo(2);
        assertThat(EventsAdmin.run(new String[] {"replay-dead", "Bad"}, env, o, e)).isOne();
        assertThat(EventsAdmin.run(new String[] {"replay-dead", module}, Map.of(), o, e)).isEqualTo(2);
        assertThat(err.toString(StandardCharsets.UTF_8)).contains("usage", "unknown command", "failed");
    }

    private static ConnectionFactory factory() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(TestBroker.uri());
            return factory;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static long waiting(String queue) {
        long count = waitingOrMinus(queue);
        return Math.max(count, 0);
    }

    /** Messages ready in a queue, or -1 when it does not exist. */
    private static long waitingOrMinus(String queue) {
        try (Connection raw = factory().newConnection(); Channel channel = raw.createChannel()) {
            return channel.queueDeclarePassive(queue).getMessageCount();
        } catch (Exception e) {
            return -1;
        }
    }
}
