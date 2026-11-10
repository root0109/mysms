package com.mysms.people.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.people.config.PeopleDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of people's reference cleanup (database/logical-references.md): each changes the rows of one logical
 * reference to a deleted owner, inside the event's tenant, and returns their ids.
 */
@Repository
public class ReferenceCleanupRepository {

    /**
     * people.consent_record.evidence_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_CONSENT_RECORD_EVIDENCE_FILE_ID = """
            UPDATE people.consent_record
               SET evidence_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND evidence_file_id = :ownerId
            RETURNING id
            """;

    /**
     * people.person.photo_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_PERSON_PHOTO_FILE_ID = """
            UPDATE people.person
               SET photo_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND photo_file_id = :ownerId
            RETURNING id
            """;

    /**
     * people.public_profile.cover_image_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_PUBLIC_PROFILE_COVER_IMAGE_FILE_ID = """
            UPDATE people.public_profile
               SET cover_image_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND cover_image_file_id = :ownerId
            RETURNING id
            """;

    /**
     * people.consent_record.recorded_by_user_id, on iam.user_account deletion: null the column.
     */
    public static final String CLEAR_CONSENT_RECORD_RECORDED_BY_USER_ID = """
            UPDATE people.consent_record
               SET recorded_by_user_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND recorded_by_user_id = :ownerId
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public ReferenceCleanupRepository(@Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Runs one cleanup statement for the deleted owner; the ids of the rows it changed. */
    public List<UUID> apply(String statement, UUID ownerId, OffsetDateTime now) {
        return jdbc.sql(statement).param("ownerId", ownerId).param("now", now).query(UUID.class).list();
    }
}
