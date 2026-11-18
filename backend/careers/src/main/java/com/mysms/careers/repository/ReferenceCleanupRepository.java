package com.mysms.careers.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.careers.config.CareersDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of careers's reference cleanup: each changes the rows of one logical reference to a deleted owner,
 * inside the event's tenant, and returns their ids.
 */
@Repository
public class ReferenceCleanupRepository {

    /**
     * careers.candidate_preference.person_id, on people.person deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_CANDIDATE_PREFERENCE_PERSON_ID = """
            UPDATE careers.candidate_preference
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND person_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public ReferenceCleanupRepository(@Qualifier(CareersDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Runs one cleanup statement for the deleted owner; the ids of the rows it changed. */
    public List<UUID> apply(String statement, UUID ownerId, OffsetDateTime now) {
        return jdbc.sql(statement).param("ownerId", ownerId).param("now", now).query(UUID.class).list();
    }
}
