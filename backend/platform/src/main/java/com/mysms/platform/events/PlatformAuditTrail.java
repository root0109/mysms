package com.mysms.platform.events;

import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.repository.AuditTrailRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Materialises every module's audit records into platform.audit_log (each change writes an `audit.recorded` event in
 * its own transaction; the platform keeps the trail). Records carry ids, the action, the changed field names, the
 * reason given and the trace — never values. A module audits only its own entities: a record naming another module's
 * entity is rejected (parked by the bus). Audit of global data (no tenant) is stored without a tenant: invisible to
 * every tenant, read by platform staff tooling.
 */
@Component
public class PlatformAuditTrail extends AppTransactionManager implements EventSubscriptions {

    private final AuditTrailRepository repository;
    private final JsonMapper json;

    public PlatformAuditTrail(AuditTrailRepository repository, JsonMapper json,
                              @Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
                              @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions) {

        super(database, transactions);
        this.repository = repository;
        this.json = json;
    }

    @Override
    public String module() {
        return "platform";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of(AuditWriter.EVENT_TYPE, this::record);
    }

    /**
     * Stores one audit record: in its tenant's trail, or — for global data, changed by platform staff — as a record
     * without a tenant that no tenant session can see (its own tenantless transaction: such events bypass the inbox).
     */
    public void record(EventEnvelope event) {
        JsonNode data = json.readTree(event.data());
        String entityType = data.path("entityType").asString(event.aggregateType());
        if (!entityType.startsWith(event.module() + ".")) {
            throw new RejectedEventException("module " + event.module() + " cannot audit " + entityType);
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("changedFields", data.path("changedFields"));
        if (data.hasNonNull("reason")) {
            properties.put("reason", data.path("reason").asString());
        }
        properties.put("traceId", data.path("traceId").asString(null));
        if (data.hasNonNull("impersonatorUserId")) {
            // the administrator who acted as the causer (iam.StartImpersonation): kept with the record
            properties.put("impersonatorUserId", UUID.fromString(data.path("impersonatorUserId").asString()).toString());
        }
        String actor = data.path("actorUserId").asString(null);
        String occurred = data.path("occurredAt").asString(null);
        UUID causer = actor == null ? null : UUID.fromString(actor);
        String props = json.writeValueAsString(properties);
        OffsetDateTime at = (occurred == null ? event.time() : Instant.parse(occurred)).atOffset(ZoneOffset.UTC);
        String action = data.path("action").asString("change");
        if (event.tenantId().isEmpty()) {
            // a global record needs a transaction that binds no tenant: the global_insert policy admits only that
            runInGlobalWriteTransaction(() -> {
                repository.insertGlobal(event.id(), event.module(), action, entityType, event.aggregateId(), causer,
                        props, at);
            });
            return;
        }
        repository.insert(event.id(), event.module(), action, entityType, event.aggregateId(), causer, props, at);
    }
}
