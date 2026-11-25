package com.mysms.payroll.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.payroll.repository.StaffMemberRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps payroll.staff_member (the login account and person of each hr.staff record) in step with hr's events, so a
 * staff member who is not a payroll officer reads only their own payslips and privacy's tasks find a person's records
 * (PayrollPrivacyTaskHandler); payroll never reads hr's schema. An entry changes only to
 * a newer row version and never after the deletion, so out-of-order and repeated deliveries are harmless. The deletion
 * arrives through {@link PayrollReferenceCleanup}, the module's one handler of {@code hr.staff.deleted}.
 */
@Component
public class StaffMemberSync implements EventSubscriptions {

    static final String STAFF = "hr.staff";
    static final String DELETED = STAFF + ".deleted";

    private final StaffMemberRepository members;
    private final JsonMapper json;
    private final Clock clock;

    public StaffMemberSync(StaffMemberRepository members, JsonMapper json, Clock clock) {
        this.members = members;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "payroll";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of(STAFF + ".created", this::apply, STAFF + ".updated", this::apply);
    }

    /** Applies one staff record event; whether the stored entry changed. */
    public boolean apply(EventEnvelope event) {
        JsonNode data = json.readTree(event.data());
        if (!data.has("rowVersion")) {
            return false; // an announcement that names no version (a reference cleanup's): nothing to keep
        }
        if (!STAFF.equals(event.aggregateType())
                || !data.path("id").asString("").equals(event.aggregateId().toString())) {
            throw new RejectedEventException("staff event " + event.id() + " is not about the record it names");
        }
        int version = data.path("rowVersion").asInt(0);
        if (version < 1) {
            throw new RejectedEventException("staff event " + event.id() + " has no row version");
        }
        UUID userId;
        UUID personId;
        try {
            userId = uuid(data, "userId");
            personId = uuid(data, "personId");
        } catch (IllegalArgumentException malformed) {
            throw new RejectedEventException("staff event " + event.id() + " carries a malformed id");
        }
        return members.apply(event.aggregateId(), userId, personId, event.type().equals(DELETED), version,
                OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
    }

    private static UUID uuid(JsonNode data, String field) {
        JsonNode value = data.path(field);
        return value.isString() && !value.asString().isEmpty() ? UUID.fromString(value.asString()) : null;
    }
}
