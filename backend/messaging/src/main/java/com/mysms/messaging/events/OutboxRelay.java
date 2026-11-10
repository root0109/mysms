package com.mysms.messaging.events;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.tx.Transactions;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Drains one module's transactional outbox to the bus (database/README "Events"). Pending rows are taken in id order
 * with {@code FOR UPDATE SKIP LOCKED} (replicas share the work), each is published and confirmed by the bus, and the
 * batch is marked published in the same transaction — a crash before commit only republishes, and consumers drop the
 * repeat through their inbox. A batch stops taking rows after {@link Settings#maxBatchTime()}, well inside its
 * transaction timeout, so a slow broker commits what it confirmed instead of republishing the whole batch forever.
 * A row that cannot become an event — not the module's own type, or unreadable — is set aside with its reason in
 * `publish_error` and the rows after it go on. Published outbox rows and processed inbox rows are pruned after their
 * retention.
 *
 * <p>The relay reads every tenant's events, so it connects as the relay role (`mysms_relay`, BYPASSRLS) over a pool of
 * its own and runs these statements only — never a business query; that role may read the outbox and inbox, mark and
 * prune them, and nothing else. Without a relay role it uses the application role only when that role itself bypasses
 * row-level security (a superuser in local development); otherwise it does not start and says so, instead of silently
 * publishing nothing.
 */
public final class OutboxRelay {

    /**
     * @param batchSize       rows taken per transaction
     * @param outboxRetention how long published rows are kept (for diagnosis) before they are pruned
     * @param inboxRetention  how long processed inbox rows are kept; longer than any redelivery (a dead-lettered event
     *                        replayed after it is handled again)
     * @param maxBatchTime    publishing stops taking rows of a batch after this; at most half the transaction timeout
     */
    public record Settings(int batchSize, Duration outboxRetention, Duration inboxRetention, Duration maxBatchTime) {

        public Settings {
            if (batchSize < 1 || maxBatchTime.isNegative() || maxBatchTime.compareTo(TRANSACTION_TIMEOUT.dividedBy(2)) > 0) {
                throw new IllegalArgumentException("relay batch size must be positive and its max batch time at most "
                        + TRANSACTION_TIMEOUT.dividedBy(2));
            }
        }

        /** 100 rows, 7 days of published outbox rows, 30 days of processed inbox rows, 20 s per batch. */
        public static Settings defaults() {
            return new Settings(100, Duration.ofDays(7), Duration.ofDays(30), Duration.ofSeconds(20));
        }
    }

    /** What one pass did. */
    public record Pass(int published, int setAside, int pruned, int inboxPruned) {

        /** Whether rows were taken: the caller runs another pass. */
        public boolean busy() {
            return published + setAside > 0;
        }
    }

    static final Duration TRANSACTION_TIMEOUT = Duration.ofSeconds(60);
    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);
    private static final Pattern SCHEMA = Pattern.compile("^[a-z]+$");
    private static final int MAX_ERROR = 500;
    private static final String BYPASSES = """
            SELECT rolsuper OR rolbypassrls FROM pg_roles WHERE rolname = current_user
            """;

    private final String module;
    private final EventBus bus;
    private final JsonMapper json;
    private final Clock clock;
    private final Settings settings;
    private final JdbcClient jdbc;
    private final PlatformTransactionManager transactions;
    private final TransactionDefinition writeDefinition;
    private final String pending;
    private final String markPublished;
    private final String setAside;
    private final String prune;
    private final String pruneInbox;

    private OutboxRelay(String module, DataSource relayDataSource, EventBus bus, JsonMapper json, Clock clock,
                        Settings settings) {
        if (!SCHEMA.matcher(module).matches()) {
            throw new IllegalArgumentException("invalid module " + module);
        }
        this.module = module;
        this.bus = bus;
        this.json = json;
        this.clock = clock;
        this.settings = settings;
        this.jdbc = JdbcClient.create(relayDataSource);
        // the relay role reads every tenant's outbox (BYPASSRLS), so its transactions bind no tenant
        this.transactions = new DataSourceTransactionManager(relayDataSource);
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        definition.setTimeout(Math.toIntExact(TRANSACTION_TIMEOUT.toSeconds()));
        this.writeDefinition = definition;
        this.pending = """
                SELECT id, tenant_id, event_type, aggregate_type, aggregate_id, payload, occurred_at
                  FROM {schema}.outbox_event
                 WHERE published_at IS NULL AND publish_error IS NULL
                 ORDER BY id
                 LIMIT :limit
                   FOR UPDATE SKIP LOCKED
                """.replace("{schema}", module);
        this.markPublished = "UPDATE {schema}.outbox_event SET published_at = :now WHERE id = :id"
                .replace("{schema}", module);
        this.setAside = "UPDATE {schema}.outbox_event SET publish_error = :error WHERE id = :id"
                .replace("{schema}", module);
        this.prune = """
                DELETE FROM {schema}.outbox_event
                 WHERE id IN (SELECT id FROM {schema}.outbox_event
                               WHERE published_at IS NOT NULL AND published_at < :before
                               ORDER BY id
                               LIMIT 500)
                """.replace("{schema}", module);
        this.pruneInbox = """
                DELETE FROM {schema}.inbox_event
                 WHERE (tenant_id, event_id) IN (SELECT tenant_id, event_id FROM {schema}.inbox_event
                                                  WHERE processed_at IS NOT NULL AND processed_at < :before
                                                  LIMIT 500)
                """.replace("{schema}", module);
    }

    /** The relay of a module database with the default settings, or empty (logged); see the other factory. */
    public static Optional<OutboxRelay> of(ModuleDatabase database, EventBus bus, JsonMapper json, Clock clock) {
        return of(database, bus, json, clock, Settings.defaults());
    }

    /** The relay of a module database, or empty (logged) when no connection may read every tenant's outbox. */
    public static Optional<OutboxRelay> of(ModuleDatabase database, EventBus bus, JsonMapper json, Clock clock,
                                           Settings settings) {
        Optional<DataSource> relay = database.relayDataSource();
        if (relay.isPresent()) {
            return Optional.of(new OutboxRelay(database.module(), relay.get(), bus, json, clock, settings));
        }
        Boolean bypasses = JdbcClient.create(database.dataSource()).sql(BYPASSES).query(Boolean.class).optional()
                .orElse(false);
        if (Boolean.TRUE.equals(bypasses)) {
            LOG.warn("module {} relays its outbox as the application role (it bypasses row-level security): local "
                    + "development only, configure a relay-username", database.module());
            return Optional.of(new OutboxRelay(database.module(), database.dataSource(), bus, json, clock, settings));
        }
        LOG.error("module {} has no relay-username and its application role is bound by row-level security: its "
                + "events stay in {}.outbox_event until a relay role is configured", database.module(),
                database.module());
        return Optional.empty();
    }

    public String module() {
        return module;
    }

    /** Publishes one batch of pending events and prunes old published events and processed inbox rows. */
    public Pass runOnce() {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        long deadline = System.nanoTime() + settings.maxBatchTime().toNanos();
        int[] taken = publishBatch(now, deadline);
        int pruned = prune(prune, now.minus(settings.outboxRetention()));
        int inboxPruned = prune(pruneInbox, now.minus(settings.inboxRetention()));
        return new Pass(taken[0], taken[1], pruned, inboxPruned);
    }

    /** Publishes pending events in one transaction until the batch or its time runs out: {published, set aside}. */
    private int[] publishBatch(OffsetDateTime now, long deadline) {
        TransactionStatus transaction = transactions.getTransaction(writeDefinition);
        try {
            List<Map<String, Object>> rows = jdbc.sql(pending).param("limit", settings.batchSize()).query()
                    .listOfRows();
            int published = 0;
            int asides = 0;
            for (Map<String, Object> row : rows) {
                if (published + asides > 0 && System.nanoTime() > deadline) {
                    break; // the rest waits for the next pass; what was confirmed commits now
                }
                EventEnvelope event;
                try {
                    event = envelope(row);
                    event.toJson(json);
                } catch (RuntimeException e) {
                    setAside(row, e.getClass().getSimpleName() + ": " + e.getMessage());
                    asides++;
                    continue;
                }
                if (!event.ownedBySource()) {
                    setAside(row, "not an event of module " + module + ": " + event.type() + " of "
                            + event.aggregateType());
                    asides++;
                    continue;
                }
                bus.publish(event);
                jdbc.sql(markPublished).param("now", now).param("id", row.get("id")).update();
                published++;
            }
            return new int[] {published, asides};
        } catch (RuntimeException | Error failure) {
            Transactions.rollback(transactions, transaction, failure);
            throw failure;
        } finally {
            Transactions.commit(transactions, transaction);
        }
    }

    /** Deletes the rows a pruning statement selects as older than {@code before}, in its own transaction. */
    private int prune(String statement, OffsetDateTime before) {
        TransactionStatus transaction = transactions.getTransaction(writeDefinition);
        try {
            return jdbc.sql(statement).param("before", before).update();
        } catch (RuntimeException | Error failure) {
            Transactions.rollback(transactions, transaction, failure);
            throw failure;
        } finally {
            Transactions.commit(transactions, transaction);
        }
    }

    private void setAside(Map<String, Object> row, String reason) {
        String kept = reason.length() > MAX_ERROR ? reason.substring(0, MAX_ERROR) : reason;
        LOG.error("module {} set outbox row {} aside, it is not published: {}", module, row.get("id"), kept);
        jdbc.sql(setAside).param("error", kept).param("id", row.get("id")).update();
    }

    private EventEnvelope envelope(Map<String, Object> row) {
        long id = ((Number) row.get("id")).longValue();
        String payload = (String) row.get("payload");
        Optional<String> trace = Optional.empty();
        try {
            JsonNode node = json.readTree(payload);
            trace = Optional.ofNullable(node.path("traceId").asString(null)).filter(t -> t.matches("[0-9a-f]{32}"));
        } catch (RuntimeException e) {
            // an opaque payload: published as it is, without a trace
        }
        Object occurred = row.get("occurred_at");
        Instant time = occurred instanceof Timestamp ts ? ts.toInstant()
                : occurred instanceof OffsetDateTime odt ? odt.toInstant() : clock.instant();
        return new EventEnvelope(EventEnvelope.idOf(module, id), module, (String) row.get("event_type"),
                Optional.ofNullable((UUID) row.get("tenant_id")), (String) row.get("aggregate_type"),
                (UUID) row.get("aggregate_id"), time, payload, trace);
    }
}
