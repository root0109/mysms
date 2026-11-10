package com.mysms.messaging.events;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.tx.Transactions;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

/**
 * Exactly-once handling of delivered events in one consuming module (database/README "Events"): inside the event's
 * tenant transaction the event id is recorded in the module's `inbox_event`, the handler runs, and the row is marked
 * processed — a redelivery of a processed event is dropped. A failing handler rolls back its own work, the failure is
 * noted on the inbox row (class and message only), and the exception goes back to the bus for a later redelivery.
 *
 * <p>The inbox is keyed by tenant and event id, so an event recorded for one tenant never stands in for another
 * tenant's. Processed rows are pruned by the module's relay after the inbox retention. Events of global master data
 * carry no tenant and are not recorded here; their handlers must be idempotent.
 */
public final class InboxProcessor {

    /** What a delivery came to. */
    public enum Outcome { PROCESSED, DUPLICATE }

    private static final Logger LOG = LoggerFactory.getLogger(InboxProcessor.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ERROR = 500;

    private static final String RECORD = """
            INSERT INTO {schema}.inbox_event (event_id, tenant_id, event_type, received_at)
            VALUES (:eventId, :tenantId, :eventType, :now)
            ON CONFLICT (tenant_id, event_id) DO NOTHING
            """;
    private static final String PROCESSED_AT = """
            SELECT processed_at IS NOT NULL
              FROM {schema}.inbox_event
             WHERE tenant_id = :tenantId AND event_id = :eventId
               FOR UPDATE
            """;
    private static final String MARK = """
            UPDATE {schema}.inbox_event SET processed_at = :now, processing_error = NULL
             WHERE tenant_id = :tenantId AND event_id = :eventId
            """;
    private static final String FAILED = """
            INSERT INTO {schema}.inbox_event (event_id, tenant_id, event_type, received_at, processing_error)
            VALUES (:eventId, :tenantId, :eventType, :now, :error)
            ON CONFLICT (tenant_id, event_id) DO UPDATE SET processing_error = EXCLUDED.processing_error
            """;

    private final ModuleDatabase database;
    private final PlatformTransactionManager transactions;
    private final Clock clock;
    private final String record;
    private final String processedAt;
    private final String mark;
    private final String failed;

    public InboxProcessor(ModuleDatabase database, Clock clock) {
        this.database = database;
        this.transactions = database.transactionManager();
        this.clock = clock;
        String schema = database.module();
        this.record = RECORD.replace("{schema}", schema);
        this.processedAt = PROCESSED_AT.replace("{schema}", schema);
        this.mark = MARK.replace("{schema}", schema);
        this.failed = FAILED.replace("{schema}", schema);
    }

    public String module() {
        return database.module();
    }

    /** Handles one delivery; throws (after noting the failure) when the handler fails. */
    public Outcome deliver(EventEnvelope event, EventHandler handler) {
        CallContext context = new CallContext(event.tenantId(), Optional.empty(),
                event.traceId().orElseGet(InboxProcessor::newTraceId));
        Optional<CallContext> previous = CallContextHolder.current();
        CallContextHolder.set(context);
        try {
            if (event.tenantId().isEmpty()) {
                handler.handle(event);
                return Outcome.PROCESSED;
            }
            try {
                TransactionStatus transaction = transactions.getTransaction(database.writeDefinition());
                try {
                    database.bindTenant();
                    OffsetDateTime now = now();
                    database.jdbc().sql(record).param("eventId", event.id()).param("eventType", event.type())
                            .param("now", now).update();
                    boolean done = database.jdbc().sql(processedAt).param("eventId", event.id())
                            .query(Boolean.class).single();
                    if (done) {
                        return Outcome.DUPLICATE;
                    }
                    handler.handle(event);
                    database.jdbc().sql(mark).param("eventId", event.id()).param("now", now()).update();
                    return Outcome.PROCESSED;
                } catch (RuntimeException | Error failure) {
                    Transactions.rollback(transactions, transaction, failure);
                    throw failure;
                } finally {
                    Transactions.commit(transactions, transaction);
                }
            } catch (RuntimeException e) {
                noteFailure(event, e);
                throw e;
            }
        } finally {
            previous.ifPresentOrElse(CallContextHolder::set, CallContextHolder::clear);
        }
    }

    private void noteFailure(EventEnvelope event, RuntimeException failure) {
        String error = failure.getClass().getName() + (failure.getMessage() == null ? "" : ": " + failure.getMessage());
        String kept = error.length() > MAX_ERROR ? error.substring(0, MAX_ERROR) : error;
        LOG.warn("module {} could not handle event {} ({}): {}", database.module(), event.id(), event.type(), kept);
        try {
            TransactionStatus transaction = transactions.getTransaction(database.writeDefinition());
            try {
                database.bindTenant();
                database.jdbc().sql(failed).param("eventId", event.id()).param("eventType", event.type())
                        .param("now", now()).param("error", kept).update();
            } catch (RuntimeException | Error noting) {
                Transactions.rollback(transactions, transaction, noting);
                throw noting;
            } finally {
                Transactions.commit(transactions, transaction);
            }
        } catch (RuntimeException e) {
            LOG.warn("module {} could not note the failure of event {}", database.module(), event.id(), e);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }

    private static String newTraceId() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
