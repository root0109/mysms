package com.mysms.hr.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.hr.config.HrDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of hr's reference cleanup: each changes the rows of one logical reference to a deleted owner, inside
 * the event's tenant, and returns their ids.
 */
@Repository
public class ReferenceCleanupRepository {

    /**
     * hr.staff.primary_department_id, on org.department deletion: null the column.
     */
    public static final String CLEAR_STAFF_PRIMARY_DEPARTMENT_ID = """
            UPDATE hr.staff
               SET primary_department_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND primary_department_id = :ownerId
            RETURNING id
            """;

    /**
     * hr.staff.user_id, on iam.user_account deletion: null the column.
     */
    public static final String CLEAR_STAFF_USER_ID = """
            UPDATE hr.staff
               SET user_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public ReferenceCleanupRepository(@Qualifier(HrDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Runs one cleanup statement for the deleted owner; the ids of the rows it changed. */
    public List<UUID> apply(String statement, UUID ownerId, OffsetDateTime now) {
        return jdbc.sql(statement).param("ownerId", ownerId).param("now", now).query(UUID.class).list();
    }
}
