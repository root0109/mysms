package com.mysms.comms.events;

import com.mysms.comms.repository.StaffRecordRepository;
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
 * Keeps comms.staff_record — the login account and institution of each staff record — in step with hr.staff from its
 * created, updated and deleted events (comms never reads hr's schema), so mail and conversations reach only accounts
 * of the sender's school. An entry changes only to a newer row version, so out-of-order and repeated deliveries are
 * harmless; an event without a row version changes nothing. Runs inside the event's tenant (messaging's
 * InboxProcessor: exactly once per event).
 */
@Component
public class StaffRecordSync implements EventSubscriptions {

    static final String STAFF = "hr.staff";

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
        return "comms";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of(STAFF + ".created", this::apply, STAFF + ".updated", this::apply, STAFF + ".deleted",
                this::apply);
    }

    /** Applies one staff record event; whether the stored entry changed. */
    public boolean apply(EventEnvelope event) {
        JsonNode data = json.readTree(event.data());
        if (!STAFF.equals(event.aggregateType())
                || !data.path("id").asString("").equals(event.aggregateId().toString())) {
            throw new RejectedEventException("staff event " + event.id() + " is not about the record it names");
        }
        if (!data.has("rowVersion")) {
            return false;
        }
        int version = data.path("rowVersion").asInt(0);
        if (version < 1) {
            throw new RejectedEventException("staff event " + event.id() + " has no row version");
        }
        UUID userId;
        UUID institutionId;
        try {
            userId = StudentRecordSync.optionalUuid(data, "userId");
            institutionId = StudentRecordSync.optionalUuid(data, "institutionId");
        } catch (IllegalArgumentException malformed) {
            throw new RejectedEventException("staff event " + event.id() + " names a malformed id");
        }
        return records.apply(event.aggregateId(), userId, institutionId, event.type().endsWith(".deleted"), version,
                OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
    }
}
