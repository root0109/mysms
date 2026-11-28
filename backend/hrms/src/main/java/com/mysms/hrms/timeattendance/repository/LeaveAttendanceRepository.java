package com.mysms.hrms.timeattendance.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.hrms.hr.repository.WorkingDays;
import com.mysms.hrms.timeattendance.config.TimeAttendanceDatabaseConfig;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements that keep staff attendance in step with approved leave (LeaveAttendanceSync): the leave application
 * as hr now holds it (hr.leave_application, the sibling area's table), its working days ({@link WorkingDays}), and the
 * day rows of time_attendance.staff_attendance written for it, overridden by it or released when it is cancelled.
 * Every statement names the tenant; the changed rows' ids are returned for their audit records and events.
 */
@Repository
public class LeaveAttendanceRepository {

    /**
     * The application, locked until the transaction ends: hr locks it the same way for each decision, so the
     * attendance of one application is reconciled once at a time and after the decision that announced it.
     */
    private static final String LOCK_APPLICATION = """
            SELECT a.staff_id, a.status, a.day_part,
                   (SELECT t.name FROM hr.leave_type t
                     WHERE t.tenant_id = :tenantId AND t.id = a.leave_type_id) AS leave_type
              FROM hr.leave_application a
             WHERE a.tenant_id = :tenantId AND a.id = :applicationId AND a.deleted_at IS NULL
               FOR UPDATE
            """;

    /**
     * The other approved leave of the same staff member that shares a day with the application: after the application
     * is released, each is recorded again so a day it also covers gets its row back.
     */
    private static final String OVERLAPPING_APPROVED = """
            SELECT o.id
              FROM hr.leave_application a
              JOIN hr.leave_application o
                ON o.tenant_id = :tenantId AND o.staff_id = a.staff_id AND o.id <> a.id AND o.deleted_at IS NULL
               AND o.status IN ('approved', 'completed')
               AND (o.from_at AT TIME ZONE 'UTC')::date <= (a.to_at AT TIME ZONE 'UTC')::date
               AND (o.to_at AT TIME ZONE 'UTC')::date >= (a.from_at AT TIME ZONE 'UTC')::date
             WHERE a.tenant_id = :tenantId AND a.id = :applicationId
             ORDER BY o.id
            """;

    /** The working days of the application, as its leave days are counted (UTC days). */
    private static final String WORKING_DAYS = """
            SELECT wd.day
              FROM (SELECT g.day::date AS day, a.institution_id
                      FROM hr.leave_application a
                     CROSS JOIN generate_series(date_trunc('day', a.from_at AT TIME ZONE 'UTC'),
                                                date_trunc('day', a.to_at AT TIME ZONE 'UTC'),
                                                interval '1 day') AS g(day)
                     WHERE a.tenant_id = :tenantId AND a.id = :applicationId) wd
             WHERE
            """ + WorkingDays.IS_WORKING_DAY + " ORDER BY wd.day";

    /**
     * Leave wins over a day already recorded on one of its days (that no other leave explains): the row takes the
     * leave's status and link and keeps the status it had, to be restored if the leave is cancelled.
     */
    private static final String OVERRIDE_DAYS = """
            UPDATE time_attendance.staff_attendance
               SET replaced_status = status, status = :status, leave_application_id = :applicationId,
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND staff_id = :staffId AND deleted_at IS NULL
               AND leave_application_id IS NULL AND attendance_date = ANY(CAST(:days AS date[]))
            RETURNING id
            """;

    /** A row for each day of the leave that has none, with a remark naming the leave. */
    private static final String INSERT_DAYS = """
            INSERT INTO time_attendance.staff_attendance
            (id, tenant_id, staff_id, attendance_date, status, leave_application_id, remark, created_at, created_by,
             updated_at, updated_by, row_version)
            SELECT d.id, :tenantId, :staffId, d.day, :status, :applicationId, :remark, :now, :actor, :now, :actor, 1
              FROM unnest(CAST(:ids AS uuid[]), CAST(:days AS date[])) AS d(id, day)
             WHERE NOT EXISTS (SELECT 1 FROM time_attendance.staff_attendance s
                                WHERE s.tenant_id = :tenantId AND s.staff_id = :staffId
                                  AND s.attendance_date = d.day AND s.deleted_at IS NULL)
            RETURNING id
            """;

    /**
     * Released leave: the rows written for it and not changed since (still at the version the leave wrote) are
     * removed.
     */
    private static final String REMOVE_WRITTEN = """
            UPDATE time_attendance.staff_attendance
               SET deleted_at = :now, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND leave_application_id = :applicationId AND deleted_at IS NULL
               AND replaced_status IS NULL AND row_version = 1
            RETURNING id
            """;

    /**
     * Released leave: a row written for it that someone has changed since is kept as they left it, without the link
     * to the leave.
     */
    private static final String RELEASE_EDITED = """
            UPDATE time_attendance.staff_attendance
               SET leave_application_id = NULL, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND leave_application_id = :applicationId AND deleted_at IS NULL
               AND replaced_status IS NULL AND row_version > 1
            RETURNING id
            """;

    /**
     * Released leave: a day it overrode gets its own status back (unless someone has changed it since from the
     * leave's status) and no longer names the leave.
     */
    private static final String RESTORE_OVERRIDDEN = """
            UPDATE time_attendance.staff_attendance
               SET status = CASE WHEN status IN ('on_leave', 'half_day') THEN replaced_status ELSE status END,
                   replaced_status = NULL, leave_application_id = NULL,
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND leave_application_id = :applicationId AND deleted_at IS NULL
               AND replaced_status IS NOT NULL
            RETURNING id
            """;

    /** The application as the reconciliation needs it; the leave type's name, when it names one. */
    public record Application(UUID staffId, String status, String dayPart, Optional<String> leaveType) {
    }

    private final JdbcClient jdbc;
    private final WorkingDays workingDays;

    public LeaveAttendanceRepository(@Qualifier(TimeAttendanceDatabaseConfig.DATABASE) ModuleDatabase database,
            WorkingDays workingDays) {
        this.jdbc = database.jdbc();
        this.workingDays = workingDays;
    }

    /** The live application, locked; empty when it does not exist or was deleted. */
    public Optional<Application> lockApplication(UUID applicationId) {
        return jdbc.sql(LOCK_APPLICATION).param("applicationId", applicationId)
                .query((rs, row) -> new Application(rs.getObject("staff_id", UUID.class), rs.getString("status"),
                        rs.getString("day_part"), Optional.ofNullable(rs.getString("leave_type"))))
                .optional();
    }

    /** The other approved leave of the staff member that shares a day with the application, in id order. */
    public List<UUID> overlappingApproved(UUID applicationId) {
        return jdbc.sql(OVERLAPPING_APPROVED).param("applicationId", applicationId).query(UUID.class).list();
    }

    /** The working days of the application, in date order. */
    public List<LocalDate> workingDays(UUID applicationId) {
        return jdbc.sql(WORKING_DAYS).param("applicationId", applicationId)
                .param(WorkingDays.PARAMETER, workingDays.weekendDays())
                .query((rs, row) -> rs.getObject("day", LocalDate.class)).list();
    }

    /** Overrides the staff member's recorded days among {@code days}; the ids of the rows changed. */
    public List<UUID> overrideDays(UUID applicationId, UUID staffId, String status, List<LocalDate> days,
            OffsetDateTime now, UUID actor) {
        return jdbc.sql(OVERRIDE_DAYS).param("applicationId", applicationId).param("staffId", staffId)
                .param("status", status).param("days", isoDays(days)).param("now", now).param("actor", actor)
                .query(UUID.class).list();
    }

    /** Writes a row for each of {@code days} the staff member has none for, with the given ids; the ids written. */
    public List<UUID> insertDays(UUID applicationId, UUID staffId, String status, String remark, List<UUID> ids,
            List<LocalDate> days, OffsetDateTime now, UUID actor) {
        return jdbc.sql(INSERT_DAYS).param("applicationId", applicationId).param("staffId", staffId)
                .param("status", status).param("remark", remark).param("ids", ids.toArray(UUID[]::new))
                .param("days", isoDays(days)).param("now", now).param("actor", actor).query(UUID.class).list();
    }

    /** Removes the rows written for the application; their ids. */
    public List<UUID> removeWritten(UUID applicationId, OffsetDateTime now, UUID actor) {
        return jdbc.sql(REMOVE_WRITTEN).param("applicationId", applicationId).param("now", now)
                .param("actor", actor).query(UUID.class).list();
    }

    /** Keeps the rows written for the application that were changed since, without the link; their ids. */
    public List<UUID> releaseEdited(UUID applicationId, OffsetDateTime now, UUID actor) {
        return jdbc.sql(RELEASE_EDITED).param("applicationId", applicationId).param("now", now)
                .param("actor", actor).query(UUID.class).list();
    }

    /** Restores the days the application overrode; their ids. */
    public List<UUID> restoreOverridden(UUID applicationId, OffsetDateTime now, UUID actor) {
        return jdbc.sql(RESTORE_OVERRIDDEN).param("applicationId", applicationId).param("now", now)
                .param("actor", actor).query(UUID.class).list();
    }

    /** Days as the statements take them: ISO dates, cast to {@code date[]} in SQL. */
    private static String[] isoDays(List<LocalDate> days) {
        return days.stream().map(LocalDate::toString).toArray(String[]::new);
    }
}
