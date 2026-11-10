package com.mysms.platform.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.platform.config.PlatformDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of platform's reference cleanup (database/logical-references.md): each changes the rows of one logical
 * reference to a deleted owner, inside the event's tenant, and returns their ids.
 */
@Repository
public class ReferenceCleanupRepository {

    /**
     * platform.upload_policy.role_id, on iam.role deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_UPLOAD_POLICY_ROLE_ID = """
            UPDATE platform.upload_policy
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND role_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * platform.deletion_log.cohort_id, on lms.cohort deletion: null the column.
     */
    public static final String CLEAR_DELETION_LOG_COHORT_ID = """
            UPDATE platform.deletion_log
               SET cohort_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND cohort_id = :ownerId
            RETURNING id
            """;

    /**
     * platform.storage_quota.user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_STORAGE_QUOTA_USER_ID = """
            UPDATE platform.storage_quota
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * platform.storage_quota.institution_id, on org.institution deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_STORAGE_QUOTA_INSTITUTION_ID = """
            UPDATE platform.storage_quota
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND institution_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * platform.file_object.uploaded_by_user_id, on iam.user_account deletion: null the column.
     */
    public static final String CLEAR_FILE_OBJECT_UPLOADED_BY_USER_ID = """
            UPDATE platform.file_object
               SET uploaded_by_user_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND uploaded_by_user_id = :ownerId
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public ReferenceCleanupRepository(@Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Runs one cleanup statement for the deleted owner; the ids of the rows it changed. */
    public List<UUID> apply(String statement, UUID ownerId, OffsetDateTime now) {
        return jdbc.sql(statement).param("ownerId", ownerId).param("now", now).query(UUID.class).list();
    }
}
