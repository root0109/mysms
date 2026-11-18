package com.mysms.sis.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.sis.config.SisEventsConfig;
import com.mysms.sis.repository.ReferenceCleanupRepository;
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
 * sis keeps its logical references consistent when their owners are deleted (references are not checked against the
 * owner when written). For each owner's `.deleted` event it clears the referencing column or soft-deletes the
 * referencing row, audits each changed row and announces it on its own outbox, inside the event's tenant (exactly once
 * per event).
 */
@Component
public class SisReferenceCleanup implements EventSubscriptions {

    /** One action: the statement, the entity it changes, and the cleared field (null = the row is archived). */
    record Step(String statement, String entity, String field) {
    }

    static final Map<String, List<Step>> STEPS = Map.ofEntries(
        Map.entry("assessment.exam_session.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_PROMOTION_BASED_ON_EXAM_SESSION_ID,
                    "sis.promotion", "basedOnExamSessionId"))),
        Map.entry("iam.user_account.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_GUARDIAN_USER_ID,
                    "sis.guardian", "userId"),
            new Step(ReferenceCleanupRepository.CLEAR_STUDENT_USER_ID,
                    "sis.student", "userId"))),
        Map.entry("org.academic_term.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_SUBJECT_REGISTRATION_ACADEMIC_TERM_ID,
                    "sis.subject_registration", "academicTermId"))),
        Map.entry("platform.file_object.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_DISCIPLINE_INCIDENT_EVIDENCE_FILE_ID,
                    "sis.discipline_incident", "evidenceFileId"))));

    private final ReferenceCleanupRepository repository;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public SisReferenceCleanup(ReferenceCleanupRepository repository,
            @Qualifier(SisEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(SisEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        this.repository = repository;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "sis";
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
                    outbox.publish(step.entity() + SisEvents.DELETED, step.entity(), id, SisEvents.payload(id));
                } else {
                    audit.recordUpdate(step.entity(), id, List.of(step.field()));
                    outbox.publish(step.entity() + SisEvents.UPDATED, step.entity(), id, SisEvents.payload(id));
                }
                changed++;
            }
        }
        return changed;
    }
}
