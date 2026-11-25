package com.mysms.comms.events;

import com.mysms.comms.repository.GuardianLinkRepository;
import com.mysms.comms.repository.GuardianLinkRepository.Link;
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
 * Keeps comms.guardian_link — which guardian record, person and account is linked to which student — in step with
 * sis.student_guardian from its created, updated and deleted events (comms never reads sis's schema), so a guardian
 * reaches only the feedback threads opened as them. An entry changes only to a newer row version, so out-of-order and
 * repeated deliveries are harmless; an event without a row version changes nothing, and one without the student or
 * guardian record is rejected. Runs inside the event's tenant (messaging's InboxProcessor: exactly once per event).
 */
@Component
public class GuardianLinkSync implements EventSubscriptions {

    static final String LINK = "sis.student_guardian";

    private final GuardianLinkRepository links;
    private final JsonMapper json;
    private final Clock clock;

    public GuardianLinkSync(GuardianLinkRepository links, JsonMapper json, Clock clock) {
        this.links = links;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "comms";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of(LINK + ".created", this::apply, LINK + ".updated", this::apply, LINK + ".deleted", this::apply);
    }

    /** Applies one guardian link event; whether the stored entry changed. */
    public boolean apply(EventEnvelope event) {
        JsonNode data = json.readTree(event.data());
        if (!LINK.equals(event.aggregateType())
                || !data.path("id").asString("").equals(event.aggregateId().toString())) {
            throw new RejectedEventException("guardian link event " + event.id() + " is not about the link it names");
        }
        if (!data.has("rowVersion")) {
            return false;
        }
        int version = data.path("rowVersion").asInt(0);
        if (version < 1) {
            throw new RejectedEventException("guardian link event " + event.id() + " has no row version");
        }
        Link link;
        try {
            link = new Link(event.aggregateId(), UUID.fromString(data.path("studentId").asString("")),
                    UUID.fromString(data.path("guardianId").asString("")),
                    UUID.fromString(data.path("guardianPersonId").asString("")),
                    StudentRecordSync.optionalUuid(data, "guardianUserId"), data.path("status").asString("active"),
                    event.type().endsWith(".deleted"), version);
        } catch (IllegalArgumentException malformed) {
            throw new RejectedEventException("guardian link event " + event.id() + " names no student or guardian");
        }
        return links.apply(link, OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
    }
}
