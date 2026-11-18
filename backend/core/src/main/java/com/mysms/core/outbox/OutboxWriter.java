package com.mysms.core.outbox;

import com.mysms.core.context.CallContextHolder;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Appends events to the module's `outbox_event` inside the caller's transaction, so they commit or roll back with the
 * business row; a relay publishes them afterwards. Payloads carry ids and codes, never personal data.
 */
public final class OutboxWriter {

    private static final Pattern SCHEMA = Pattern.compile("^[a-z]+$");
    private static final String AUDIT_EVENT_TYPE = AuditWriter.EVENT_TYPE;

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final String insert;
    private final String prefix;

    public OutboxWriter(String schema, JdbcClient jdbc, JsonMapper json) {
        if (!SCHEMA.matcher(schema).matches()) {
            throw new IllegalArgumentException("invalid schema " + schema);
        }
        this.jdbc = jdbc;
        this.json = json;
        this.prefix = schema + ".";
        this.insert = "INSERT INTO " + schema + ".outbox_event (tenant_id, event_type, aggregate_type, aggregate_id, payload)"
                + " VALUES (:tenantId, :eventType, :aggregateType, :aggregateId, :payload)";
    }

    /**
     * Records one event in the current transaction. The tenant is the caller's (absent for the global master data).
     *
     * @param eventType     e.g. `master.country.created`
     * @param aggregateType e.g. `master.country`
     * @throws IllegalArgumentException when the event or its aggregate is not the module's own
     */
    public void publish(String eventType, String aggregateType, UUID aggregateId, Map<String, ?> payload) {
        if (!(eventType.startsWith(prefix) || AUDIT_EVENT_TYPE.equals(eventType))
                || !aggregateType.startsWith(prefix)) {
            throw new IllegalArgumentException("module " + prefix + " cannot publish " + eventType + " of "
                    + aggregateType);
        }
        UUID tenant = CallContextHolder.current().flatMap(c -> c.tenantId()).orElse(null);
        jdbc.sql(insert)
                .param("tenantId", tenant)
                .param("eventType", eventType)
                .param("aggregateType", aggregateType)
                .param("aggregateId", aggregateId)
                .param("payload", json.writeValueAsString(payload))
                .update();
    }
}
