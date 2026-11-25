package com.mysms.comms.repository;

import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.core.db.ModuleDatabase;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * comms.guardian_link: which guardian (record, person, account) is linked to which student, kept from sis's guardian
 * link events. A row changes only to a newer source row version, so a late or repeated event changes nothing.
 */
@Repository
public class GuardianLinkRepository {

    /** One announced state of a link. */
    public record Link(UUID linkId, UUID studentId, UUID guardianId, UUID guardianPersonId, UUID guardianUserId,
            String status, boolean deleted, int version) {
    }

    private static final String APPLY = """
            INSERT INTO comms.guardian_link (tenant_id, link_id, student_id, guardian_id, guardian_person_id,
                                             guardian_user_id, status, is_deleted, source_version, synced_at)
            VALUES (:tenantId, :linkId, :studentId, :guardianId, :guardianPersonId, :guardianUserId, :status, :deleted,
                    :version, :now)
            ON CONFLICT (tenant_id, link_id) DO UPDATE
               SET student_id = EXCLUDED.student_id, guardian_id = EXCLUDED.guardian_id,
                   guardian_person_id = EXCLUDED.guardian_person_id, guardian_user_id = EXCLUDED.guardian_user_id,
                   status = EXCLUDED.status, is_deleted = EXCLUDED.is_deleted,
                   source_version = EXCLUDED.source_version, synced_at = EXCLUDED.synced_at
             WHERE comms.guardian_link.source_version < EXCLUDED.source_version
            """;

    private final JdbcClient jdbc;

    public GuardianLinkRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Applies one announced state of a link; whether the stored entry changed (false for an older or repeated one). */
    public boolean apply(Link link, OffsetDateTime now) {
        return jdbc.sql(APPLY).param("linkId", link.linkId()).param("studentId", link.studentId())
                .param("guardianId", link.guardianId()).param("guardianPersonId", link.guardianPersonId())
                .param("guardianUserId", link.guardianUserId()).param("status", link.status())
                .param("deleted", link.deleted()).param("version", link.version()).param("now", now).update() == 1;
    }
}
