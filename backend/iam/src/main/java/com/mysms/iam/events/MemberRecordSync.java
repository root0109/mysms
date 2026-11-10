package com.mysms.iam.events;

import com.mysms.iam.repository.MemberRecordRepository;
import com.mysms.iam.repository.MemberRecordRepository.Kind;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps iam.member_record — the staff and student records of each login account and person, which iam.GetMe answers
 * as {@code staffIds} and {@code studentIds} — in step with hr.staff and sis.student (against the DAG, events only:
 * iam never reads another module's schema). Their created, updated and deleted events carry the record's account,
 * person, institution and row version; an entry changes only to a newer version, so out-of-order and repeated
 * deliveries are harmless. Runs inside the event's tenant (messaging's InboxProcessor: exactly once per event).
 *
 * <p>An event that does not name the record's owner — the reference cleanup of a module announces only the id of a row
 * it changed — changes nothing: the cleanups that touch these records clear a deleted account or department, which no
 * caller of iam.GetMe can be.
 */
@Component
public class MemberRecordSync implements EventSubscriptions {

    private static final Map<String, Kind> KINDS = Map.of("hr.staff", Kind.STAFF, "sis.student", Kind.STUDENT);

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
        return "iam";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        Map<String, EventHandler> handlers = new HashMap<>();
        for (String aggregate : KINDS.keySet()) {
            for (String verb : new String[] {".created", ".updated", ".deleted"}) {
                handlers.put(aggregate + verb, this::apply);
            }
        }
        return Map.copyOf(handlers);
    }

    /** Applies one record event; whether the stored entry changed. */
    public boolean apply(EventEnvelope event) {
        Kind kind = KINDS.get(event.aggregateType());
        JsonNode data = json.readTree(event.data());
        if (kind == null || !data.path("id").asString("").equals(event.aggregateId().toString())) {
            throw new RejectedEventException("member event " + event.id() + " is not about the record it names");
        }
        if (!data.has("rowVersion")) {
            return false; // a cleanup's announcement of the id alone (see the class comment)
        }
        int version = data.path("rowVersion").asInt(0);
        if (version < 1) {
            throw new RejectedEventException("member event " + event.id() + " has no row version");
        }
        try {
            return records.apply(kind, event.aggregateId(), uuid(data, "userId"), uuid(data, "personId"),
                    uuid(data, "institutionId"), event.type().endsWith(".deleted"), version,
                    OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
        } catch (IllegalArgumentException malformed) {
            throw new RejectedEventException("member event " + event.id() + " carries a malformed id");
        }
    }

    private static UUID uuid(JsonNode data, String field) {
        JsonNode value = data.path(field);
        return value.isString() && !value.asString().isEmpty() ? UUID.fromString(value.asString()) : null;
    }
}
