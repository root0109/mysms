package com.mysms.hrms.timeattendance.events;

import com.mysms.core.context.Actor;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.hrms.timeattendance.config.TimeAttendanceEventsConfig;
import com.mysms.hrms.timeattendance.repository.LeaveAttendanceRepository;
import com.mysms.hrms.timeattendance.repository.LeaveAttendanceRepository.Application;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Writes staff attendance for approved leave (BE-6): on hr's approve_leave and cancel_leave events the day rows of the
 * application are brought in line with the application as hr now holds it, so the two events may arrive in any order
 * or twice. Approved leave has a row on each of its working days (hrms's working-day rule): {@code on_leave} for a full
 * day, {@code half_day} for a half day, naming the application, with the remark "Leave: <leave type>". A day already
 * recorded is overridden — leave wins — and keeps the status it had ({@code replaced_status}); a day another leave
 * already explains is left to that leave. Leave no longer approved (cancelled before it started) takes away the rows
 * written for it that nobody has changed since (a changed one stays as it is, without the link), gives overridden days
 * their status back, and then records again any other approved leave of the staff member that shares a day with it, so
 * such a day gets that leave's row. Each changed row is audited and announced; runs inside the event's tenant
 * (messaging's InboxProcessor: exactly once per event). The two areas are one module, so the leave is read from hr's
 * table directly.
 */
@Component
public class LeaveAttendanceSync implements EventSubscriptions {

    static final String LEAVE_APPLICATION = "hr.leave_application";
    static final String STAFF_ATTENDANCE = "time_attendance.staff_attendance";
    private static final Set<String> APPROVED = Set.of("approved", "completed");
    private static final List<String> LEAVE_FIELDS = List.of("status", "leaveApplicationId");

    private final LeaveAttendanceRepository repository;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public LeaveAttendanceSync(LeaveAttendanceRepository repository,
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
        return Map.of(LEAVE_APPLICATION + ".approve_leave", this::reconcile, LEAVE_APPLICATION + ".cancel_leave",
                this::reconcile);
    }

    /** Brings the attendance of the event's application in line with it; how many rows changed. */
    public int reconcile(EventEnvelope event) {
        UUID applicationId = event.aggregateId();
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        UUID actor = Actor.current().orElse(null);
        Optional<Application> application = repository.lockApplication(applicationId);
        if (application.isPresent() && APPROVED.contains(application.get().status())) {
            return record(applicationId, application.get(), now, actor);
        }
        int changed = 0;
        for (UUID id : repository.removeWritten(applicationId, now, actor)) {
            audit.recordDelete(STAFF_ATTENDANCE, id);
            outbox.publish(STAFF_ATTENDANCE + TimeAttendanceEvents.DELETED, STAFF_ATTENDANCE, id,
                    TimeAttendanceEvents.payload(id));
            changed++;
        }
        changed += updated(repository.releaseEdited(applicationId, now, actor));
        changed += updated(repository.restoreOverridden(applicationId, now, actor));
        for (UUID other : repository.overlappingApproved(applicationId)) {
            Optional<Application> approved = repository.lockApplication(other);
            if (approved.isPresent() && APPROVED.contains(approved.get().status())) {
                changed += record(other, approved.get(), now, actor);
            }
        }
        return changed;
    }

    /** Gives each working day of approved leave its row: overrides recorded days, writes the rest. */
    private int record(UUID applicationId, Application application, OffsetDateTime now, UUID actor) {
        String status = "full_day".equals(application.dayPart()) ? "on_leave" : "half_day";
        String remark = application.leaveType().map(type -> "Leave: " + type).orElse("Leave");
        List<LocalDate> days = repository.workingDays(applicationId);
        if (days.isEmpty()) {
            return 0;
        }
        int changed = updated(repository.overrideDays(applicationId, application.staffId(), status, days, now,
                actor));
        List<UUID> ids = days.stream().map(day -> UuidV7.next(clock)).toList();
        for (UUID id : repository.insertDays(applicationId, application.staffId(), status, remark, ids, days, now,
                actor)) {
            audit.recordCreate(STAFF_ATTENDANCE, id);
            outbox.publish(STAFF_ATTENDANCE + TimeAttendanceEvents.CREATED, STAFF_ATTENDANCE, id,
                    TimeAttendanceEvents.payload(id));
            changed++;
        }
        return changed;
    }

    /** Audits and announces rows the leave changed; how many. */
    private int updated(List<UUID> ids) {
        for (UUID id : ids) {
            audit.recordUpdate(STAFF_ATTENDANCE, id, LEAVE_FIELDS);
            outbox.publish(STAFF_ATTENDANCE + TimeAttendanceEvents.UPDATED, STAFF_ATTENDANCE, id,
                    TimeAttendanceEvents.payload(id));
        }
        return ids.size();
    }
}
