package com.mysms.core.outbox;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Writes the audit record of a change as an `audit.recorded` event into the module's own outbox, in the same
 * transaction as the change (JAVA-CODING-RULES §4: audit is never optional and never annotation-driven). The platform
 * module materialises these events into `platform.audit_log`. Records hold ids and field names only — no values.
 */
public final class AuditWriter {

    public static final String EVENT_TYPE = "audit.recorded";

    private final OutboxWriter outbox;
    private final Clock clock;

    public AuditWriter(OutboxWriter outbox, Clock clock) {
        this.outbox = outbox;
        this.clock = clock;
    }

    public void recordCreate(String entityType, UUID entityId) {
        record("create", entityType, entityId, List.of());
    }

    public void recordUpdate(String entityType, UUID entityId, List<String> changedFields) {
        record("update", entityType, entityId, changedFields);
    }

    public void recordDelete(String entityType, UUID entityId) {
        record("delete", entityType, entityId, List.of());
    }

    /**
     * A workflow step (submit, approve, cancel...): the action is the step, and the reason the caller gave for it, if
     * any, is kept with the record — it is the caller's own account of the decision, not a column value.
     */
    public void recordTransition(String entityType, UUID entityId, String step, List<String> changedFields,
                                 String reason) {
        record(step, entityType, entityId, changedFields, reason);
    }

    private void record(String action, String entityType, UUID entityId, List<String> changedFields) {
        record(action, entityType, entityId, changedFields, null);
    }

    private void record(String action, String entityType, UUID entityId, List<String> changedFields, String reason) {
        CallContext context = CallContextHolder.require();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("entityType", entityType);
        payload.put("entityId", entityId.toString());
        payload.put("actorUserId", context.userId().map(UUID::toString).orElse(null));
        payload.put("changedFields", List.copyOf(changedFields));
        if (reason != null) {
            payload.put("reason", reason);
        }
        payload.put("traceId", context.traceId());
        payload.put("occurredAt", clock.instant().toString());
        outbox.publish(EVENT_TYPE, entityType, entityId, payload);
    }
}
