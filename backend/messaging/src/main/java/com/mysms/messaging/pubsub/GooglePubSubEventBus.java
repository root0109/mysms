package com.mysms.messaging.pubsub;

import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.StatusCode;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.DeadLetterPolicy;
import com.google.pubsub.v1.ExpirationPolicy;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.RetryPolicy;
import com.google.pubsub.v1.SeekRequest;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;
import com.google.protobuf.ByteString;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Timestamp;
import com.mysms.messaging.events.EventBus;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.RejectedEventException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cloud Pub/Sub adapter for the {@link EventBus}. Each source module publishes to its own topic so IAM can limit a
 * service account to its own stream; each consuming module gets a durable subscription per source, shared by replicas
 * and filtered to that source. Source and type attributes are checked against the CloudEvents body.
 *
 * <p>Negative acknowledgements are retried with exponential backoff, and poison messages go to the consumer's
 * dead-letter topic.
 */
public final class GooglePubSubEventBus implements EventBus {

    public record Settings(String projectId, String topicPrefix, String subscriptionPrefix, Set<String> sourceModules,
                           boolean createResources, String emulatorHost, Duration topicRetention,
                           Duration publishTimeout, Duration retryDelay, Duration maxRetryDelay,
                           int maxDeliveryAttempts, long maxOutstandingMessages) {

        public Settings {
            if (!StringUtils.hasText(projectId)) {
                throw new IllegalArgumentException("Google Cloud project id is required");
            }
            if (!validPrefix(topicPrefix) || !validPrefix(subscriptionPrefix)) {
                throw new IllegalArgumentException("Pub/Sub topic and subscription prefixes must start with a letter "
                        + "and contain only letters, digits, '.', '_', '~', '+', '%' or '-'");
            }
            sourceModules = Set.copyOf(sourceModules);
            if (sourceModules.isEmpty() || sourceModules.stream().anyMatch(m -> !m.matches("^[a-z][a-z_]*$"))) {
                throw new IllegalArgumentException("Pub/Sub source modules must be lowercase module names");
            }
            emulatorHost = emulatorHost == null ? "" : emulatorHost.trim();
            if (topicRetention == null || topicRetention.compareTo(Duration.ofMinutes(10)) < 0
                    || topicRetention.compareTo(Duration.ofDays(31)) > 0) {
                throw new IllegalArgumentException("Pub/Sub topic retention must be between 10 minutes and 31 days");
            }
            if (publishTimeout == null || publishTimeout.isNegative() || publishTimeout.isZero()) {
                throw new IllegalArgumentException("Pub/Sub publish timeout must be positive");
            }
            if (retryDelay == null || retryDelay.isNegative() || retryDelay.isZero() || maxRetryDelay == null
                    || maxRetryDelay.compareTo(retryDelay) < 0 || maxRetryDelay.compareTo(Duration.ofMinutes(10)) > 0) {
                throw new IllegalArgumentException("Pub/Sub retry backoff must be positive and at most 10 minutes");
            }
            if (maxDeliveryAttempts < 5 || maxDeliveryAttempts > 100 || maxOutstandingMessages < 1) {
                throw new IllegalArgumentException("Pub/Sub delivery attempts must be 5-100 and flow control positive");
            }
            if (topicPrefix.length() + 16 > 255 || subscriptionPrefix.length() + 64 > 255) {
                throw new IllegalArgumentException("Pub/Sub resource prefixes leave too little room "
                        + "for resource names");
            }
        }

        private static boolean validPrefix(String prefix) {
            return prefix != null && prefix.matches("^[A-Za-z][A-Za-z0-9._~+%-]*$");
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(GooglePubSubEventBus.class);
    private static final Pattern TYPE = Pattern.compile("^[a-z0-9_]+(\\.[a-z0-9_]+)+$");
    private static final Pattern CONSUMER = Pattern.compile("^[a-z][a-z_]*$");
    private static final long START_TIMEOUT_SECONDS = 30;

    private final Settings settings;
    private final JsonMapper json;
    private final TopicAdminClient topicAdmin;
    private final SubscriptionAdminClient subscriptionAdmin;
    private final Map<String, Publisher> publishers = new ConcurrentHashMap<>();
    private final Set<String> ensuredTopics = ConcurrentHashMap.newKeySet();
    private final Set<String> ensuredSubscriptions = ConcurrentHashMap.newKeySet();
    private final List<Subscriber> subscribers = new CopyOnWriteArrayList<>();
    private final List<ManagedChannel> emulatorChannels = new CopyOnWriteArrayList<>();
    private final Object resourceLock = new Object();
    private final AtomicBoolean operational = new AtomicBoolean(true);
    private volatile boolean closed;

    public GooglePubSubEventBus(Settings settings, JsonMapper json) {
        this.settings = settings;
        this.json = json;
        if (settings.createResources()) {
            this.topicAdmin = createTopicAdmin();
            this.subscriptionAdmin = createSubscriptionAdmin();
        } else {
            this.topicAdmin = null;
            this.subscriptionAdmin = null;
        }
        LOG.info("events bus: Google Cloud Pub/Sub project {}{}", settings.projectId(),
                settings.emulatorHost().isBlank() ? "" : " (emulator " + settings.emulatorHost() + ")");
    }

    @Override
    public String name() {
        return "google-pubsub";
    }

    @Override
    public boolean connected() {
        return !closed && operational.get() && subscribers.stream().allMatch(Subscriber::isRunning);
    }

    @Override
    public void publish(EventEnvelope event) {
        if (!event.ownedBySource()) {
            throw new IllegalArgumentException("module " + event.module() + " does not own event " + event.type());
        }
        if (!settings.sourceModules().contains(event.module())) {
            throw new IllegalArgumentException("unknown Pub/Sub source module " + event.module());
        }
        String topic = topicId(event.module());
        ensureTopic(topic);
        PubsubMessage message = PubsubMessage.newBuilder()
                .setData(ByteString.copyFrom(event.toJson(json), StandardCharsets.UTF_8))
                .putAttributes("eventId", event.id().toString())
                .putAttributes("eventType", event.type())
                .putAttributes("sourceModule", event.module())
                .build();
        try {
            publishers.computeIfAbsent(topic, this::createPublisher).publish(message)
                    .get(settings.publishTimeout().toMillis(), TimeUnit.MILLISECONDS);
            operational.set(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            operational.set(false);
            throw new IllegalStateException("interrupted while publishing event " + event.id() + " to Pub/Sub", e);
        } catch (ExecutionException | TimeoutException e) {
            operational.set(false);
            throw new IllegalStateException("Google Cloud Pub/Sub did not confirm event " + event.id(), e);
        } catch (RuntimeException e) {
            operational.set(false);
            throw new IllegalStateException("cannot publish event " + event.id() + " to Google Cloud Pub/Sub", e);
        }
    }

    @Override
    public Subscription subscribe(String consumer, Set<String> eventTypes, EventHandler handler) {
        if (!CONSUMER.matcher(consumer).matches()) {
            throw new IllegalArgumentException("invalid consumer " + consumer);
        }
        if (eventTypes.isEmpty()) {
            return () -> { };
        }
        Map<String, Set<String>> typesBySource = typesBySource(eventTypes);
        PubSubSubscription subscription = new PubSubSubscription(consumer);
        try {
            for (Map.Entry<String, Set<String>> entry : typesBySource.entrySet()) {
                String source = entry.getKey();
                String topic = topicId(source);
                ensureTopic(topic);
                String subscriptionId = subscriptionId(consumer, source);
                String deadTopic = deadLetterTopicId(consumer);
                ensureDeadLetterResources(consumer, deadTopic);
                ensureSubscription(subscriptionId, topic, source, deadTopic, true);
                Subscriber subscriber = createSubscriber(subscriptionId, consumer, source, entry.getValue(), handler);
                subscription.add(subscriber);
            }
            return subscription;
        } catch (RuntimeException e) {
            subscription.close();
            throw e;
        }
    }

    private Map<String, Set<String>> typesBySource(Set<String> eventTypes) {
        Map<String, Set<String>> grouped = new LinkedHashMap<>();
        for (String eventType : new TreeSet<>(eventTypes)) {
            if (!TYPE.matcher(eventType).matches()) {
                throw new IllegalArgumentException("invalid event type " + eventType);
            }
            if (EventEnvelope.AUDIT_TYPE.equals(eventType)) {
                settings.sourceModules().stream().sorted().forEach(source ->
                        grouped.computeIfAbsent(source, ignored -> new TreeSet<>()).add(eventType));
            } else {
                String source = eventType.substring(0, eventType.indexOf('.'));
                if (!settings.sourceModules().contains(source)) {
                    throw new IllegalArgumentException("event type " + eventType
                            + " has no configured Pub/Sub source module " + source);
                }
                grouped.computeIfAbsent(source, ignored -> new TreeSet<>()).add(eventType);
            }
        }
        Map<String, Set<String>> immutable = new LinkedHashMap<>();
        grouped.forEach((source, types) -> immutable.put(source, Set.copyOf(types)));
        return immutable;
    }

    private Subscriber createSubscriber(String subscriptionId, String consumer, String source, Set<String> eventTypes,
                                        EventHandler handler) {
        String resource = SubscriptionName.of(settings.projectId(), subscriptionId).toString();
        MessageReceiver receiver = (message, reply) -> receive(consumer, source, eventTypes, handler, message, reply);
        Subscriber.Builder builder = Subscriber.newBuilder(resource, receiver)
                .setFlowControlSettings(FlowControlSettings.newBuilder()
                        .setMaxOutstandingElementCount(settings.maxOutstandingMessages()).build());
        configureEmulator(builder);
        Subscriber subscriber = builder.build();
        subscriber.addListener(new com.google.api.core.ApiService.Listener() {
            @Override
            public void failed(com.google.api.core.ApiService.State from, Throwable failure) {
                operational.set(false);
                LOG.error("Pub/Sub subscriber {} failed while in {}", subscriptionId, from, failure);
            }
        }, Runnable::run);
        try {
            subscriber.startAsync().awaitRunning(START_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            subscribers.add(subscriber);
            operational.set(true);
            return subscriber;
        } catch (TimeoutException | IllegalStateException e) {
            subscriber.stopAsync();
            throw new IllegalStateException("cannot start Pub/Sub subscription " + subscriptionId, e);
        }
    }

    private void receive(String consumer, String source, Set<String> eventTypes, EventHandler handler,
                         PubsubMessage message, AckReplyConsumer reply) {
        String eventType = message.getAttributesMap().get("eventType");
        String claimedSource = message.getAttributesMap().get("sourceModule");
        EventEnvelope event;
        try {
            if (!source.equals(claimedSource)) {
                throw new IllegalArgumentException("source attribute does not match subscription");
            }
            event = EventEnvelope.fromJson(json, message.getData().toStringUtf8());
            if (!source.equals(event.module()) || !event.type().equals(eventType) || !event.ownedBySource()) {
                throw new IllegalArgumentException("event source or type does not match Pub/Sub attributes");
            }
        } catch (RuntimeException e) {
            LOG.error("consumer {} received an unreadable or unauthentic event from Pub/Sub source {}; it is nacked",
                    consumer, source);
            reply.nack();
            return;
        }
        if (!eventTypes.contains(event.type())) {
            reply.ack();
            return;
        }
        try {
            handler.handle(event);
            reply.ack();
        } catch (RejectedEventException e) {
            parkRejected(consumer, message, event, reply, e);
        } catch (RuntimeException e) {
            LOG.warn("consumer {} failed on event {} ({}, {}); Pub/Sub will retry it", consumer, event.id(),
                    event.type(), e.getClass().getSimpleName());
            reply.nack();
        }
    }

    private void parkRejected(String consumer, PubsubMessage message, EventEnvelope event, AckReplyConsumer reply,
                              RejectedEventException rejection) {
        String topic = deadLetterTopicId(consumer);
        PubsubMessage parked = PubsubMessage.newBuilder()
                .setData(message.getData())
                .putAllAttributes(message.getAttributesMap())
                .putAttributes("mysmsDeadLetterConsumer", consumer)
                .putAttributes("mysmsDeadLetterReason", rejection.getClass().getSimpleName())
                .build();
        try {
            publishers.computeIfAbsent(topic, this::createPublisher).publish(parked)
                    .get(settings.publishTimeout().toMillis(), TimeUnit.MILLISECONDS);
            operational.set(true);
            LOG.error("consumer {} rejected event {} ({}, {}); it was published to the consumer dead-letter topic",
                    consumer, event.id(), event.type(), rejection.getClass().getSimpleName());
            reply.ack();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            operational.set(false);
            LOG.error("could not park rejected event {} for consumer {}; it will be retried", event.id(), consumer);
            reply.nack();
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            operational.set(false);
            LOG.error("could not park rejected event {} for consumer {}; it will be retried", event.id(), consumer);
            reply.nack();
        }
    }

    private Publisher createPublisher(String topicId) {
        Publisher.Builder builder = Publisher.newBuilder(TopicName.of(settings.projectId(), topicId).toString());
        configureEmulator(builder);
        try {
            return builder.build();
        } catch (IOException e) {
            throw new IllegalStateException("cannot create Pub/Sub publisher for topic " + topicId, e);
        }
    }

    private TopicAdminClient createTopicAdmin() {
        if (settings.emulatorHost().isBlank()) {
            try {
                return TopicAdminClient.create();
            } catch (IOException e) {
                throw new IllegalStateException("cannot create Pub/Sub topic admin client", e);
            }
        }
        try {
            TopicAdminSettings clientSettings = TopicAdminSettings.newBuilder()
                    .setTransportChannelProvider(channelProvider())
                    .setCredentialsProvider(NoCredentialsProvider.create())
                    .build();
            return TopicAdminClient.create(clientSettings);
        } catch (IOException e) {
            throw new IllegalStateException("cannot create Pub/Sub emulator topic admin client", e);
        }
    }

    private SubscriptionAdminClient createSubscriptionAdmin() {
        if (settings.emulatorHost().isBlank()) {
            try {
                return SubscriptionAdminClient.create();
            } catch (IOException e) {
                throw new IllegalStateException("cannot create Pub/Sub subscription admin client", e);
            }
        }
        try {
            SubscriptionAdminSettings clientSettings = SubscriptionAdminSettings.newBuilder()
                    .setTransportChannelProvider(channelProvider())
                    .setCredentialsProvider(NoCredentialsProvider.create())
                    .build();
            return SubscriptionAdminClient.create(clientSettings);
        } catch (IOException e) {
            throw new IllegalStateException("cannot create Pub/Sub emulator subscription admin client", e);
        }
    }

    private void configureEmulator(Publisher.Builder builder) {
        if (!settings.emulatorHost().isBlank()) {
            builder.setChannelProvider(channelProvider()).setCredentialsProvider(NoCredentialsProvider.create());
        }
    }

    private void configureEmulator(Subscriber.Builder builder) {
        if (!settings.emulatorHost().isBlank()) {
            builder.setChannelProvider(channelProvider()).setCredentialsProvider(NoCredentialsProvider.create());
        }
    }

    private TransportChannelProvider channelProvider() {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(settings.emulatorHost()).usePlaintext().build();
        emulatorChannels.add(channel);
        return FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
    }

    private void ensureTopic(String topicId) {
        if (!settings.createResources() || ensuredTopics.contains(topicId)) {
            return;
        }
        synchronized (resourceLock) {
            if (ensuredTopics.contains(topicId)) {
                return;
            }
            String name = TopicName.of(settings.projectId(), topicId).toString();
            try {
                Topic.Builder builder = Topic.newBuilder().setName(name);
                if (settings.emulatorHost().isBlank()) {
                    builder.setMessageRetentionDuration(protoDuration(settings.topicRetention()));
                }
                topicAdmin.createTopic(builder.build());
            } catch (ApiException e) {
                if (e.getStatusCode().getCode() != StatusCode.Code.ALREADY_EXISTS) {
                    throw new IllegalStateException("cannot create Pub/Sub topic " + topicId, e);
                }
                ensureTopicRetention(name, topicAdmin.getTopic(name));
            }
            ensuredTopics.add(topicId);
        }
    }

    private void ensureTopicRetention(String name, Topic topic) {
        if (settings.emulatorHost().isBlank()
                && (!topic.hasMessageRetentionDuration()
                || topic.getMessageRetentionDuration().getSeconds() < settings.topicRetention().getSeconds())) {
            Topic updated = topic.toBuilder().setMessageRetentionDuration(protoDuration(settings.topicRetention()))
                    .build();
            topicAdmin.updateTopic(updated, FieldMask.newBuilder().addPaths("message_retention_duration").build());
        }
    }

    private void ensureDeadLetterResources(String consumer, String deadTopic) {
        ensureTopic(deadTopic);
        ensureSubscription(deadLetterSubscriptionId(consumer), deadTopic, "", null, false);
    }

    private void ensureSubscription(String subscriptionId, String topicId, String source, String deadTopic,
                                    boolean withDeadLetter) {
        if (!settings.createResources() || ensuredSubscriptions.contains(subscriptionId)) {
            return;
        }
        synchronized (resourceLock) {
            if (ensuredSubscriptions.contains(subscriptionId)) {
                return;
            }
            String subscription = SubscriptionName.of(settings.projectId(), subscriptionId).toString();
            String topic = TopicName.of(settings.projectId(), topicId).toString();
            String filter = source.isBlank() ? "" : "attributes.sourceModule = \"" + source + "\"";
            com.google.pubsub.v1.Subscription.Builder builder = com.google.pubsub.v1.Subscription.newBuilder()
                    .setName(subscription)
                    .setTopic(topic)
                    .setFilter(filter)
                    .setMessageRetentionDuration(protoDuration(settings.topicRetention()))
                    .setRetainAckedMessages(true)
                    .setRetryPolicy(RetryPolicy.newBuilder()
                            .setMinimumBackoff(protoDuration(settings.retryDelay()))
                            .setMaximumBackoff(protoDuration(settings.maxRetryDelay())).build())
                    .setExpirationPolicy(ExpirationPolicy.getDefaultInstance());
            if (withDeadLetter) {
                builder.setDeadLetterPolicy(DeadLetterPolicy.newBuilder()
                        .setDeadLetterTopic(TopicName.of(settings.projectId(), deadTopic).toString())
                        .setMaxDeliveryAttempts(settings.maxDeliveryAttempts()).build());
            }
            try {
                subscriptionAdmin.createSubscription(builder.build());
                if (settings.emulatorHost().isBlank()) {
                    seekToRetentionWindow(subscription);
                }
            } catch (ApiException e) {
                if (e.getStatusCode().getCode() != StatusCode.Code.ALREADY_EXISTS) {
                    throw new IllegalStateException("cannot create Pub/Sub subscription " + subscriptionId, e);
                }
                validateExistingSubscription(subscription, topic, filter, deadTopic, withDeadLetter);
            }
            ensuredSubscriptions.add(subscriptionId);
        }
    }

    private void validateExistingSubscription(String name, String topic, String filter, String deadTopic,
                                              boolean withDeadLetter) {
        com.google.pubsub.v1.Subscription existing = subscriptionAdmin.getSubscription(name);
        boolean matches = topic.equals(existing.getTopic()) && filter.equals(existing.getFilter())
                && existing.getRetainAckedMessages()
                && existing.getMessageRetentionDuration().equals(protoDuration(settings.topicRetention()));
        if (withDeadLetter) {
            matches &= existing.hasDeadLetterPolicy()
                    && TopicName.of(settings.projectId(), deadTopic).toString()
                            .equals(existing.getDeadLetterPolicy().getDeadLetterTopic())
                    && existing.getDeadLetterPolicy().getMaxDeliveryAttempts() == settings.maxDeliveryAttempts();
        } else {
            matches &= !existing.hasDeadLetterPolicy();
        }
        matches &= existing.hasExpirationPolicy() && !existing.getExpirationPolicy().hasTtl();
        if (existing.hasRetryPolicy()) {
            matches &= existing.getRetryPolicy().getMinimumBackoff().equals(protoDuration(settings.retryDelay()))
                    && existing.getRetryPolicy().getMaximumBackoff().equals(protoDuration(settings.maxRetryDelay()));
        } else {
            matches = false;
        }
        if (!matches) {
            throw new IllegalStateException("Pub/Sub subscription " + name + " has different topic, filter, or "
                    + "dead-letter settings; update it with the documented topology before starting the service");
        }
    }

    private void seekToRetentionWindow(String subscription) {
        Instant rewindTo = Instant.now().minus(settings.topicRetention());
        Timestamp timestamp = Timestamp.newBuilder().setSeconds(rewindTo.getEpochSecond())
                .setNanos(rewindTo.getNano()).build();
        subscriptionAdmin.seek(SeekRequest.newBuilder().setSubscription(subscription).setTime(timestamp).build());
    }

    private static com.google.protobuf.Duration protoDuration(Duration duration) {
        return com.google.protobuf.Duration.newBuilder().setSeconds(duration.getSeconds())
                .setNanos(duration.getNano()).build();
    }

    /** Pub/Sub topic for the given source module. */
    public String topicId(String source) {
        return settings.topicPrefix() + source;
    }

    /** Durable subscription shared by replicas of one consumer module and fed by one source module. */
    public String subscriptionId(String consumer, String source) {
        return settings.subscriptionPrefix() + consumer + "-from-" + source;
    }

    /** Topic holding dead-letter events for one consumer module. */
    public String deadLetterTopicId(String consumer) {
        return settings.topicPrefix() + consumer + "-dead";
    }

    /** Durable subscription used by operators to inspect retained dead-letter events. */
    public String deadLetterSubscriptionId(String consumer) {
        return settings.subscriptionPrefix() + consumer + "-dead";
    }

    @Override
    public void close() {
        closed = true;
        subscribers.forEach(this::stopSubscriber);
        subscribers.clear();
        publishers.values().forEach(this::stopPublisher);
        publishers.clear();
        if (topicAdmin != null) {
            topicAdmin.close();
        }
        if (subscriptionAdmin != null) {
            subscriptionAdmin.close();
        }
        emulatorChannels.forEach(this::stopChannel);
        emulatorChannels.clear();
    }

    private void stopSubscriber(Subscriber subscriber) {
        try {
            if (subscriber.isRunning()) {
                subscriber.stopAsync().awaitTerminated(10, TimeUnit.SECONDS);
            }
        } catch (TimeoutException | IllegalStateException e) {
            LOG.debug("stopping a Pub/Sub subscriber failed", e);
        }
    }

    private void stopPublisher(Publisher publisher) {
        publisher.shutdown();
        try {
            publisher.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.debug("waiting for a Pub/Sub publisher to stop was interrupted", e);
        }
    }

    private void stopChannel(ManagedChannel channel) {
        channel.shutdown();
        try {
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            channel.shutdownNow();
        }
    }

    private final class PubSubSubscription implements Subscription {

        private final String consumer;
        private final List<Subscriber> ownSubscribers = new ArrayList<>();
        private boolean subscriptionClosed;

        PubSubSubscription(String consumer) {
            this.consumer = consumer;
        }

        synchronized void add(Subscriber subscriber) {
            if (subscriptionClosed) {
                stopSubscriber(subscriber);
                return;
            }
            ownSubscribers.add(subscriber);
        }

        @Override
        public synchronized void close() {
            if (subscriptionClosed) {
                return;
            }
            subscriptionClosed = true;
            ownSubscribers.forEach(subscriber -> {
                stopSubscriber(subscriber);
                subscribers.remove(subscriber);
            });
            ownSubscribers.clear();
            LOG.debug("stopped Pub/Sub subscriptions for consumer {}", consumer);
        }
    }
}
