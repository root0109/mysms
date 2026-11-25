package com.mysms.privacy.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.privacy.business.ConsentStateWriter;
import com.mysms.privacy.repository.GuardianLinkRepository;
import com.mysms.privacy.repository.GuardianLinkRepository.Link;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps privacy.guardian_link — who is whose guardian, and whether as a legal guardian with custody — in step with
 * sis.student_guardian from its events, which carry the persons the link joins and the guardian's login account (sis
 * resolves them when it publishes; privacy never reads another schema). A link changes only to a newer row version,
 * so out-of-order and repeated deliveries are harmless; a deleted link stays, inactive, so a late event cannot bring
 * it back. A changed link has its student's consent answers recomputed ({@link ConsentStateWriter}) in the same
 * transaction, since the active legal guardians decide for a minor — and, when the link moved to another student's
 * person (a student record re-assigned in sis), the previous student's too. Runs inside the event's tenant
 * (messaging's InboxProcessor: exactly once per event).
 *
 * <p>An event without the link's state (an id-only announcement) changes nothing, as in {@link SubjectSync}.
 */
@Component
public class GuardianLinkSync implements EventSubscriptions {

    static final String LINK = "sis.student_guardian";
    private static final String ACTIVE = "active";
    private static final String INACTIVE = "inactive";

    private final GuardianLinkRepository links;
    private final ConsentStateWriter stateWriter;
    private final JsonMapper json;
    private final Clock clock;

    public GuardianLinkSync(GuardianLinkRepository links, ConsentStateWriter stateWriter, JsonMapper json,
            Clock clock) {
        this.links = links;
        this.stateWriter = stateWriter;
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
        Link stored = new Link(link.id(), link.requiredId("studentPersonId"), link.requiredId("guardianPersonId"),
                link.optionalId("guardianUserId"), link.requiredFlag("isLegalGuardian"), link.requiredFlag("hasCustody"),
                link.deleted() ? INACTIVE : status, version);
        Optional<UUID> previousStudent = links.lockStudentOf(stored.linkId());
        if (!links.apply(stored, OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC))) {
            return false;
        }
        // in person order (the database's uuid order), as ConsentStateWriter#recomputePurpose locks subjects
        Stream.concat(previousStudent.stream(), Stream.of(stored.studentPersonId())).distinct()
                .sorted(Comparator.comparing(UUID::toString)).forEach(stateWriter::recomputeSubject);
        return true;
    }
}
