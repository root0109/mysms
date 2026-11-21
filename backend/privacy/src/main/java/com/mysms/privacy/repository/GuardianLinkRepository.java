package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * privacy.guardian_link: who is whose guardian, kept from sis.student_guardian events. A row changes only to a newer
 * source row version, so a late or repeated event changes nothing; a deleted link stays as an inactive row. The
 * consent rules read who may decide for whom from the active legal-guardian links.
 */
@Repository
public class GuardianLinkRepository {

    /** The link as one sis.student_guardian event describes it. */
    public record Link(UUID linkId, UUID studentPersonId, UUID guardianPersonId, UUID guardianUserId,
            boolean legalGuardian, boolean custody, String status, int version) {
    }

    private static final String APPLY = """
            INSERT INTO privacy.guardian_link (tenant_id, link_id, student_person_id, guardian_person_id,
                                               guardian_user_id, is_legal_guardian, has_custody, status,
                                               source_row_version, updated_at)
            VALUES (:tenantId, :linkId, :studentPersonId, :guardianPersonId, :guardianUserId, :legalGuardian,
                    :custody, :status, :version, :now)
            ON CONFLICT (tenant_id, link_id) DO UPDATE
               SET student_person_id = EXCLUDED.student_person_id, guardian_person_id = EXCLUDED.guardian_person_id,
                   guardian_user_id = EXCLUDED.guardian_user_id, is_legal_guardian = EXCLUDED.is_legal_guardian,
                   has_custody = EXCLUDED.has_custody, status = EXCLUDED.status,
                   source_row_version = EXCLUDED.source_row_version, updated_at = EXCLUDED.updated_at
             WHERE privacy.guardian_link.source_row_version < EXCLUDED.source_row_version
            """;

    private static final String LEGAL_GUARDIANS = """
            SELECT DISTINCT g.guardian_person_id
              FROM privacy.guardian_link g
             WHERE g.tenant_id = :tenantId
               AND g.student_person_id = :studentPersonId AND g.status = 'active' AND g.is_legal_guardian
            """;
    private static final String IS_LEGAL_GUARDIAN = """
            SELECT EXISTS (SELECT 1 FROM privacy.guardian_link g
                            WHERE g.tenant_id = :tenantId
                              AND g.guardian_person_id = :guardianPersonId AND g.student_person_id = :studentPersonId
                              AND g.status = 'active' AND g.is_legal_guardian)
            """;
    /** A student without a subject row yet is of unknown age, which counts as a minor. */
    private static final String MINOR_WARDS = """
            SELECT DISTINCT g.student_person_id
              FROM privacy.guardian_link g
              LEFT JOIN privacy.subject s ON s.tenant_id = :tenantId AND s.person_id = g.student_person_id
             WHERE g.tenant_id = :tenantId
               AND g.guardian_person_id = :guardianPersonId AND g.status = 'active' AND g.is_legal_guardian
               AND (s.status IS NULL OR s.status <> 'adult')
             ORDER BY g.student_person_id
            """;

    private final JdbcClient jdbc;

    public GuardianLinkRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The persons with an active legal-guardian link to the student: who may decide for a minor. */
    public Set<UUID> legalGuardians(UUID studentPersonId) {
        return Set.copyOf(jdbc.sql(LEGAL_GUARDIANS).param("studentPersonId", studentPersonId).query(UUID.class).list());
    }

    /** Whether the person has an active legal-guardian link to the student (identified by persons, not accounts). */
    public boolean isLegalGuardian(UUID guardianPersonId, UUID studentPersonId) {
        return Boolean.TRUE.equals(jdbc.sql(IS_LEGAL_GUARDIAN).param("guardianPersonId", guardianPersonId)
                .param("studentPersonId", studentPersonId).query(Boolean.class).single());
    }

    /** The students the person is the active legal guardian of who are not known to be adults. */
    public List<UUID> minorWards(UUID guardianPersonId) {
        return jdbc.sql(MINOR_WARDS).param("guardianPersonId", guardianPersonId).query(UUID.class).list();
    }

    /** Applies one event of a link; whether the stored row changed (false for an older or repeated version). */
    public boolean apply(Link link, OffsetDateTime now) {
        return jdbc.sql(APPLY).param("linkId", link.linkId()).param("studentPersonId", link.studentPersonId())
                .param("guardianPersonId", link.guardianPersonId()).param("guardianUserId", link.guardianUserId())
                .param("legalGuardian", link.legalGuardian()).param("custody", link.custody())
                .param("status", link.status()).param("version", link.version()).param("now", now).update() == 1;
    }
}
