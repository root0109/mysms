package com.mysms.academics.assessment.repository;

import com.mysms.academics.assessment.config.AssessmentDatabaseConfig;
import com.mysms.core.db.ModuleDatabase;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * assessment.student_record: the person of each student record, kept from sis's student events. A row changes only to
 * a newer source row version, so a late or repeated event changes nothing.
 */
@Repository
public class StudentRecordRepository {

    private static final String APPLY = """
            INSERT INTO assessment.student_record (tenant_id, student_id, person_id, source_version, synced_at)
            VALUES (:tenantId, :studentId, :personId, :version, :now)
            ON CONFLICT (tenant_id, student_id) DO UPDATE
               SET person_id = EXCLUDED.person_id, source_version = EXCLUDED.source_version,
                   synced_at = EXCLUDED.synced_at
             WHERE assessment.student_record.source_version < EXCLUDED.source_version
            """;

    private final JdbcClient jdbc;

    public StudentRecordRepository(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Applies one event of a student record; whether the stored entry changed (false for an older or repeated one). */
    public boolean apply(UUID studentId, UUID personId, int version, OffsetDateTime now) {
        return jdbc.sql(APPLY).param("studentId", studentId).param("personId", personId).param("version", version)
                .param("now", now).update() == 1;
    }
}
