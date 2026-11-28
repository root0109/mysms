package com.mysms.hrms.timeattendance.events;

import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.hrms.timeattendance.config.TimeAttendanceEventsConfig;
import com.mysms.hrms.timeattendance.repository.ReferenceCleanupRepository;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
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
 * Keeps time_attendance's references consistent when their owners are deleted. hr.leave_application rows are only
 * ever soft-deleted, so the in-module foreign key never fires; for hr's {@code .deleted} event this clears the
 * referencing column, audited and announced on the outbox, inside the event's tenant; the inbox delivers each event
 * exactly once.
 */
@Component
public class TimeAttendanceReferenceCleanup implements EventSubscriptions {

    /** One action: the statement, the entity it changes, and the cleared field (null = the row is archived). */
    record Step(String statement, String entity, String field) {
    }

    static final Map<String, List<Step>> STEPS = Map.ofEntries(
        Map.entry("hr.leave_application.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_STAFF_ATTENDANCE_LEAVE_APPLICATION_ID,
                    "time_attendance.staff_attendance", "leaveApplicationId"))));

    private final ReferenceCleanupRepository repository;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public TimeAttendanceReferenceCleanup(ReferenceCleanupRepository repository,
            @Qualifier(TimeAttendanceEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(TimeAttendanceEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        this.repository = repository;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "time_attendance";
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
                audit.recordUpdate(step.entity(), id, List.of(step.field()));
                outbox.publish(step.entity() + TimeAttendanceEvents.UPDATED, step.entity(), id,
                        TimeAttendanceEvents.payload(id));
                changed++;
            }
        }
        return changed;
    }
}
