package com.mysms.finance.events;

import com.mysms.finance.repository.StudentRecordRepository;
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
 * Keeps finance.student_record — the person of each student record — in step with sis.student from its created and
 * updated events (finance never reads sis's schema), so privacy's tasks find a data principal's fee dues, concessions,
 * invoices and payments ({@link FinancePrivacyTaskHandler}). An entry changes only to a newer row version, so
 * out-of-order and repeated deliveries are harmless; a deleted student keeps its entry, since its books of account stay
 * the person's. Runs inside the event's tenant (messaging's InboxProcessor: exactly once per event).
 */
@Component
public class StudentRecordSync implements EventSubscriptions {

    static final String STUDENT = "sis.student";

    private final StudentRecordRepository records;
    private final JsonMapper json;
    private final Clock clock;

    public StudentRecordSync(StudentRecordRepository records, JsonMapper json, Clock clock) {
        this.records = records;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "finance";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of(STUDENT + ".created", this::apply, STUDENT + ".updated", this::apply);
    }

    /** Applies one student record event; whether the stored entry changed. */
    public boolean apply(EventEnvelope event) {
        JsonNode data = json.readTree(event.data());
        if (!STUDENT.equals(event.aggregateType())
                || !data.path("id").asString("").equals(event.aggregateId().toString())) {
            throw new RejectedEventException("student event " + event.id() + " is not about the record it names");
        }
        if (!data.has("rowVersion")) {
            return false; // a reference cleanup's announcement of the id alone: nothing to keep
        }
        int version = data.path("rowVersion").asInt(0);
        if (version < 1) {
            throw new RejectedEventException("student event " + event.id() + " has no row version");
        }
        UUID personId;
        try {
            personId = UUID.fromString(data.path("personId").asString(""));
        } catch (IllegalArgumentException malformed) {
            throw new RejectedEventException("student event " + event.id() + " names no person");
        }
        return records.apply(event.aggregateId(), personId, version,
                OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
    }
}
