package com.mysms.academics.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.academics.config.AcademicsEventsConfig;
import com.mysms.academics.repository.ReferenceCleanupRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * academics keeps its logical references consistent when their owners are deleted (references are not checked against
 * the owner when written). For each owner's `.deleted` event it clears the referencing column or soft-deletes the
 * referencing row, audits each changed row and announces it on its own outbox, inside the event's tenant (exactly once
 * per event).
 */
@Component
public class AcademicsReferenceCleanup implements EventSubscriptions {

    /** One action: the statement, the entity it changes, and the cleared field (null = the row is archived). */
    record Step(String statement, String entity, String field) {
    }

    static final String STAFF_DELETED = "hr.staff.deleted";

    static final Map<String, List<Step>> STEPS = Map.ofEntries(
        Map.entry(STAFF_DELETED, List.of(
            new Step(ReferenceCleanupRepository.CLEAR_CLASS_SECTION_CLASS_TEACHER_STAFF_ID,
                    "academics.class_section", "classTeacherStaffId"),
            new Step(ReferenceCleanupRepository.ARCHIVE_TEACHER_SUBJECT_STAFF_ID,
                    "academics.teacher_subject", null))),
        Map.entry("platform.file_object.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_PROGRAM_IMAGE_FILE_ID,
                    "academics.program", "imageFileId"))));

    private final ReferenceCleanupRepository repository;
    private final StaffRecordSync staffRecords;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public AcademicsReferenceCleanup(ReferenceCleanupRepository repository, StaffRecordSync staffRecords,
            @Qualifier(AcademicsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AcademicsEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        this.repository = repository;
        this.staffRecords = staffRecords;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "academics";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        Map<String, EventHandler> handlers = new HashMap<>(STEPS.keySet().stream()
                .collect(Collectors.toMap(type -> type, type -> (EventHandler) this::handle)));
        // a module subscribes once per event type: the staff copy learns of the deletion in the same transaction
        handlers.put(STAFF_DELETED, event -> {
            staffRecords.apply(event);
            handle(event);
        });
        return handlers;
    }

    /** Applies every step of the deleted owner; returns how many rows changed. */
    public int handle(EventEnvelope event) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        int changed = 0;
        for (Step step : STEPS.getOrDefault(event.type(), List.of())) {
            for (UUID id : repository.apply(step.statement(), event.aggregateId(), now)) {
                if (step.field() == null) {
                    audit.recordDelete(step.entity(), id);
                    outbox.publish(step.entity() + AcademicsEvents.DELETED, step.entity(), id, AcademicsEvents.payload(id));
                } else {
                    audit.recordUpdate(step.entity(), id, List.of(step.field()));
                    outbox.publish(step.entity() + AcademicsEvents.UPDATED, step.entity(), id, AcademicsEvents.payload(id));
                }
                changed++;
            }
        }
        return changed;
    }
}
