package com.mysms.sis.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.sis.config.SisDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of sis's reference cleanup (database/logical-references.md): each changes the rows of one logical
 * reference to a deleted owner, inside the event's tenant, and returns their ids.
 */
@Repository
public class ReferenceCleanupRepository {

    /**
     * sis.discipline_incident.evidence_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_DISCIPLINE_INCIDENT_EVIDENCE_FILE_ID = """
            UPDATE sis.discipline_incident
               SET evidence_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND evidence_file_id = :ownerId
            RETURNING id
            """;

    /**
     * sis.guardian.user_id, on iam.user_account deletion: null the column.
     */
    public static final String CLEAR_GUARDIAN_USER_ID = """
            UPDATE sis.guardian
               SET user_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
            RETURNING id
            """;

    /**
     * sis.student.user_id, on iam.user_account deletion: null the column.
     */
    public static final String CLEAR_STUDENT_USER_ID = """
            UPDATE sis.student
               SET user_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
            RETURNING id
            """;

    /**
     * sis.subject_registration.academic_term_id, on org.academic_term deletion: null the column.
     */
    public static final String CLEAR_SUBJECT_REGISTRATION_ACADEMIC_TERM_ID = """
            UPDATE sis.subject_registration
               SET academic_term_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND academic_term_id = :ownerId
            RETURNING id
            """;

    /**
     * sis.promotion.based_on_exam_session_id, on assessment.exam_session deletion: null the column.
     */
    public static final String CLEAR_PROMOTION_BASED_ON_EXAM_SESSION_ID = """
            UPDATE sis.promotion
               SET based_on_exam_session_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND based_on_exam_session_id = :ownerId
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public ReferenceCleanupRepository(@Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Runs one cleanup statement for the deleted owner; the ids of the rows it changed. */
    public List<UUID> apply(String statement, UUID ownerId, OffsetDateTime now) {
        return jdbc.sql(statement).param("ownerId", ownerId).param("now", now).query(UUID.class).list();
    }
}
