package com.mysms.people.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.people.config.PeopleEventsConfig;
import com.mysms.people.repository.ReferenceCleanupRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * people keeps its logical references consistent when their owners are deleted (references are not checked against the
 * owner when written). For each owner's `.deleted` event it clears the referencing column or soft-deletes the
 * referencing row, audits each changed row and announces it on its own outbox, inside the event's tenant (exactly once
 * per event).
 */
@Component
public class PeopleReferenceCleanup implements EventSubscriptions {

    /** One action: the statement, the entity it changes, and the cleared field (null = the row is archived). */
    record Step(String statement, String entity, String field) {
    }

    static final Map<String, List<Step>> STEPS = Map.ofEntries(
        Map.entry("platform.file_object.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_PERSON_PHOTO_FILE_ID,
                    "people.person", "photoFileId"),
            new Step(ReferenceCleanupRepository.CLEAR_PUBLIC_PROFILE_COVER_IMAGE_FILE_ID,
                    "people.public_profile", "coverImageFileId"))));

    private final ReferenceCleanupRepository repository;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public PeopleReferenceCleanup(ReferenceCleanupRepository repository,
            @Qualifier(PeopleEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PeopleEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        this.repository = repository;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "people";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return STEPS.keySet().stream().collect(Collectors.toMap(type -> type, type -> (EventHandler) this::handle));
    }

    /** Applies every step of the deleted owner; returns how many rows changed. */
    public int handle(EventEnvelope event) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        int changed = 0;
        for (Step step : STEPS.getOrDefault(event.type(), List.of())) {
            for (UUID id : repository.apply(step.statement(), event.aggregateId(), now)) {
                if (step.field() == null) {
                    audit.recordDelete(step.entity(), id);
                    outbox.publish(step.entity() + PeopleEvents.DELETED, step.entity(), id, PeopleEvents.payload(id));
                } else {
                    audit.recordUpdate(step.entity(), id, List.of(step.field()));
                    outbox.publish(step.entity() + PeopleEvents.UPDATED, step.entity(), id, PeopleEvents.payload(id));
                }
                changed++;
            }
        }
        return changed;
    }
}
