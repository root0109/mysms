package com.mysms.academics.timetable.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.academics.timetable.repository.InstitutionZoneRepository;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps timetable.institution_zone — each school's own time zone — in step with org.institution from its created,
 * updated and deleted events (timetable never calls org); a zone Java does not know counts as none. An entry changes
 * only to a newer row version, so out-of-order and repeated deliveries are harmless; an event without a row version
 * changes nothing. Runs inside the event's tenant (messaging's InboxProcessor: exactly once per event).
 */
@Component
public class InstitutionZoneSync implements EventSubscriptions {

    static final String INSTITUTION = "org.institution";

    private final InstitutionZoneRepository zones;
    private final JsonMapper json;
    private final Clock clock;

    public InstitutionZoneSync(InstitutionZoneRepository zones, JsonMapper json, Clock clock) {
        this.zones = zones;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "timetable";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of(INSTITUTION + ".created", this::apply, INSTITUTION + ".updated", this::apply,
                INSTITUTION + ".deleted", this::apply);
    }

    /** Applies one institution event; whether the stored entry changed. */
    public boolean apply(EventEnvelope event) {
        JsonNode data = json.readTree(event.data());
        if (!INSTITUTION.equals(event.aggregateType())
                || !data.path("id").asString("").equals(event.aggregateId().toString())) {
            throw new RejectedEventException("institution event " + event.id() + " is not about the row it names");
        }
        if (!data.has("rowVersion")) {
            return false;
        }
        int version = data.path("rowVersion").asInt(0);
        if (version < 1) {
            throw new RejectedEventException("institution event " + event.id() + " has no row version");
        }
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        return zones.apply(event.aggregateId(), knownZone(data.path("timezone").asString("")),
                event.type().endsWith(".deleted"), version, now);
    }

    /** The zone as an IANA id; none when it is absent or not a zone Java knows (the request then names it). */
    private static String knownZone(String given) {
        String zone = given.trim();
        if (zone.isEmpty()) {
            return null;
        }
        try {
            return ZoneId.of(zone).getId();
        } catch (DateTimeException unknown) {
            return null;
        }
    }
}
