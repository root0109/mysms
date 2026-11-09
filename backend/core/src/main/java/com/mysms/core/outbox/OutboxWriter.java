package com.mysms.core.outbox;

import com.mysms.core.context.CallContextHolder;
import com.mysms.core.events.EventEnvelope;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Appends events to the module's own `outbox_event` table, inside the caller's transaction (JAVA-CODING-RULES §4):
 * the event commits or rolls back with the business row, and a relay publishes it afterwards. Payloads carry ids and
 * codes, never personal data.
 */
public final class OutboxWriter {

    private static final Pattern SCHEMA = Pattern.compile("^[a-z]+$");

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
     * @throws IllegalArgumentException when the event or its aggregate is not the module's own: consumers refuse an
     *                                  event its source does not own ({@code EventEnvelope.ownedBySource})
     */
    public void publish(String eventType, String aggregateType, UUID aggregateId, Map<String, ?> payload) {
        if (!(eventType.startsWith(prefix) || EventEnvelope.AUDIT_TYPE.equals(eventType))
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
