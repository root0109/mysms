package com.mysms.platform.org.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.platform.org.config.OrgDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of org's reference cleanup: each changes the rows of one logical reference to a deleted owner, inside
 * the event's tenant, and returns their ids.
 */
@Repository
public class ReferenceCleanupRepository {

    /**
     * org.institution.logo_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_INSTITUTION_LOGO_FILE_ID = """
            UPDATE org.institution
               SET logo_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND logo_file_id = :ownerId
            RETURNING id
            """;

    /**
     * org.institution.signature_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_INSTITUTION_SIGNATURE_FILE_ID = """
            UPDATE org.institution
               SET signature_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND signature_file_id = :ownerId
            RETURNING id
            """;

    /**
     * org.institution.theme_id, on platform.theme deletion: null the column.
     */
    public static final String CLEAR_INSTITUTION_THEME_ID = """
            UPDATE org.institution
               SET theme_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND theme_id = :ownerId
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public ReferenceCleanupRepository(@Qualifier(OrgDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Runs one cleanup statement for the deleted owner; the ids of the rows it changed. */
    public List<UUID> apply(String statement, UUID ownerId, OffsetDateTime now) {
        return jdbc.sql(statement).param("ownerId", ownerId).param("now", now).query(UUID.class).list();
    }
}
