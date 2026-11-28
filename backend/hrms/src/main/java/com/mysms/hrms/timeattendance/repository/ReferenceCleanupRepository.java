package com.mysms.hrms.timeattendance.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.hrms.timeattendance.config.TimeAttendanceDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of time_attendance's reference cleanup: each changes the rows of one reference to a deleted owner,
 * inside the event's tenant, and returns their ids. The leave link is an in-module foreign key, but its owner is
 * only ever soft-deleted, so the link is still released by hr's {@code .deleted} event, not by the constraint.
 */
@Repository
public class ReferenceCleanupRepository {

    /**
     * time_attendance.staff_attendance.leave_application_id, on hr.leave_application deletion: null the column.
     */
    public static final String CLEAR_STAFF_ATTENDANCE_LEAVE_APPLICATION_ID = """
            UPDATE time_attendance.staff_attendance
               SET leave_application_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND leave_application_id = :ownerId
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public ReferenceCleanupRepository(@Qualifier(TimeAttendanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Runs one cleanup statement for the deleted owner; the ids of the rows it changed. */
    public List<UUID> apply(String statement, UUID ownerId, OffsetDateTime now) {
        return jdbc.sql(statement).param("ownerId", ownerId).param("now", now).query(UUID.class).list();
    }
}
