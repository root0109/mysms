package com.mysms.learning.lms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.domain.EnrollmentEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
/**
 * Plain-JDBC access to lms.enrollment: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class EnrollmentRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("courseId", "r.course_id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("memberType", "r.member_type", ValueType.STRING),
            field("memberRole", "r.member_role", ValueType.STRING),
            field("progressPercent", "r.progress_percent", ValueType.DECIMAL),
            field("currentLessonId", "r.current_lesson_id", ValueType.UUID),
            field("sourceCohortId", "r.source_cohort_id", ValueType.UUID),
            field("paymentId", "r.payment_id", ValueType.UUID),
            field("purchasedCertificate", "r.purchased_certificate", ValueType.BOOLEAN),
            field("certificateId", "r.certificate_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<EnrollmentEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", EnrollmentEntity::getCreatedAt),
            Map.entry("updateTime", EnrollmentEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.course_id, r.user_id, r.member_type, r.member_role, r.progress_percent,
                   r.current_lesson_id, r.source_cohort_id, r.payment_id, r.purchased_certificate, r.certificate_id,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.enrollment r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    /** A user's live enrolment in a course (enrolments are unique per course and user). */
    private static final String FIND_OWN = SELECT_LIVE + " AND r.course_id = :courseId AND r.user_id = :userId";
    /** A user's live enrolments, newest first (the student dashboard). */
    private static final String LIST_OWN = SELECT_LIVE + " AND r.user_id = :userId ORDER BY r.updated_at DESC, r.id"
            + " LIMIT :limit";
    /**
     * Recomputes a student's progress in a course from their completed lessons and SCORM chapters over the course's
     * lessons and SCORM chapters (template chapters only), capped at 100, and remembers where they are.
     */
    private static final String RECORD_PROGRESS = """
            UPDATE lms.enrollment
               SET progress_percent = LEAST(100, ROUND(100.0 * (
                       SELECT count(*) FROM lms.lesson_progress p
                        WHERE p.tenant_id = :tenantId AND p.course_id = :courseId AND p.user_id = :userId
                          AND p.status = 'complete' AND p.deleted_at IS NULL) / GREATEST(1, (
                       SELECT count(*) FROM lms.lesson l JOIN lms.chapter c ON c.tenant_id = :tenantId
                              AND c.id = l.chapter_id AND c.deleted_at IS NULL AND c.cohort_id IS NULL
                        WHERE l.tenant_id = :tenantId AND l.deleted_at IS NULL AND c.course_id = :courseId
                          AND NOT c.is_scorm_package) + (
                       SELECT count(*) FROM lms.chapter c
                        WHERE c.tenant_id = :tenantId AND c.course_id = :courseId AND c.deleted_at IS NULL
                          AND c.cohort_id IS NULL AND c.is_scorm_package)), 2)),
                   current_lesson_id = COALESCE(:lessonId, current_lesson_id),
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND course_id = :courseId AND user_id = :userId AND deleted_at IS NULL
            """;
    private static final String INSERT = """
            INSERT INTO lms.enrollment
            (id, tenant_id, course_id, user_id, member_type, member_role, progress_percent, current_lesson_id,
             source_cohort_id, payment_id, purchased_certificate, certificate_id, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :courseId, :userId, COALESCE(:memberType, 'student'::character varying),
                    COALESCE(:memberRole, 'member'::character varying), COALESCE(:progressPercent, 0),
                    :currentLessonId, :sourceCohortId, :paymentId, COALESCE(:purchasedCertificate, false),
                    :certificateId, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, course_id, user_id, member_type, member_role, progress_percent, current_lesson_id,
                      source_cohort_id, payment_id, purchased_certificate, certificate_id, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.enrollment
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.enrollment
               SET member_role = :memberRole,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String IX_ENROLLMENT_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.enrollment
                            WHERE tenant_id = :tenantId
                              AND course_id = :courseId AND user_id = :userId AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_CERTIFICATE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.certificate WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.course WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_CURRENT_LESSON_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.lesson WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_SOURCE_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.course p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.course_id IN (SELECT p0.id FROM lms.course p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)))
            """;

    private final JdbcClient jdbc;

    public EnrollmentRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<EnrollmentEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(EnrollmentRepository::map).optional();
    }

    public Page<EnrollmentEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<EnrollmentEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(EnrollmentRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), EnrollmentEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public EnrollmentEntity insert(EnrollmentEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("courseId", e.getCourseId())
                .param("userId", e.getUserId())
                .param("memberType", e.getMemberType())
                .param("memberRole", e.getMemberRole())
                .param("progressPercent", e.getProgressPercent())
                .param("currentLessonId", e.getCurrentLessonId())
                .param("sourceCohortId", e.getSourceCohortId())
                .param("paymentId", e.getPaymentId())
                .param("purchasedCertificate", e.getPurchasedCertificate())
                .param("certificateId", e.getCertificateId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(EnrollmentRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(EnrollmentEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("memberRole", e.getMemberRole())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(EnrollmentEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** ix_enrollment_1: course_id, user_id where (deleted_at IS NULL). */
    public boolean takenCourseIdUserId1(EnrollmentEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(IX_ENROLLMENT_1_TAKEN)
                .param("courseId", e.getCourseId())
                .param("userId", e.getUserId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced lms.certificate row exists (live). */
    public boolean existsCertificateId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CERTIFICATE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.course row exists (live). */
    public boolean existsCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COURSE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.lesson row exists (live). */
    public boolean existsCurrentLessonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CURRENT_LESSON_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.cohort row exists (live). */
    public boolean existsSourceCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_SOURCE_COHORT_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through course_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** The user's live enrolment in the course. */
    public Optional<EnrollmentEntity> findOwn(UUID courseId, UUID userId) {
        return jdbc.sql(FIND_OWN).param("courseId", courseId).param("userId", userId)
                .query(EnrollmentRepository::map).optional();
    }

    /** The user's live enrolments, most recently active first, at most {@code limit}. */
    public List<EnrollmentEntity> listOwn(UUID userId, int limit) {
        return jdbc.sql(LIST_OWN).param("userId", userId).param("limit", limit)
                .query(EnrollmentRepository::map).list();
    }

    /** Recomputes the student's progress in the course (and where they are, when a lesson is given). */
    public void recordProgress(UUID courseId, UUID userId, UUID lessonId, OffsetDateTime now, UUID actor) {
        jdbc.sql(RECORD_PROGRESS).param("courseId", courseId).param("userId", userId).param("lessonId", lessonId)
                .param("now", now).param("actor", actor).update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, EnrollmentEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static EnrollmentEntity map(ResultSet rs, int row) throws SQLException {
        return EnrollmentEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .memberType(rs.getString("member_type"))
                .memberRole(rs.getString("member_role"))
                .progressPercent(rs.getBigDecimal("progress_percent"))
                .currentLessonId(rs.getObject("current_lesson_id", UUID.class))
                .sourceCohortId(rs.getObject("source_cohort_id", UUID.class))
                .paymentId(rs.getObject("payment_id", UUID.class))
                .purchasedCertificate(rs.getObject("purchased_certificate", Boolean.class))
                .certificateId(rs.getObject("certificate_id", UUID.class))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .deletedAt(rs.getObject("deleted_at", OffsetDateTime.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
