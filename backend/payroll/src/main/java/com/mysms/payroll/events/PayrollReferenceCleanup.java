package com.mysms.payroll.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.payroll.config.PayrollEventsConfig;
import com.mysms.payroll.repository.ReferenceCleanupRepository;
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
 * payroll keeps its logical references consistent when their owners are deleted (database/logical-references.md, "Events
 * drive cleanup"; references are not checked against the owner when written). For each owner's `.deleted` event it
 * clears the referencing column or soft-deletes the referencing row, audits each changed row and announces it on its
 * own outbox, inside the event's tenant (messaging's InboxProcessor: exactly once per event).
 */
@Component
public class PayrollReferenceCleanup implements EventSubscriptions {

    /** One action: the statement, the entity it changes, and the cleared field (null = the row is archived). */
    record Step(String statement, String entity, String field) {
    }

    static final Map<String, List<Step>> STEPS = Map.ofEntries(
        Map.entry("hr.staff.deleted", List.of(
            new Step(ReferenceCleanupRepository.ARCHIVE_STAFF_BANK_ACCOUNT_STAFF_ID,
                    "payroll.staff_bank_account", null))),
        Map.entry("platform.file_object.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_PAYROLL_PAYMENT_ATTACHMENT_FILE_ID,
                    "payroll.payroll_payment", "attachmentFileId"),
            new Step(ReferenceCleanupRepository.CLEAR_PAYSLIP_DOCUMENT_FILE_ID,
                    "payroll.payslip", "documentFileId"))));

    private final ReferenceCleanupRepository repository;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public PayrollReferenceCleanup(ReferenceCleanupRepository repository,
            @Qualifier(PayrollEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PayrollEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        this.repository = repository;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "payroll";
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
                    outbox.publish(step.entity() + PayrollEvents.DELETED, step.entity(), id, PayrollEvents.payload(id));
                } else {
                    audit.recordUpdate(step.entity(), id, List.of(step.field()));
                    outbox.publish(step.entity() + PayrollEvents.UPDATED, step.entity(), id, PayrollEvents.payload(id));
                }
                changed++;
            }
        }
        return changed;
    }
}
