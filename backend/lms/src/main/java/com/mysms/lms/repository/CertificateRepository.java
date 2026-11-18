package com.mysms.lms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.CertificateEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
 * Plain-JDBC access to lms.certificate: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class CertificateRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("courseId", "r.course_id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("evaluatorId", "r.evaluator_id", ValueType.UUID),
            field("certificateNumber", "r.certificate_number", ValueType.STRING),
            field("issueDate", "r.issue_date", ValueType.DATE),
            field("expiryDate", "r.expiry_date", ValueType.DATE),
            field("templateCode", "r.template_code", ValueType.STRING),
            field("isPublished", "r.is_published", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<CertificateEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", CertificateEntity::getCreatedAt),
            Map.entry("updateTime", CertificateEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.user_id, r.course_id, r.cohort_id, r.evaluator_id, r.certificate_number,
                   r.issue_date, r.expiry_date, r.template_code, r.is_published, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.certificate r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.certificate
            (id, tenant_id, user_id, course_id, cohort_id, evaluator_id, certificate_number, issue_date, expiry_date,
             template_code, is_published, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :userId, :courseId, :cohortId, :evaluatorId, :certificateNumber, :issueDate,
                    :expiryDate, :templateCode, COALESCE(:isPublished, false), :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, user_id, course_id, cohort_id, evaluator_id, certificate_number, issue_date,
                      expiry_date, template_code, is_published, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.certificate
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.certificate
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.course WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_EVALUATOR_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.evaluator WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ENROLLMENT_CERTIFICATE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.enrollment WHERE tenant_id = :tenantId
                AND certificate_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.course p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.course_id IN (SELECT p0.id FROM lms.course p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)) OR r.course_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public CertificateRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CertificateEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(CertificateRepository::map).optional();
    }

    public Page<CertificateEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<CertificateEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(CertificateRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), CertificateEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public CertificateEntity insert(CertificateEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("userId", e.getUserId())
                .param("courseId", e.getCourseId())
                .param("cohortId", e.getCohortId())
                .param("evaluatorId", e.getEvaluatorId())
                .param("certificateNumber", e.getCertificateNumber())
                .param("issueDate", e.getIssueDate())
                .param("expiryDate", e.getExpiryDate())
                .param("templateCode", e.getTemplateCode())
                .param("isPublished", e.getIsPublished())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(CertificateRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(CertificateEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(CertificateEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.cohort row exists (live). */
    public boolean existsCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COHORT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.course row exists (live). */
    public boolean existsCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COURSE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.evaluator row exists (live). */
    public boolean existsEvaluatorId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EVALUATOR_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.enrollment rows still reference the row. */
    public boolean referencedByEnrollmentCertificateId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ENROLLMENT_CERTIFICATE_ID).param("id", id)
                .query(Boolean.class).single());
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

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static final String FIND_OWN_FOR_COURSE = SELECT_LIVE
            + " AND r.user_id = :userId AND r.course_id = :courseId ORDER BY r.created_at LIMIT 1";

    /** The user's live certificate for the course, when one was issued. */
    public Optional<CertificateEntity> findOwnForCourse(UUID userId, UUID courseId) {
        return jdbc.sql(FIND_OWN_FOR_COURSE).param("userId", userId).param("courseId", courseId)
                .query(CertificateRepository::map).optional();
    }

    private static List<Object> sortValues(SortSpec sort, CertificateEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static CertificateEntity map(ResultSet rs, int row) throws SQLException {
        return CertificateEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .evaluatorId(rs.getObject("evaluator_id", UUID.class))
                .certificateNumber(rs.getString("certificate_number"))
                .issueDate(rs.getObject("issue_date", LocalDate.class))
                .expiryDate(rs.getObject("expiry_date", LocalDate.class))
                .templateCode(rs.getString("template_code"))
                .isPublished(rs.getObject("is_published", Boolean.class))
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
