package com.mysms.finance.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.finance.config.FinanceEventsConfig;
import com.mysms.finance.repository.ReferenceCleanupRepository;
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
 * finance keeps its logical references consistent when their owners are deleted (database/logical-references.md, "Events
 * drive cleanup"; references are not checked against the owner when written). For each owner's `.deleted` event it
 * clears the referencing column or soft-deletes the referencing row, audits each changed row and announces it on its
 * own outbox, inside the event's tenant (messaging's InboxProcessor: exactly once per event).
 */
@Component
public class FinanceReferenceCleanup implements EventSubscriptions {

    /** One action: the statement, the entity it changes, and the cleared field (null = the row is archived). */
    record Step(String statement, String entity, String field) {
    }

    static final Map<String, List<Step>> STEPS = Map.ofEntries(
        Map.entry("academics.course.deleted", List.of(
            new Step(ReferenceCleanupRepository.ARCHIVE_COURSE_FEE_DEFAULT_COURSE_ID,
                    "finance.course_fee_default", null))),
        Map.entry("lms.cohort.deleted", List.of(
            new Step(ReferenceCleanupRepository.ARCHIVE_COUPON_SCOPE_LMS_COHORT_ID,
                    "finance.coupon_scope", null))),
        Map.entry("lms.course.deleted", List.of(
            new Step(ReferenceCleanupRepository.ARCHIVE_COUPON_SCOPE_LMS_COURSE_ID,
                    "finance.coupon_scope", null))),
        Map.entry("people.person_address.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_PAYMENT_BILLING_ADDRESS_ID,
                    "finance.payment", "billingAddressId"))),
        Map.entry("platform.file_object.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_INVOICE_DOCUMENT_FILE_ID,
                    "finance.invoice", "documentFileId"))),
        Map.entry("sis.program_enrollment.deleted", List.of(
            new Step(ReferenceCleanupRepository.ARCHIVE_PROGRAM_ENROLLMENT_FEE_PROGRAM_ENROLLMENT_ID,
                    "finance.program_enrollment_fee", null),
            new Step(ReferenceCleanupRepository.CLEAR_STUDENT_FEE_DUE_PROGRAM_ENROLLMENT_ID,
                    "finance.student_fee_due", "programEnrollmentId"))),
        Map.entry("sis.student_enrollment.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_STUDENT_FEE_DUE_STUDENT_ENROLLMENT_ID,
                    "finance.student_fee_due", "studentEnrollmentId"))));

    private final ReferenceCleanupRepository repository;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public FinanceReferenceCleanup(ReferenceCleanupRepository repository,
            @Qualifier(FinanceEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(FinanceEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        this.repository = repository;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "finance";
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
                    outbox.publish(step.entity() + FinanceEvents.DELETED, step.entity(), id, FinanceEvents.payload(id));
                } else {
                    audit.recordUpdate(step.entity(), id, List.of(step.field()));
                    outbox.publish(step.entity() + FinanceEvents.UPDATED, step.entity(), id, FinanceEvents.payload(id));
                }
                changed++;
            }
        }
        return changed;
    }
}
