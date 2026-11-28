package com.mysms.academics.attendance.repository;

import com.mysms.academics.attendance.config.AttendanceDatabaseConfig;
import com.mysms.core.db.ModuleDatabase;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * attendance.member_record: the person of each student record, kept from sis's events. A row changes only to a
 * newer source row version, so a late or repeated event changes nothing.
 */
@Repository
public class MemberRecordRepository {

    private static final String APPLY = """
            INSERT INTO attendance.member_record (tenant_id, record_id, record_type, person_id, source_version,
                                                  synced_at)
            VALUES (:tenantId, :recordId, :recordType, :personId, :version, :now)
            ON CONFLICT (tenant_id, record_id) DO UPDATE
               SET person_id = EXCLUDED.person_id, source_version = EXCLUDED.source_version,
                   synced_at = EXCLUDED.synced_at
             WHERE attendance.member_record.source_version < EXCLUDED.source_version
               AND attendance.member_record.record_type = EXCLUDED.record_type
            """;

    private final JdbcClient jdbc;

    public MemberRecordRepository(@Qualifier(AttendanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Applies one event of a record; whether the stored entry changed (false for an older or repeated version). */
    public boolean apply(String recordType, UUID recordId, UUID personId, int version, OffsetDateTime now) {
        return jdbc.sql(APPLY).param("recordId", recordId).param("recordType", recordType)
                .param("personId", personId).param("version", version).param("now", now).update() == 1;
    }
}
