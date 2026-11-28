package com.mysms.academics.events;

import com.mysms.academics.repository.StaffRecordRepository;
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
 * Keeps academics.staff_record — the person of each staff record — in step with hr.staff from its created, updated and
 * deleted events (academics never reads another module's schema), so privacy's tasks find a data principal's lesson
 * plans ({@link AcademicsPrivacyTaskHandler}) and teaching assignments name only live staff. An entry changes only to a
 * newer row version, so out-of-order and repeated deliveries are harmless; a deleted staff record keeps its entry,
 * flagged, since its lesson plans stay the person's. The deleted event reaches {@link #apply} through
 * {@link AcademicsReferenceCleanup}, academics's one subscriber of that type. Runs inside the event's tenant
 * (messaging's InboxProcessor: exactly once per event).
 */
@Component
public class StaffRecordSync implements EventSubscriptions {

    private final StaffRecordRepository records;
    private final JsonMapper json;
    private final Clock clock;

    public StaffRecordSync(StaffRecordRepository records, JsonMapper json, Clock clock) {
        this.records = records;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "academics";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of("hr.staff.created", this::apply, "hr.staff.updated", this::apply);
    }

    /** Applies one staff event; whether the stored entry changed. */
    public boolean apply(EventEnvelope event) {
        JsonNode data = json.readTree(event.data());
        if (!data.path("id").asString("").equals(event.aggregateId().toString())) {
            throw new RejectedEventException("staff event " + event.id() + " is not about the record it names");
        }
        if (!data.has("rowVersion")) {
            return false; // a reference cleanup's announcement of the id alone: nothing to keep
        }
        int version = data.path("rowVersion").asInt(0);
        if (version < 1) {
            throw new RejectedEventException("staff event " + event.id() + " has no row version");
        }
        UUID personId;
        try {
            personId = UUID.fromString(data.path("personId").asString(""));
        } catch (IllegalArgumentException malformed) {
            throw new RejectedEventException("staff event " + event.id() + " names no person");
        }
        return records.apply(event.aggregateId(), personId, event.type().endsWith(".deleted"), version,
                OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
    }
}
