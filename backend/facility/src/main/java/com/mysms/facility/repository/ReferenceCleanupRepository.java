package com.mysms.facility.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.facility.config.FacilityDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of facility's reference cleanup (database/logical-references.md): each changes the rows of one logical
 * reference to a deleted owner, inside the event's tenant, and returns their ids.
 */
@Repository
public class ReferenceCleanupRepository {

    /**
     * facility.room.default_batch_id, on academics.batch deletion: null the column.
     */
    public static final String CLEAR_ROOM_DEFAULT_BATCH_ID = """
            UPDATE facility.room
               SET default_batch_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND default_batch_id = :ownerId
            RETURNING id
            """;

    /**
     * facility.room.default_class_section_id, on academics.class_section deletion: null the column.
     */
    public static final String CLEAR_ROOM_DEFAULT_CLASS_SECTION_ID = """
            UPDATE facility.room
               SET default_class_section_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND default_class_section_id = :ownerId
            RETURNING id
            """;

    /**
     * facility.room.default_course_id, on academics.course deletion: null the column.
     */
    public static final String CLEAR_ROOM_DEFAULT_COURSE_ID = """
            UPDATE facility.room
               SET default_course_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND default_course_id = :ownerId
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public ReferenceCleanupRepository(@Qualifier(FacilityDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Runs one cleanup statement for the deleted owner; the ids of the rows it changed. */
    public List<UUID> apply(String statement, UUID ownerId, OffsetDateTime now) {
        return jdbc.sql(statement).param("ownerId", ownerId).param("now", now).query(UUID.class).list();
    }
}
