package com.mysms.payroll.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.payroll.config.PayrollDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * payroll.staff_member: the login account of each staff record, kept from hr's staff events. A row changes only to a
 * newer source row version, so a late or repeated event changes nothing, and a deleted record stays as a tombstone a
 * late event cannot bring back.
 */
@Repository
public class StaffMemberRepository {

    private static final String APPLY = """
            INSERT INTO payroll.staff_member (tenant_id, staff_id, user_id, is_deleted, source_version, synced_at)
            VALUES (:tenantId, :staffId, :userId, :deleted, :version, :now)
            ON CONFLICT (tenant_id, staff_id) DO UPDATE
               SET user_id = EXCLUDED.user_id, is_deleted = EXCLUDED.is_deleted,
                   source_version = EXCLUDED.source_version, synced_at = EXCLUDED.synced_at
             WHERE payroll.staff_member.source_version < EXCLUDED.source_version
               AND NOT payroll.staff_member.is_deleted
            """;

    /** The live staff records of one login account. */
    private static final String STAFF_OF = """
            SELECT m.staff_id
              FROM payroll.staff_member m
             WHERE m.tenant_id = :tenantId AND NOT m.is_deleted AND m.user_id = :userId
             ORDER BY m.staff_id
            """;

    private final JdbcClient jdbc;

    public StaffMemberRepository(@Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /**
     * Applies one event of a staff record; whether the stored entry changed (false for an older or repeated version,
     * and for anything after the record's deletion).
     */
    public boolean apply(UUID staffId, UUID userId, boolean deleted, int version, OffsetDateTime now) {
        return jdbc.sql(APPLY).param("staffId", staffId).param("userId", userId).param("deleted", deleted)
                .param("version", version).param("now", now).update() == 1;
    }

    /** The live staff records of the login account. */
    public List<UUID> staffOf(UUID userId) {
        return jdbc.sql(STAFF_OF).param("userId", userId).query(UUID.class).list();
    }
}
