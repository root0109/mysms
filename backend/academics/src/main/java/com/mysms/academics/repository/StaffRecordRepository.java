package com.mysms.academics.repository;

import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.core.db.ModuleDatabase;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * academics.staff_record: the person of each staff record, kept from hr's events. A row changes only to a newer source
 * row version, so a late or repeated event changes nothing.
 */
@Repository
public class StaffRecordRepository {

    private static final String APPLY = """
            INSERT INTO academics.staff_record (tenant_id, staff_id, person_id, source_version, synced_at)
            VALUES (:tenantId, :staffId, :personId, :version, :now)
            ON CONFLICT (tenant_id, staff_id) DO UPDATE
               SET person_id = EXCLUDED.person_id, source_version = EXCLUDED.source_version,
                   synced_at = EXCLUDED.synced_at
             WHERE academics.staff_record.source_version < EXCLUDED.source_version
            """;

    private final JdbcClient jdbc;

    public StaffRecordRepository(@Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Applies one event of a staff record; whether the stored entry changed (false for an older or repeated one). */
    public boolean apply(UUID staffId, UUID personId, int version, OffsetDateTime now) {
        return jdbc.sql(APPLY).param("staffId", staffId).param("personId", personId).param("version", version)
                .param("now", now).update() == 1;
    }
}
