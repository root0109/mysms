package com.mysms.comms.business;

import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.OutboundMessageEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.repository.OutboundMessageRepository;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ApiException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Delivers queued outbound messages, one at a time: a write transaction takes the message (queued → processing), the
 * provider is called outside any transaction, and a second write transaction records the outcome (delivered with its
 * time, or failed — whatever the provider threw, so a message is never left processing). A message someone else took
 * or cancelled meanwhile is left alone. Runs after the transaction that queued the messages has committed.
 */
@Component
public class MessageDeliveryExecutor extends AppTransactionManager {

    static final String AGGREGATE = OutboundMessageEntity.TABLE;
    /** What a failed delivery records: a fixed code, never the provider's sentence (it may echo the address). */
    static final String UNDELIVERED = "UNDELIVERED";
    private static final Logger LOG = LoggerFactory.getLogger(MessageDeliveryExecutor.class);

    private final OutboundMessageRepository repository;
    private final DeliveryChannels channels;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public MessageDeliveryExecutor(OutboundMessageRepository repository, DeliveryChannels channels,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.channels = channels;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    /** Delivers each of the messages that is still queued; answers how many were delivered. */
    public int dispatch(List<UUID> ids) {
        int delivered = 0;
        for (UUID id : ids) {
            Optional<OutboundMessageEntity> taken = doInWriteTransaction(() -> take(id));
            if (taken.isEmpty()) {
                continue;
            }
            OutboundMessageEntity message = taken.get();
            boolean done = deliver(message);
            runInWriteTransaction(() -> record(message, done));
            delivered += done ? 1 : 0;
        }
        return delivered;
    }

    private Optional<OutboundMessageEntity> take(UUID id) {
        Optional<OutboundMessageEntity> found = repository.findById(id)
                .filter(message -> "queued".equals(message.getStatus()));
        if (found.isEmpty() || repository.move(id, found.get().getRowVersion(), "queued", "processing", null, null,
                now(), Actor.current().orElse(null)) == 0) {
            return Optional.empty();
        }
        found.get().setRowVersion(found.get().getRowVersion() + 1);
        return found;
    }

    private boolean deliver(OutboundMessageEntity message) {
        try {
            channels.require(message.getChannel()).deliver(message.getRecipient(), message.getSubject(),
                    message.getBody());
            return true;
        } catch (ApiException e) {
            LOG.warn("outbound message {} was not delivered ({})", message.getId(), e.code());
            return false;
        } catch (Exception e) {
            // any provider failure dead-ends the row as failed (retryable), never leaves it processing
            LOG.warn("outbound message {} was not delivered ({})", message.getId(), e.getClass().getSimpleName());
            return false;
        }
    }

    private void record(OutboundMessageEntity message, boolean delivered) {
        OffsetDateTime now = now();
        String status = delivered ? "delivered" : "failed";
        if (repository.move(message.getId(), message.getRowVersion(), "processing", status,
                delivered ? null : UNDELIVERED, delivered ? now : null, now, Actor.current().orElse(null)) == 1) {
            audit.recordTransition(AGGREGATE, message.getId(), status, List.of("status", "sentAt", "providerResponse"),
                    null);
            outbox.publish(AGGREGATE + "." + status, AGGREGATE, message.getId(), CommsEvents.payload(message.getId()));
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
