package com.mysms.academics.attendance.events;

import com.mysms.academics.attendance.repository.MemberRecordRepository;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps attendance.member_record — the person of each staff and student record — in step with hr.staff and
 * sis.student from their created and updated events (attendance never reads another module's schema), so privacy's
 * tasks and the attendance.after_leaving sweep find a data principal's attendance ({@link AttendancePrivacyTaskHandler}).
 * An entry changes only to a newer row version, so out-of-order and repeated deliveries are harmless; a deleted record
 * keeps its entry, since its attendance stays the person's. Runs inside the event's tenant (messaging's
 * InboxProcessor: exactly once per event).
 */
@Component
public class MemberRecordSync implements EventSubscriptions {

    private static final Map<String, String> KINDS = Map.of("hr.staff", "staff", "sis.student", "student");

    private final MemberRecordRepository records;
    private final JsonMapper json;
    private final Clock clock;

    public MemberRecordSync(MemberRecordRepository records, JsonMapper json, Clock clock) {
        this.records = records;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "attendance";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of("hr.staff.created", this::apply, "hr.staff.updated", this::apply,
                "sis.student.created", this::apply, "sis.student.updated", this::apply);
    }

    /** Applies one record event; whether the stored entry changed. */
    public boolean apply(EventEnvelope event) {
        String kind = KINDS.get(event.aggregateType());
        JsonNode data = json.readTree(event.data());
        if (kind == null || !data.path("id").asString("").equals(event.aggregateId().toString())) {
            throw new RejectedEventException("member event " + event.id() + " is not about the record it names");
        }
        if (!data.has("rowVersion")) {
            return false; // a reference cleanup's announcement of the id alone: nothing to keep
        }
        int version = data.path("rowVersion").asInt(0);
        if (version < 1) {
            throw new RejectedEventException("member event " + event.id() + " has no row version");
        }
        UUID personId;
        try {
            personId = UUID.fromString(data.path("personId").asString(""));
        } catch (IllegalArgumentException malformed) {
            throw new RejectedEventException("member event " + event.id() + " names no person");
        }
        return records.apply(kind, event.aggregateId(), personId, version,
                OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
    }
}
