package com.mysms.campus.facility.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.campus.facility.config.FacilityEventsConfig;
import com.mysms.campus.facility.repository.ReferenceCleanupRepository;
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
 * Keeps facility's logical references consistent when their owners are deleted (references are not checked when
 * written). For each owner's {@code .deleted} event it clears the referencing column or soft-deletes the referencing
 * row, audited and announced on the outbox, inside the event's tenant; the inbox delivers each event exactly once.
 */
@Component
public class FacilityReferenceCleanup implements EventSubscriptions {

    /** One action: the statement, the entity it changes, and the cleared field (null = the row is archived). */
    record Step(String statement, String entity, String field) {
    }

    static final Map<String, List<Step>> STEPS = Map.ofEntries(
        Map.entry("academics.batch.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_ROOM_DEFAULT_BATCH_ID,
                    "facility.room", "defaultBatchId"))),
        Map.entry("academics.class_section.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_ROOM_DEFAULT_CLASS_SECTION_ID,
                    "facility.room", "defaultClassSectionId"))),
        Map.entry("academics.course.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_ROOM_DEFAULT_COURSE_ID,
                    "facility.room", "defaultCourseId"))));

    private final ReferenceCleanupRepository repository;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public FacilityReferenceCleanup(ReferenceCleanupRepository repository,
            @Qualifier(FacilityEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(FacilityEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        this.repository = repository;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "facility";
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
                    outbox.publish(step.entity() + FacilityEvents.DELETED, step.entity(), id, FacilityEvents.payload(id));
                } else {
                    audit.recordUpdate(step.entity(), id, List.of(step.field()));
                    outbox.publish(step.entity() + FacilityEvents.UPDATED, step.entity(), id, FacilityEvents.payload(id));
                }
                changed++;
            }
        }
        return changed;
    }
}
