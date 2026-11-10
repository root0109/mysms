package com.mysms.assessment.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of assessment's reference cleanup (database/logical-references.md): each changes the rows of one logical
 * reference to a deleted owner, inside the event's tenant, and returns their ids.
 */
@Repository
public class ReferenceCleanupRepository {

    /**
     * assessment.assignment.attachment_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_ASSIGNMENT_ATTACHMENT_FILE_ID = """
            UPDATE assessment.assignment
               SET attachment_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND attachment_file_id = :ownerId
            RETURNING id
            """;

    /**
     * assessment.exam_result.room_id, on facility.room deletion: null the column.
     */
    public static final String CLEAR_EXAM_RESULT_ROOM_ID = """
            UPDATE assessment.exam_result
               SET room_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND room_id = :ownerId
            RETURNING id
            """;

    /**
     * assessment.homework.attachment_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_HOMEWORK_ATTACHMENT_FILE_ID = """
            UPDATE assessment.homework
               SET attachment_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND attachment_file_id = :ownerId
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public ReferenceCleanupRepository(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Runs one cleanup statement for the deleted owner; the ids of the rows it changed. */
    public List<UUID> apply(String statement, UUID ownerId, OffsetDateTime now) {
        return jdbc.sql(statement).param("ownerId", ownerId).param("now", now).query(UUID.class).list();
    }
}
