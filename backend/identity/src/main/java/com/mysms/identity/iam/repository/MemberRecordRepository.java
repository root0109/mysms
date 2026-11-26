package com.mysms.identity.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * iam.member_record: the staff and student records of each login account and person, kept from hr's and sis's events.
 * A row changes only to a newer source row version, so a late or repeated event
 * changes nothing, and a deleted record stays as a tombstone a late event cannot bring back.
 */
@Repository
public class MemberRecordRepository {

    /** The kinds of record kept: the hr.staff and sis.student rows. */
    public enum Kind {
        STAFF("staff"), STUDENT("student");

        private final String column;

        Kind(String column) {
            this.column = column;
        }

        public String column() {
            return column;
        }
    }

    private static final String APPLY = """
            INSERT INTO iam.member_record (tenant_id, record_id, record_type, user_id, person_id, institution_id,
                                           is_deleted, source_version, synced_at)
            VALUES (:tenantId, :recordId, :recordType, :userId, :personId, :institutionId, :deleted, :version, :now)
            ON CONFLICT (tenant_id, record_id) DO UPDATE
               SET user_id = EXCLUDED.user_id, person_id = EXCLUDED.person_id,
                   institution_id = EXCLUDED.institution_id, is_deleted = EXCLUDED.is_deleted,
                   source_version = EXCLUDED.source_version, synced_at = EXCLUDED.synced_at
             WHERE iam.member_record.source_version < EXCLUDED.source_version
               AND iam.member_record.record_type = EXCLUDED.record_type
            """;

    /** The live records of one kind that belong to the account or to the person, oldest record first. */
    private static final String RECORDS_OF = """
            SELECT m.record_id
              FROM iam.member_record m
             WHERE m.tenant_id = :tenantId AND m.record_type = :recordType AND NOT m.is_deleted
               AND (m.user_id = :userId OR m.person_id = :personId)
             ORDER BY m.record_id
            """;

    /** A deleted account no longer owns the records that named it (their own module clears its column too). */
    private static final String FORGET_ACCOUNT = """
            UPDATE iam.member_record
               SET user_id = NULL, synced_at = :now
             WHERE tenant_id = :tenantId AND user_id = :userId
            """;

    private final JdbcClient jdbc;

    public MemberRecordRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /**
     * Applies one event of a record; whether the stored entry changed (false for an older or repeated version).
     *
     * @param deleted whether the event is the record's deletion
     */
    public boolean apply(Kind kind, UUID recordId, UUID userId, UUID personId, UUID institutionId, boolean deleted,
            int version, OffsetDateTime now) {
        return jdbc.sql(APPLY).param("recordId", recordId).param("recordType", kind.column())
                .param("userId", userId).param("personId", personId).param("institutionId", institutionId)
                .param("deleted", deleted).param("version", version).param("now", now).update() == 1;
    }

    /** Takes a deleted account off the records that named it. */
    public void forgetAccount(UUID userId, OffsetDateTime now) {
        jdbc.sql(FORGET_ACCOUNT).param("userId", userId).param("now", now).update();
    }

    /**
     * The live records of one kind of the account or of the person (UUIDv7 ids: oldest first).
     *
     * @param personId the person of the account, or null when it has none
     */
    public List<UUID> recordsOf(Kind kind, UUID userId, UUID personId) {
        return jdbc.sql(RECORDS_OF).param("recordType", kind.column()).param("userId", userId)
                .param("personId", personId).query(UUID.class).list();
    }
}
