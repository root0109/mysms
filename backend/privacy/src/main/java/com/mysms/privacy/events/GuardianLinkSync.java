package com.mysms.privacy.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.privacy.repository.GuardianLinkRepository;
import com.mysms.privacy.repository.GuardianLinkRepository.Link;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps privacy.guardian_link — who is whose guardian, and whether as a legal guardian with custody — in step with
 * sis.student_guardian from its events, which carry the persons the link joins and the guardian's login account (sis
 * resolves them when it publishes; privacy never reads another schema). A link changes only to a newer row version,
 * so out-of-order and repeated deliveries are harmless; a deleted link stays, inactive, so a late event cannot bring
 * it back. Runs inside the event's tenant (messaging's InboxProcessor: exactly once per event).
 *
 * <p>An event without the link's state (an id-only announcement) changes nothing, as in {@link SubjectSync}.
 */
@Component
public class GuardianLinkSync implements EventSubscriptions {

    static final String LINK = "sis.student_guardian";
    private static final String ACTIVE = "active";
    private static final String INACTIVE = "inactive";

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
        return "privacy";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of(LINK + ".created", this::apply, LINK + ".updated", this::apply, LINK + ".deleted", this::apply);
    }

    /** Applies one guardian link event; whether the stored link changed. */
    public boolean apply(EventEnvelope event) {
        if (!LINK.equals(event.aggregateType())) {
            throw new RejectedEventException("event " + event.id() + " is not a guardian link's");
        }
        SourceEvent link = SourceEvent.read(event, json);
        if (!link.carriesState()) {
            return false; // an announcement of the id alone (see the class comment)
        }
        int version = link.rowVersion();
        String status = link.requiredText("status", Set.of(ACTIVE, INACTIVE));
        return links.apply(new Link(link.id(), link.requiredId("studentPersonId"), link.requiredId("guardianPersonId"),
                link.optionalId("guardianUserId"), link.requiredFlag("isLegalGuardian"), link.requiredFlag("hasCustody"),
                link.deleted() ? INACTIVE : status, version),
                OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
    }
}
