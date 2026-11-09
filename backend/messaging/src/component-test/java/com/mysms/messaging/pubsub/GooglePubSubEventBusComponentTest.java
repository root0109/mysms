package com.mysms.messaging.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.TopicName;
import com.google.protobuf.ByteString;
import com.mysms.messaging.events.EventBus;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.RejectedEventException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Real Pub/Sub emulator: confirms, durable filtered subscriptions, and CloudEvents envelope preservation. */
class GooglePubSubEventBusComponentTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final String prefix = "t" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "-";
    private final String subscriptionPrefix = "s" + prefix;

    private GooglePubSubEventBus bus;

    @BeforeEach
    void open() {
        GooglePubSubEventBus.Settings settings = new GooglePubSubEventBus.Settings("mysms-test", prefix,
                subscriptionPrefix, Set.of("hr"), true, PubSubTestEmulator.host(), Duration.ofDays(3),
                Duration.ofSeconds(10), Duration.ofSeconds(1), Duration.ofSeconds(5), 5, 10);
        bus = new GooglePubSubEventBus(settings, JSON);
    }

    @AfterEach
    void close() {
        bus.close();
    }

    @Test
    void publishesACloudEventToItsDurableConsumerSubscription() throws Exception {
        List<EventEnvelope> received = new CopyOnWriteArrayList<>();
        EventBus.Subscription subscription = bus.subscribe("componenttest", Set.of("hr.staff.deleted"), received::add);
        try {
            EventEnvelope expected = new EventEnvelope(UUID.randomUUID(), "hr", "hr.staff.deleted",
                    Optional.of(UUID.randomUUID()), "hr.staff", UUID.randomUUID(), Instant.now(),
                    "{\"id\":\"synthetic\"}",
                    Optional.of("4bf92f3577b34da6a3ce929d0e0e4736"));

            bus.publish(expected);

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (received.isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
            assertThat(received).containsExactly(expected);
            assertThat(bus.connected()).isTrue();
        } finally {
            subscription.close();
        }
    }

    @Test
    void rejectedEventsArePublishedToTheConsumerDeadLetterTopicImmediately() throws Exception {
        List<PubsubMessage> parked = new CopyOnWriteArrayList<>();
        EventBus.Subscription subscription = bus.subscribe("componenttest", Set.of("hr.staff.deleted"), event -> {
            throw new RejectedEventException("not supported");
        });
        ManagedChannel channel = ManagedChannelBuilder.forTarget(PubSubTestEmulator.host()).usePlaintext().build();
        MessageReceiver deadLetterReceiver = (message, reply) -> {
            parked.add(message);
            reply.ack();
        };
        Subscriber deadLetterSubscriber = Subscriber.newBuilder(
                        SubscriptionName.of("mysms-test", bus.deadLetterSubscriptionId("componenttest")).toString(),
                        deadLetterReceiver)
                .setChannelProvider(FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel)))
                .setCredentialsProvider(NoCredentialsProvider.create()).build();
        try {
            deadLetterSubscriber.startAsync().awaitRunning(10, TimeUnit.SECONDS);
            EventEnvelope expected = new EventEnvelope(UUID.randomUUID(), "hr", "hr.staff.deleted",
                    Optional.of(UUID.randomUUID()), "hr.staff", UUID.randomUUID(), Instant.now(), "{}",
                    Optional.empty());

            bus.publish(expected);

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (parked.isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
            assertThat(parked).hasSize(1);
            assertThat(parked.get(0).getData().toStringUtf8()).isEqualTo(expected.toJson(JSON));
            assertThat(parked.get(0).getAttributesMap())
                    .containsEntry("mysmsDeadLetterConsumer", "componenttest")
                    .containsEntry("mysmsDeadLetterReason", "RejectedEventException");
        } finally {
            subscription.close();
            deadLetterSubscriber.stopAsync().awaitTerminated(10, TimeUnit.SECONDS);
            channel.shutdownNow();
        }
    }

    @Test
    void retriesAHandlerFailureInsteadOfAcknowledgingIt() throws Exception {
        AtomicInteger deliveries = new AtomicInteger();
        EventBus.Subscription subscription = bus.subscribe("componenttest", Set.of("hr.staff.deleted"), event -> {
            deliveries.incrementAndGet();
            throw new IllegalStateException("temporary failure");
        });
        try {
            bus.publish(event("hr", "hr.staff.deleted", "hr.staff"));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
            while (deliveries.get() < 2 && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
            assertThat(deliveries.get()).isGreaterThanOrEqualTo(2);
        } finally {
            subscription.close();
        }
    }

    @Test
    void rejectsAnEnvelopeWhoseBodyClaimsADifferentSource() throws Exception {
        List<EventEnvelope> received = new CopyOnWriteArrayList<>();
        CountDownLatch handled = new CountDownLatch(1);
        EventBus.Subscription subscription = bus.subscribe("componenttest", Set.of("hr.staff.deleted"), event -> {
            received.add(event);
            handled.countDown();
        });
        ManagedChannel channel = ManagedChannelBuilder.forTarget(PubSubTestEmulator.host()).usePlaintext().build();
        Publisher publisher = Publisher.newBuilder(TopicName.of("mysms-test", bus.topicId("hr")).toString())
                .setChannelProvider(FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel)))
                .setCredentialsProvider(NoCredentialsProvider.create()).build();
        try {
            EventEnvelope forged = event("sis", "hr.staff.deleted", "sis.student");
            PubsubMessage message = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFrom(forged.toJson(JSON), StandardCharsets.UTF_8))
                    .putAttributes("eventType", "hr.staff.deleted")
                    .putAttributes("sourceModule", "hr")
                    .build();
            publisher.publish(message).get(10, TimeUnit.SECONDS);

            assertThat(handled.await(2, TimeUnit.SECONDS)).isFalse();
            assertThat(received).isEmpty();
        } finally {
            subscription.close();
            publisher.shutdown();
            publisher.awaitTermination(10, TimeUnit.SECONDS);
            channel.shutdownNow();
        }
    }

    private static EventEnvelope event(String module, String type, String aggregateType) {
        return new EventEnvelope(UUID.randomUUID(), module, type, Optional.empty(), aggregateType, UUID.randomUUID(),
                Instant.now(), "{}", Optional.empty());
    }
}
