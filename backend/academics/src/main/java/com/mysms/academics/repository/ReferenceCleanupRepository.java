package com.mysms.academics.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.academics.config.AcademicsDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of academics's reference cleanup: each changes the rows of one logical reference to a deleted owner,
 * inside the event's tenant, and returns their ids.
 */
@Repository
public class ReferenceCleanupRepository {

    /**
     * academics.class_section.class_teacher_staff_id, on hr.staff deletion: null the column.
     */
    public static final String CLEAR_CLASS_SECTION_CLASS_TEACHER_STAFF_ID = """
            UPDATE academics.class_section
               SET class_teacher_staff_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND class_teacher_staff_id = :ownerId
            RETURNING id
            """;

    /**
     * academics.program.image_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_PROGRAM_IMAGE_FILE_ID = """
            UPDATE academics.program
               SET image_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND image_file_id = :ownerId
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public ReferenceCleanupRepository(@Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Runs one cleanup statement for the deleted owner; the ids of the rows it changed. */
    public List<UUID> apply(String statement, UUID ownerId, OffsetDateTime now) {
        return jdbc.sql(statement).param("ownerId", ownerId).param("now", now).query(UUID.class).list();
    }
}
