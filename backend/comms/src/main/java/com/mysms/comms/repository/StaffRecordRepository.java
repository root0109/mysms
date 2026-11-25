package com.mysms.comms.repository;

import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.core.db.ModuleDatabase;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * comms.staff_record: the login account and institution of each staff record, kept from hr's staff events. A row
 * changes only to a newer source row version, so a late or repeated event changes nothing.
 */
@Repository
public class StaffRecordRepository {

    private static final String APPLY = """
            INSERT INTO comms.staff_record (tenant_id, staff_id, user_id, institution_id, is_deleted, source_version,
                                            synced_at)
            VALUES (:tenantId, :staffId, :userId, :institutionId, :deleted, :version, :now)
            ON CONFLICT (tenant_id, staff_id) DO UPDATE
               SET user_id = EXCLUDED.user_id, institution_id = EXCLUDED.institution_id,
                   is_deleted = EXCLUDED.is_deleted, source_version = EXCLUDED.source_version,
                   synced_at = EXCLUDED.synced_at
             WHERE comms.staff_record.source_version < EXCLUDED.source_version
            """;

    private final JdbcClient jdbc;

    public StaffRecordRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Applies one event of a staff record; whether the stored entry changed (false for an older or repeated one). */
    public boolean apply(UUID staffId, UUID userId, UUID institutionId, boolean deleted, int version,
            OffsetDateTime now) {
        return jdbc.sql(APPLY).param("staffId", staffId).param("userId", userId).param("institutionId", institutionId)
                .param("deleted", deleted).param("version", version).param("now", now).update() == 1;
    }
}
