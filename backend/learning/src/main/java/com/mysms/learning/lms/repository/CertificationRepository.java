package com.mysms.learning.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC reads of certification: who has finished a cohort, who already holds a
 * certificate for a cohort or a course, and a certificate as a verifier sees it. Certificates themselves are written
 * through {@link CertificateRepository}. Every statement filters the caller's tenant itself.
 */
@Repository
public class CertificationRepository {

    private static final String GRADUATES = """
            SELECT user_id FROM lms.cohort_enrollment
             WHERE tenant_id = :tenantId AND cohort_id = :cohortId AND is_active AND grade_published
               AND deleted_at IS NULL
             ORDER BY created_at, id
            """;
    private static final String HOLDS_FOR_COHORT = """
            SELECT EXISTS (SELECT 1 FROM lms.certificate WHERE tenant_id = :tenantId AND cohort_id = :cohortId
                              AND user_id = :userId AND deleted_at IS NULL)
            """;
    private static final String HOLDS_FOR_COURSE = """
            SELECT EXISTS (SELECT 1 FROM lms.certificate WHERE tenant_id = :tenantId AND course_id = :courseId
                              AND cohort_id IS NOT DISTINCT FROM :cohortId AND user_id = :userId
                              AND deleted_at IS NULL)
            """;
    private static final String VERIFICATION = """
            SELECT c.is_published, c.issue_date, c.expiry_date, COALESCE(h.title, k.title) AS title
              FROM lms.certificate c
              LEFT JOIN lms.cohort h ON h.tenant_id = :tenantId AND h.id = c.cohort_id
              LEFT JOIN lms.course k ON k.tenant_id = :tenantId AND k.id = c.course_id
             WHERE c.tenant_id = :tenantId AND c.id = :id AND c.deleted_at IS NULL
            """;

    private static final String SET_REQUEST_STATUS = """
            UPDATE lms.certificate_request
               SET status = :status, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EVALUATION_OF_REQUEST = """
            SELECT EXISTS (SELECT 1 FROM lms.certificate_evaluation WHERE tenant_id = :tenantId
                              AND certificate_request_id = :requestId AND deleted_at IS NULL)
            """;
    private static final String EVALUATOR_OF_USER = """
            SELECT id FROM lms.evaluator WHERE tenant_id = :tenantId AND user_id = :userId AND deleted_at IS NULL
             ORDER BY created_at, id LIMIT 1
            """;
    private static final String AWARDED = """
            SELECT EXISTS (SELECT 1 FROM lms.badge_award WHERE tenant_id = :tenantId AND badge_id = :badgeId
                              AND user_id = :userId AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public CertificationRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** A certificate as a verifier sees it. */
    public record Verification(boolean isPublished, LocalDate issueDate, LocalDate expiryDate, String title) {
    }

    /** The cohort's active students whose final grade is published, in the order they joined. */
    public List<UUID> graduates(UUID cohortId) {
        return jdbc.sql(GRADUATES).param("cohortId", cohortId).query(UUID.class).list();
    }

    /** Whether the student already holds a live certificate of the cohort. */
    public boolean holdsForCohort(UUID cohortId, UUID userId) {
        return Boolean.TRUE.equals(jdbc.sql(HOLDS_FOR_COHORT).param("cohortId", cohortId).param("userId", userId)
                .query(Boolean.class).single());
    }

    /** Whether the student already holds a live certificate of the course (in the same cohort, or none). */
    public boolean holdsForCourse(UUID courseId, UUID cohortId, UUID userId) {
        return Boolean.TRUE.equals(jdbc.sql(HOLDS_FOR_COURSE).param("courseId", courseId).param("cohortId", cohortId)
                .param("userId", userId).query(Boolean.class).single());
    }

    /** Moves a certificate request to a status under its row_version guard. */
    public int setRequestStatus(UUID id, int expected, String status, OffsetDateTime now, UUID actor) {
        return jdbc.sql(SET_REQUEST_STATUS).param("id", id).param("expected", expected).param("status", status)
                .param("now", now).param("actor", actor).update();
    }

    /** Whether the booking already has an evaluation. */
    public boolean hasEvaluation(UUID requestId) {
        return Boolean.TRUE.equals(jdbc.sql(EVALUATION_OF_REQUEST).param("requestId", requestId).query(Boolean.class)
                .single());
    }

    /** The evaluator record of a user, if they are one. */
    public Optional<UUID> evaluatorOf(UUID userId) {
        return jdbc.sql(EVALUATOR_OF_USER).param("userId", userId).query(UUID.class).optional();
    }

    /** Whether the user already holds the badge. */
    public boolean awarded(UUID badgeId, UUID userId) {
        return Boolean.TRUE.equals(jdbc.sql(AWARDED).param("badgeId", badgeId).param("userId", userId)
                .query(Boolean.class).single());
    }

    public Optional<Verification> verification(UUID id) {
        return jdbc.sql(VERIFICATION).param("id", id)
                .query((rs, n) -> new Verification(rs.getBoolean("is_published"),
                        rs.getObject("issue_date", LocalDate.class), rs.getObject("expiry_date", LocalDate.class),
                        rs.getString("title")))
                .optional();
    }
}
