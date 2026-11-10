package com.mysms.attendance.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.attendance.config.AttendanceDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of attendance's reference cleanup (database/logical-references.md): each changes the rows of one logical
 * reference to a deleted owner, inside the event's tenant, and returns their ids.
 */
@Repository
public class ReferenceCleanupRepository {

    /**
     * attendance.attendance_record.leave_request_id, on sis.student_leave_request deletion: null the column.
     */
    public static final String CLEAR_ATTENDANCE_RECORD_LEAVE_REQUEST_ID = """
            UPDATE attendance.attendance_record
               SET leave_request_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND leave_request_id = :ownerId
            RETURNING id
            """;

    /**
     * attendance.attendance_sheet.timetable_session_id, on timetable.session deletion: null the column.
     */
    public static final String CLEAR_ATTENDANCE_SHEET_TIMETABLE_SESSION_ID = """
            UPDATE attendance.attendance_sheet
               SET timetable_session_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND timetable_session_id = :ownerId
            RETURNING id
            """;

    /**
     * attendance.staff_attendance.leave_application_id, on hr.leave_application deletion: null the column.
     */
    public static final String CLEAR_STAFF_ATTENDANCE_LEAVE_APPLICATION_ID = """
            UPDATE attendance.staff_attendance
               SET leave_application_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND leave_application_id = :ownerId
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public ReferenceCleanupRepository(@Qualifier(AttendanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Runs one cleanup statement for the deleted owner; the ids of the rows it changed. */
    public List<UUID> apply(String statement, UUID ownerId, OffsetDateTime now) {
        return jdbc.sql(statement).param("ownerId", ownerId).param("now", now).query(UUID.class).list();
    }
}
