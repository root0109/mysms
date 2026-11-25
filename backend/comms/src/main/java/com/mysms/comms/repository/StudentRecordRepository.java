package com.mysms.comms.repository;

import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.core.db.ModuleDatabase;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * comms.student_record: the person and login account of each student record, kept from sis's student events. A row
 * changes only to a newer source row version, so a late or repeated event changes nothing.
 */
@Repository
public class StudentRecordRepository {

    private static final String APPLY = """
            INSERT INTO comms.student_record (tenant_id, student_id, person_id, user_id, source_version, synced_at)
            VALUES (:tenantId, :studentId, :personId, :userId, :version, :now)
            ON CONFLICT (tenant_id, student_id) DO UPDATE
               SET person_id = EXCLUDED.person_id, user_id = EXCLUDED.user_id,
                   source_version = EXCLUDED.source_version, synced_at = EXCLUDED.synced_at
             WHERE comms.student_record.source_version < EXCLUDED.source_version
            """;

    private final JdbcClient jdbc;

    public StudentRecordRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /**
     * Applies one event of a student record ({@code userId} null while the student has no login account); whether the
     * stored entry changed (false for an older or repeated one).
     */
    public boolean apply(UUID studentId, UUID personId, UUID userId, int version, OffsetDateTime now) {
        return jdbc.sql(APPLY).param("studentId", studentId).param("personId", personId).param("userId", userId)
                .param("version", version).param("now", now).update() == 1;
    }
}
