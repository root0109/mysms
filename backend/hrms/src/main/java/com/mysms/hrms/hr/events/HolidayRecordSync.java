package com.mysms.hrms.hr.events;

import com.mysms.hrms.hr.repository.HolidayRecordRepository;
import com.mysms.hrms.hr.repository.HolidayRecordRepository.HolidayRecord;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps hr.holiday_record — the holiday calendar leave and pay count working days against — in step with org.holiday
 * from its created, updated and deleted events (hrms never reads org's schema). An entry changes only to a newer row
 * version, so out-of-order and repeated deliveries are harmless; an event without a row version (published before
 * holiday events carried the row) changes nothing. Runs inside the event's tenant (messaging's InboxProcessor: exactly
 * once per event). When holidays move to master.holiday (ROADMAP ARC-8) only the event type this listens to changes.
 */
@Component
public class HolidayRecordSync implements EventSubscriptions {

    static final String HOLIDAY = "org.holiday";

    private final HolidayRecordRepository records;
    private final JsonMapper json;
    private final Clock clock;

    public HolidayRecordSync(HolidayRecordRepository records, JsonMapper json, Clock clock) {
        this.records = records;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "hr";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of(HOLIDAY + ".created", this::apply, HOLIDAY + ".updated", this::apply, HOLIDAY + ".deleted",
                this::apply);
    }

    /** Applies one holiday event; whether the stored entry changed. */
    public boolean apply(EventEnvelope event) {
        JsonNode data = json.readTree(event.data());
        if (!HOLIDAY.equals(event.aggregateType())
                || !data.path("id").asString("").equals(event.aggregateId().toString())) {
            throw new RejectedEventException("holiday event " + event.id() + " is not about the row it names");
        }
        if (!data.has("rowVersion")) {
            return false;
        }
        int version = data.path("rowVersion").asInt(0);
        String title = data.path("title").asString("");
        if (version < 1 || title.isEmpty()) {
            throw new RejectedEventException("holiday event " + event.id() + " has no row version or title");
        }
        HolidayRecord record;
        try {
            JsonNode institution = data.path("institutionId");
            record = new HolidayRecord(event.aggregateId(),
                    institution.isString() ? UUID.fromString(institution.asString()) : null, title,
                    LocalDate.parse(data.path("startDate").asString("")),
                    LocalDate.parse(data.path("endDate").asString("")));
        } catch (IllegalArgumentException | DateTimeParseException malformed) {
            throw new RejectedEventException("holiday event " + event.id() + " names a malformed id or date");
        }
        if (record.endDate().isBefore(record.startDate())) {
            throw new RejectedEventException("holiday event " + event.id() + " ends before it starts");
        }
        return records.apply(record, event.type().endsWith(".deleted"), version,
                OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
    }
}
