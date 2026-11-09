package com.mysms.messaging.rabbitmq;

import com.mysms.core.events.EventBus;
import com.mysms.core.events.EventEnvelope;
import com.mysms.core.events.EventHandler;
import com.mysms.core.events.RejectedEventException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link EventBus} on RabbitMQ (the default bus). One durable topic exchange carries every event, routed by its type
 * (`hr.staff.deleted`). Each consuming module has one durable quorum queue, `mysms.<module>.events`, bound to the
 * types it handles: replicas of the module share it, other modules have their own.
 *
 * <ul>
 *   <li><b>Never lost on the way in.</b> Publishing waits for the broker's confirm, so the relay marks an outbox row
 *   published only once the event is safe. An event no queue is bound to yet (its consumer was never started) goes to
 *   the exchange's alternate exchange and waits in `<exchange>.unrouted` for {@link #replayUnrouted(int)}, instead
 *   of being dropped. A failed publish closes the publishing channel; the next publish opens a new one.</li>
 *   <li><b>Authentic.</b> Every message carries the AMQP `user-id` of the connection that published it, which the
 *   broker verifies. A consumer accepts an event only from the broker user of the module it claims to come from
 *   ({@link Settings#publisherUser()}, `mysms-<module>` by default), and the broker's topic permissions limit each
 *   user to its own module's routing keys (DEPLOYMENT.md "Events").</li>
 *   <li><b>Retried with a growing delay.</b> Deliveries are acknowledged only after the module's inbox transaction
 *   committed. A failure is returned to the queue after a delay that doubles with each delivery, up to
 *   {@link Settings#maxRetryDelay()}; after the delivery limit the message goes to `mysms.<module>.dead` through the
 *   dead-letter exchange, for {@link #replayDead(String, int)} once the cause is fixed. An unreadable, unauthentic or
 *   {@link RejectedEventException rejected} event is dead-lettered at once.</li>
 *   <li><b>Self-healing consumers.</b> Connection loss is recovered by the client; a consumer the broker cancels (its
 *   queue deleted or moved) or whose channel failed subscribes again.</li>
 * </ul>
 */
public final class RabbitEventBus implements EventBus {

    /**
     * @param uri               `amqp(s)://user:password@host:port/vhost` (from the environment)
     * @param exchange          the events exchange
     * @param deliveryLimit     returns of one message to its queue before it is dead-lettered (it is delivered
     *                          once more than this)
     * @param prefetch          unacknowledged deliveries per consumer
     * @param publishTimeout    how long a publish waits for its confirm
     * @param retryDelay        delay before a failed delivery is returned, doubled at each further delivery
     * @param maxRetryDelay     the longest such delay
     * @param publisherUser     the broker user each module publishes as: `{module}` is replaced by the source module;
     *                          {@link #SELF} accepts only this bus's own user (one process, the monolith)
     * @param unroutedRetention how long an event no queue was bound to is kept for replay
     */
    public record Settings(URI uri, String exchange, int deliveryLimit, int prefetch, Duration publishTimeout,
                           Duration retryDelay, Duration maxRetryDelay, String publisherUser,
                           Duration unroutedRetention) {

        public Settings {
            if (deliveryLimit < 1 || prefetch < 1) {
                throw new IllegalArgumentException("delivery limit and prefetch must be positive");
            }
            if (publisherUser == null || publisherUser.isBlank()) {
                publisherUser = DEFAULT_PUBLISHER_USER;
            }
        }

        /** The defaults on an exchange: 5 returns, prefetch 10, 10 s confirms, 1 s to 60 s retries, 3 days unrouted. */
        public static Settings of(URI uri, String exchange, String publisherUser) {
            return new Settings(uri, exchange, 5, 10, Duration.ofSeconds(10), Duration.ofSeconds(1),
                    Duration.ofSeconds(60), publisherUser, Duration.ofDays(3));
        }
    }

    /** {@link Settings#publisherUser()}: events are accepted from this bus's own broker user only. */
    public static final String SELF = "self";
    /** The default {@link Settings#publisherUser()}: one broker user per module. */
    public static final String DEFAULT_PUBLISHER_USER = "mysms-{module}";

    private static final Logger LOG = LoggerFactory.getLogger(RabbitEventBus.class);
    private static final Pattern CONSUMER = Pattern.compile("^[a-z]+$");
    private static final Pattern TYPE = Pattern.compile("^[a-z0-9_]+(\\.[a-z0-9_]+)+$");
    private static final String DELIVERY_COUNT = "x-delivery-count";

    private final Settings settings;
    private final JsonMapper json;
    private final Connection connection;
    private final String user;
    private final Object publishLock = new Object();
    private final String deadExchange;
    private final String unrouted;
    private final ScheduledExecutorService scheduler;
    private Channel publisher;

    public RabbitEventBus(Settings settings, JsonMapper json, String clientName) {
        this.settings = settings;
        this.json = json;
        this.deadExchange = settings.exchange() + ".dead";
        this.unrouted = settings.exchange() + ".unrouted";
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(settings.uri());
            factory.setAutomaticRecoveryEnabled(true);
            factory.setTopologyRecoveryEnabled(true);
            this.user = factory.getUsername();
            this.connection = factory.newConnection(clientName);
            synchronized (publishLock) {
                publisherChannel();
            }
        } catch (IOException | TimeoutException | GeneralSecurityException | URISyntaxException e) {
            throw new IllegalStateException("cannot connect to RabbitMQ at " + settings.uri().getHost() + ":"
                    + settings.uri().getPort() + " (" + reason(e) + ")", e);
        }
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "rabbitmq-events");
            thread.setDaemon(true);
            return thread;
        });
        LOG.info("events bus: RabbitMQ {}:{} exchange {} as {}", settings.uri().getHost(), settings.uri().getPort(),
                settings.exchange(), user);
    }

    @Override
    public String name() {
        return "rabbitmq";
    }

    @Override
    public boolean connected() {
        return connection.isOpen();
    }

    /** The open publishing channel, declared topology included; a new one after a failure. Under publishLock. */
    private Channel publisherChannel() throws IOException {
        if (publisher == null || !publisher.isOpen()) {
            Channel channel = connection.createChannel();
            channel.confirmSelect();
            declareExchanges(channel);
            publisher = channel;
        }
        return publisher;
    }

    /**
     * The exchanges and the unrouted queue, declared (idempotently) by publishers and consumers alike. The unrouted
     * queue is created by the first process with its retention and then left as it is, so a process with another
     * setting (the operator command) still starts; change the retention on the broker with a policy.
     */
    private void declareExchanges(Channel channel) throws IOException {
        channel.exchangeDeclare(unrouted, BuiltinExchangeType.FANOUT, true);
        if (!queueExists(unrouted)) {
            channel.queueDeclare(unrouted, true, false, false, Map.of("x-queue-type", "quorum",
                    "x-message-ttl", settings.unroutedRetention().toMillis()));
        }
        channel.queueBind(unrouted, unrouted, "");
        channel.exchangeDeclare(settings.exchange(), BuiltinExchangeType.TOPIC, true, false,
                Map.of("alternate-exchange", unrouted));
        channel.exchangeDeclare(deadExchange, BuiltinExchangeType.DIRECT, true);
    }

    private boolean queueExists(String queue) {
        Channel probe = null;
        try {
            probe = connection.createChannel();
            probe.queueDeclarePassive(queue);
            return true;
        } catch (IOException | ShutdownSignalException e) {
            return false; // NOT_FOUND closes the probe channel
        } finally {
            quietlyClose(probe);
        }
    }

    /** The broker's own words for a failure (the client wraps them in exceptions without a message). */
    static String reason(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof ShutdownSignalException signal && signal.getReason() != null) {
                return String.valueOf(signal.getMessage());
            }
            if (t.getCause() == null) {
                return String.valueOf(t.getMessage());
            }
        }
        return "unknown";
    }

    @Override
    public void publish(EventEnvelope event) {
        byte[] body = event.toJson(json).getBytes(StandardCharsets.UTF_8);
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .contentType(EventEnvelope.CONTENT_TYPE)
                .deliveryMode(2)
                .messageId(event.id().toString())
                .type(event.type())
                .appId(event.module())
                .userId(user)
                .timestamp(Date.from(event.time()))
                .build();
        send(settings.exchange(), event.type(), properties, body, event.id().toString());
    }

    private void send(String exchange, String routingKey, AMQP.BasicProperties properties, byte[] body, String what) {
        synchronized (publishLock) {
            try {
                Channel channel = publisherChannel();
                channel.basicPublish(exchange, routingKey, properties, body);
                channel.waitForConfirmsOrDie(settings.publishTimeout().toMillis());
            } catch (IOException | TimeoutException | ShutdownSignalException e) {
                discardPublisher();
                throw new IllegalStateException("RabbitMQ did not confirm event " + what, e);
            } catch (InterruptedException e) {
                discardPublisher();
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while publishing event " + what, e);
            }
        }
    }

    /** After a failed publish the channel's confirm state is unknown (often it is closed): the next publish opens anew. */
    private void discardPublisher() {
        Channel failed = publisher;
        publisher = null;
        if (failed != null && failed.isOpen()) {
            try {
                failed.close();
            } catch (IOException | TimeoutException | ShutdownSignalException e) {
                LOG.debug("closing the failed publishing channel failed", e);
            }
        }
    }

    @Override
    public Subscription subscribe(String consumer, Set<String> eventTypes, EventHandler handler) {
        if (!CONSUMER.matcher(consumer).matches()) {
            throw new IllegalArgumentException("invalid consumer " + consumer);
        }
        eventTypes.forEach(type -> {
            if (!TYPE.matcher(type).matches()) {
                throw new IllegalArgumentException("invalid event type " + type);
            }
        });
        ConsumerSubscription subscription = new ConsumerSubscription(consumer, Set.copyOf(eventTypes), handler);
        try {
            subscription.start();
        } catch (IOException e) {
            throw new IllegalStateException(subscribeFailure(consumer, e), e);
        }
        return subscription;
    }

    private static String subscribeFailure(String consumer, IOException e) {
        String cause = reason(e);
        if (cause.contains("PRECONDITION_FAILED")) {
            return "cannot subscribe " + consumer + ": its queue " + queueOf(consumer) + " exists with other "
                    + "arguments (" + cause + "); see DEPLOYMENT.md \"Events\" for changing queue settings";
        }
        return "cannot subscribe " + consumer + " on RabbitMQ (" + cause + ")";
    }

    /** One module's consumer; subscribes again when the broker cancels it or its channel fails. */
    private final class ConsumerSubscription implements Subscription {

        private final String consumer;
        private final Set<String> types;
        private final EventHandler handler;
        private final Object lock = new Object();
        private volatile boolean closed;
        private Channel channel;
        private String tag;

        ConsumerSubscription(String consumer, Set<String> types, EventHandler handler) {
            this.consumer = consumer;
            this.types = types;
            this.handler = handler;
        }

        void start() throws IOException {
            synchronized (lock) {
                if (closed) {
                    return;
                }
                String queue = queueOf(consumer);
                String dead = deadQueueOf(consumer);
                Channel opened = connection.createChannel();
                try {
                    opened.basicQos(settings.prefetch());
                    declareExchanges(opened);
                    opened.queueDeclare(queue, true, false, false, Map.of(
                            "x-queue-type", "quorum",
                            "x-delivery-limit", settings.deliveryLimit(),
                            "x-dead-letter-exchange", deadExchange,
                            "x-dead-letter-routing-key", consumer));
                    opened.queueDeclare(dead, true, false, false, Map.of("x-queue-type", "quorum"));
                    opened.queueBind(dead, deadExchange, consumer);
                    for (String type : types) {
                        opened.queueBind(queue, settings.exchange(), type);
                    }
                    opened.addShutdownListener(cause -> {
                        if (!closed && !cause.isHardError() && !cause.isInitiatedByApplication()) {
                            LOG.warn("the channel of consumer {} failed ({}); it subscribes again", consumer,
                                    cause.getMessage());
                            restartLater();
                        }
                    });
                    tag = opened.basicConsume(queue, false, (t, message) -> deliver(opened, message),
                            cancelled -> {
                                LOG.warn("consumer {} was cancelled by the broker; it subscribes again", consumer);
                                restartLater();
                            });
                    channel = opened;
                } catch (IOException | RuntimeException e) {
                    quietlyClose(opened);
                    throw e;
                }
            }
        }

        private void restartLater() {
            if (!closed) {
                scheduler.schedule(this::restart, settings.retryDelay().toMillis(), TimeUnit.MILLISECONDS);
            }
        }

        private void restart() {
            synchronized (lock) {
                if (closed) {
                    return;
                }
                quietlyClose(channel);
                channel = null;
            }
            try {
                start();
                LOG.info("consumer {} subscribed again", consumer);
            } catch (IOException | RuntimeException e) {
                LOG.warn("consumer {} could not subscribe again; it retries", consumer, e);
                restartLater();
            }
        }

        private void deliver(Channel on, Delivery message) throws IOException {
            long delivery = message.getEnvelope().getDeliveryTag();
            EventEnvelope event;
            try {
                event = EventEnvelope.fromJson(json, new String(message.getBody(), StandardCharsets.UTF_8));
            } catch (RuntimeException e) {
                LOG.error("consumer {} received an unreadable message; it is dead-lettered", consumer);
                on.basicReject(delivery, false);
                return;
            }
            String expected = expectedPublisher(event.module());
            String actual = message.getProperties() == null ? null : message.getProperties().getUserId();
            if (!expected.equals(actual)) {
                LOG.error("consumer {} received event {} claiming module {} from broker user {} (expected {}); it is "
                        + "dead-lettered", consumer, event.id(), event.module(), actual, expected);
                on.basicReject(delivery, false);
                return;
            }
            try {
                handler.handle(event);
                on.basicAck(delivery, false);
            } catch (RejectedEventException e) {
                LOG.error("consumer {} rejected event {} ({}); it is dead-lettered", consumer, event.id(),
                        e.getMessage());
                on.basicReject(delivery, false);
            } catch (RuntimeException e) {
                long delay = retryDelay(deliveryCount(message));
                LOG.warn("consumer {} failed on event {} ({}); returned in {} ms", consumer, event.id(),
                        event.type(), delay);
                scheduler.schedule(() -> nack(on, delivery), delay, TimeUnit.MILLISECONDS);
            }
        }

        private void nack(Channel on, long delivery) {
            try {
                if (on.isOpen()) {
                    on.basicNack(delivery, false, true);
                }
            } catch (IOException | ShutdownSignalException e) {
                // the channel closed meanwhile: the broker returns the unacknowledged message itself
                LOG.debug("consumer {} could not return delivery {}", consumer, delivery, e);
            }
        }

        @Override
        public void close() {
            synchronized (lock) {
                closed = true;
                if (channel != null && channel.isOpen()) {
                    try {
                        channel.basicCancel(tag);
                    } catch (IOException | ShutdownSignalException e) {
                        LOG.debug("cancelling consumer {} failed", consumer, e);
                    }
                }
                quietlyClose(channel);
                channel = null;
            }
        }
    }

    private String expectedPublisher(String module) {
        return SELF.equals(settings.publisherUser()) ? user : settings.publisherUser().replace("{module}", module);
    }

    private long retryDelay(long deliveries) {
        long base = settings.retryDelay().toMillis();
        long max = settings.maxRetryDelay().toMillis();
        long delay = base << Math.min(deliveries, 30);
        return delay <= 0 || delay > max ? max : delay;
    }

    private static long deliveryCount(Delivery message) {
        Map<String, Object> headers = message.getProperties() == null ? null : message.getProperties().getHeaders();
        Object count = headers == null ? null : headers.get(DELIVERY_COUNT);
        return count instanceof Number n ? n.longValue() : 0;
    }

    private static void quietlyClose(Channel channel) {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
            } catch (IOException | TimeoutException | ShutdownSignalException e) {
                LOG.debug("closing a channel failed", e);
            }
        }
    }

    /**
     * Returns up to {@code max} dead-lettered events of a consumer to its queue, once the cause of their failure is
     * fixed; each is delivered afresh (and handled once, through the inbox). Run as a broker user that may set the
     * original publisher's `user-id` (the `impersonator` tag, DEPLOYMENT.md "Events").
     *
     * @return the number of events returned
     */
    public int replayDead(String consumer, int max) {
        if (!CONSUMER.matcher(consumer).matches()) {
            throw new IllegalArgumentException("invalid consumer " + consumer);
        }
        return move(deadQueueOf(consumer), max, response -> new String[] {"", queueOf(consumer)});
    }

    /**
     * Publishes again up to {@code max} events that no queue was bound to when they were first published (their
     * consumer has been started since); one still unroutable waits again. Same broker user as {@link #replayDead}.
     *
     * @return the number of events published again
     */
    public int replayUnrouted(int max) {
        return move(unrouted, max, response -> new String[] {settings.exchange(),
                response.getEnvelope().getRoutingKey()});
    }

    private interface Target {
        String[] of(GetResponse response);
    }

    private int move(String queue, int max, Target target) {
        int moved = 0;
        try (Channel source = connection.createChannel()) {
            long waiting = source.queueDeclarePassive(queue).getMessageCount();
            long limit = Math.min(max, waiting);
            while (moved < limit) {
                GetResponse response = source.basicGet(queue, false);
                if (response == null) {
                    break;
                }
                String[] to = target.of(response);
                AMQP.BasicProperties properties = response.getProps().builder()
                        .headers(withoutDeathHistory(response.getProps().getHeaders())).build();
                try {
                    send(to[0], to[1], properties, response.getBody(), String.valueOf(properties.getMessageId()));
                } catch (IllegalStateException e) {
                    source.basicNack(response.getEnvelope().getDeliveryTag(), false, true);
                    throw e;
                }
                source.basicAck(response.getEnvelope().getDeliveryTag(), false);
                moved++;
            }
        } catch (IOException | TimeoutException e) {
            throw new IllegalStateException("cannot replay " + queue + " (" + reason(e) + ")", e);
        }
        LOG.info("replayed {} events from {}", moved, queue);
        return moved;
    }

    private static Map<String, Object> withoutDeathHistory(Map<String, Object> headers) {
        if (headers == null) {
            return null;
        }
        Map<String, Object> kept = new HashMap<>(headers);
        kept.keySet().removeIf(k -> k.startsWith("x-death") || k.startsWith("x-first-death")
                || k.startsWith("x-last-death") || DELIVERY_COUNT.equals(k));
        return kept;
    }

    /** The durable queue of a consuming module. */
    public static String queueOf(String consumer) {
        return "mysms." + consumer + ".events";
    }

    /** Where a consuming module's events go after the delivery limit or a rejection. */
    public static String deadQueueOf(String consumer) {
        return "mysms." + consumer + ".dead";
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        try {
            if (connection.isOpen()) {
                connection.close();
            }
        } catch (IOException e) {
            LOG.warn("closing the RabbitMQ connection failed", e);
        }
    }
}
