package com.mysms.library.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.library.config.LibraryDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of library's reference cleanup: each changes the rows of one logical reference to a deleted owner,
 * inside the event's tenant, and returns their ids.
 */
@Repository
public class ReferenceCleanupRepository {

    /**
     * library.media.cover_image_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_MEDIA_COVER_IMAGE_FILE_ID = """
            UPDATE library.media
               SET cover_image_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND cover_image_file_id = :ownerId
            RETURNING id
            """;

    /**
     * library.media_course.course_id, on academics.course deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_MEDIA_COURSE_COURSE_ID = """
            UPDATE library.media_course
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND course_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * library.media_subject.subject_id, on academics.subject deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_MEDIA_SUBJECT_SUBJECT_ID = """
            UPDATE library.media_subject
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND subject_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public ReferenceCleanupRepository(@Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Runs one cleanup statement for the deleted owner; the ids of the rows it changed. */
    public List<UUID> apply(String statement, UUID ownerId, OffsetDateTime now) {
        return jdbc.sql(statement).param("ownerId", ownerId).param("now", now).query(UUID.class).list();
    }
}
