package com.mysms.assessment.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.assessment.config.AssessmentEventsConfig;
import com.mysms.assessment.repository.ReferenceCleanupRepository;
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
 * assessment keeps its logical references consistent when their owners are deleted (database/logical-references.md, "Events
 * drive cleanup"; references are not checked against the owner when written). For each owner's `.deleted` event it
 * clears the referencing column or soft-deletes the referencing row, audits each changed row and announces it on its
 * own outbox, inside the event's tenant (messaging's InboxProcessor: exactly once per event).
 */
@Component
public class AssessmentReferenceCleanup implements EventSubscriptions {

    /** One action: the statement, the entity it changes, and the cleared field (null = the row is archived). */
    record Step(String statement, String entity, String field) {
    }

    static final Map<String, List<Step>> STEPS = Map.ofEntries(
        Map.entry("facility.room.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_EXAM_RESULT_ROOM_ID,
                    "assessment.exam_result", "roomId"))),
        Map.entry("platform.file_object.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_ASSIGNMENT_ATTACHMENT_FILE_ID,
                    "assessment.assignment", "attachmentFileId"),
            new Step(ReferenceCleanupRepository.CLEAR_HOMEWORK_ATTACHMENT_FILE_ID,
                    "assessment.homework", "attachmentFileId"))));

    private final ReferenceCleanupRepository repository;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public AssessmentReferenceCleanup(ReferenceCleanupRepository repository,
            @Qualifier(AssessmentEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AssessmentEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        this.repository = repository;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "assessment";
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
                    outbox.publish(step.entity() + AssessmentEvents.DELETED, step.entity(), id, AssessmentEvents.payload(id));
                } else {
                    audit.recordUpdate(step.entity(), id, List.of(step.field()));
                    outbox.publish(step.entity() + AssessmentEvents.UPDATED, step.entity(), id, AssessmentEvents.payload(id));
                }
                changed++;
            }
        }
        return changed;
    }
}
