package com.mysms.people.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.people.repository.MemberLinkRepository;
import com.mysms.people.repository.MemberLinkRepository.Link;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps people.member_link in step with sis.student, hr.staff and sis.student_guardian from their events (people may
 * not call sis or hr: they depend on people). The created, updated and deleted events of those rows carry the person,
 * the institution and the row version (a guardian link the child and its status instead of an institution; a staff
 * record its joining date); an entry changes only to a newer version, so out-of-order and repeated deliveries
 * are harmless. Runs inside the event's tenant (messaging's InboxProcessor: exactly once per event).
 *
 * <p>An event without a row version — a reference cleanup announcing only the id of a row it changed — changes
 * nothing.
 */
@Component
public class MemberLinkSync implements EventSubscriptions {

    private static final Map<String, String> TYPES = Map.of("sis.student", "student", "hr.staff", "staff",
            "sis.student_guardian", "guardian");
    private static final Set<String> VERBS = Set.of(".created", ".updated", ".deleted");

    private final MemberLinkRepository links;
    private final JsonMapper json;
    private final Clock clock;

    public MemberLinkSync(MemberLinkRepository links, JsonMapper json, Clock clock) {
        this.links = links;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "people";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        Map<String, EventHandler> handlers = new HashMap<>();
        for (String aggregate : TYPES.keySet()) {
            for (String verb : VERBS) {
                handlers.put(aggregate + verb, this::apply);
            }
        }
        return Map.copyOf(handlers);
    }

    /** Applies one record event; whether the stored entry changed. */
    public boolean apply(EventEnvelope event) {
        String type = TYPES.get(event.aggregateType());
        JsonNode data = json.readTree(event.data());
        if (type == null || !data.path("id").asString("").equals(event.aggregateId().toString())) {
            throw new RejectedEventException("member event " + event.id() + " is not about the record it names");
        }
        if (!data.has("rowVersion")) {
            return false;
        }
        int version = data.path("rowVersion").asInt(0);
        if (version < 1) {
            throw new RejectedEventException("member event " + event.id() + " has no row version");
        }
        try {
            boolean guardian = "guardian".equals(type);
            Link link = new Link(type, uuid(data, guardian ? "guardianPersonId" : "personId"),
                    guardian ? null : uuid(data, "institutionId"), guardian ? uuid(data, "studentPersonId") : null,
                    !guardian || "active".equals(data.path("status").asString("")), date(data, "joiningDate"));
            return links.apply(event.aggregateId(), link, event.type().endsWith(".deleted"), version,
                    OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
        } catch (IllegalArgumentException | DateTimeParseException malformed) {
            throw new RejectedEventException("member event " + event.id() + " carries a malformed value");
        }
    }

    private static UUID uuid(JsonNode data, String field) {
        JsonNode value = data.path(field);
        return value.isString() && !value.asString().isEmpty() ? UUID.fromString(value.asString()) : null;
    }

    private static LocalDate date(JsonNode data, String field) {
        JsonNode value = data.path(field);
        return value.isString() && !value.asString().isEmpty() ? LocalDate.parse(value.asString()) : null;
    }
}
