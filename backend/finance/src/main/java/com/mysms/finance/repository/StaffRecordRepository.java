package com.mysms.finance.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.finance.config.FinanceDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * finance.staff_record: the login account and institution of each staff record, kept from hr's staff events. A row
 * changes only to a newer source row version, so a late or repeated event changes nothing.
 */
@Repository
public class StaffRecordRepository {

    private static final String APPLY = """
            INSERT INTO finance.staff_record (tenant_id, staff_id, user_id, institution_id, is_deleted, source_version,
                                              synced_at)
            VALUES (:tenantId, :staffId, :userId, :institutionId, :deleted, :version, :now)
            ON CONFLICT (tenant_id, staff_id) DO UPDATE
               SET user_id = EXCLUDED.user_id, institution_id = EXCLUDED.institution_id,
                   is_deleted = EXCLUDED.is_deleted, source_version = EXCLUDED.source_version,
                   synced_at = EXCLUDED.synced_at
             WHERE finance.staff_record.source_version < EXCLUDED.source_version
            """;
    private static final String ACCOUNT_IN = """
            SELECT EXISTS (SELECT 1 FROM finance.staff_record r
                            WHERE r.tenant_id = :tenantId AND r.user_id = :userId
                              AND r.institution_id = :institutionId AND NOT r.is_deleted)
            """;

    private final JdbcClient jdbc;

    public StaffRecordRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Applies one event of a staff record; whether the stored entry changed (false for an older or repeated one). */
    public boolean apply(UUID staffId, UUID userId, UUID institutionId, boolean deleted, int version,
            OffsetDateTime now) {
        return jdbc.sql(APPLY).param("staffId", staffId).param("userId", userId).param("institutionId", institutionId)
                .param("deleted", deleted).param("version", version).param("now", now).update() == 1;
    }

    /** Whether the account is the login of one of the institution's live staff records. */
    public boolean accountIn(UUID userId, UUID institutionId) {
        return Boolean.TRUE.equals(jdbc.sql(ACCOUNT_IN).param("userId", userId)
                .param("institutionId", institutionId).query(Boolean.class).single());
    }
}
