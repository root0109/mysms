package com.mysms.library.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.library.config.LibraryEventsConfig;
import com.mysms.library.repository.ReferenceCleanupRepository;
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
 * library keeps its logical references consistent when their owners are deleted (database/logical-references.md, "Events
 * drive cleanup"; references are not checked against the owner when written). For each owner's `.deleted` event it
 * clears the referencing column or soft-deletes the referencing row, audits each changed row and announces it on its
 * own outbox, inside the event's tenant (messaging's InboxProcessor: exactly once per event).
 */
@Component
public class LibraryReferenceCleanup implements EventSubscriptions {

    /** One action: the statement, the entity it changes, and the cleared field (null = the row is archived). */
    record Step(String statement, String entity, String field) {
    }

    static final Map<String, List<Step>> STEPS = Map.ofEntries(
        Map.entry("academics.course.deleted", List.of(
            new Step(ReferenceCleanupRepository.ARCHIVE_MEDIA_COURSE_COURSE_ID,
                    "library.media_course", null))),
        Map.entry("academics.subject.deleted", List.of(
            new Step(ReferenceCleanupRepository.ARCHIVE_MEDIA_SUBJECT_SUBJECT_ID,
                    "library.media_subject", null))),
        Map.entry("platform.file_object.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_MEDIA_COVER_IMAGE_FILE_ID,
                    "library.media", "coverImageFileId"))));

    private final ReferenceCleanupRepository repository;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public LibraryReferenceCleanup(ReferenceCleanupRepository repository,
            @Qualifier(LibraryEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LibraryEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        this.repository = repository;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "library";
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
                    outbox.publish(step.entity() + LibraryEvents.DELETED, step.entity(), id, LibraryEvents.payload(id));
                } else {
                    audit.recordUpdate(step.entity(), id, List.of(step.field()));
                    outbox.publish(step.entity() + LibraryEvents.UPDATED, step.entity(), id, LibraryEvents.payload(id));
                }
                changed++;
            }
        }
        return changed;
    }
}
