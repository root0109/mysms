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
import com.mysms.learning.lms.domain.CertificateEvaluationEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
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
 * Plain-JDBC access to lms.certificate_evaluation: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class CertificateEvaluationRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("courseId", "r.course_id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("certificateRequestId", "r.certificate_request_id", ValueType.UUID),
            field("evaluatorUserId", "r.evaluator_user_id", ValueType.UUID),
            field("evaluationDate", "r.evaluation_date", ValueType.DATE),
            field("rating", "r.rating", ValueType.DECIMAL),
            field("summary", "r.summary", ValueType.STRING),
            field("status", "r.status", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<CertificateEvaluationEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", CertificateEvaluationEntity::getCreatedAt),
            Map.entry("updateTime", CertificateEvaluationEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.course_id, r.user_id, r.cohort_id, r.certificate_request_id,
                   r.evaluator_user_id, r.evaluation_date, r.start_time, r.end_time, r.rating, r.summary, r.status,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.certificate_evaluation r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.certificate_evaluation
            (id, tenant_id, course_id, user_id, cohort_id, certificate_request_id, evaluator_user_id,
             evaluation_date, start_time, end_time, rating, summary, status, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :courseId, :userId, :cohortId, :certificateRequestId, :evaluatorUserId,
                    :evaluationDate, :startTime, :endTime, :rating, :summary,
                    COALESCE(:status, 'pending'::character varying), :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, course_id, user_id, cohort_id, certificate_request_id, evaluator_user_id,
                      evaluation_date, start_time, end_time, rating, summary, status, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.certificate_evaluation
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.certificate_evaluation
               SET course_id = :courseId,
                   user_id = :userId,
                   cohort_id = :cohortId,
                   certificate_request_id = :certificateRequestId,
                   evaluator_user_id = :evaluatorUserId,
                   evaluation_date = :evaluationDate,
                   start_time = :startTime,
                   end_time = :endTime,
                   rating = :rating,
                   summary = :summary,
                   status = :status,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_CERTIFICATE_REQUEST_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.certificate_request WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.course WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
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

    public CertificateEvaluationRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CertificateEvaluationEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(CertificateEvaluationRepository::map).optional();
    }

    public Page<CertificateEvaluationEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<CertificateEvaluationEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(CertificateEvaluationRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), CertificateEvaluationEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public CertificateEvaluationEntity insert(CertificateEvaluationEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("courseId", e.getCourseId())
                .param("userId", e.getUserId())
                .param("cohortId", e.getCohortId())
                .param("certificateRequestId", e.getCertificateRequestId())
                .param("evaluatorUserId", e.getEvaluatorUserId())
                .param("evaluationDate", e.getEvaluationDate())
                .param("startTime", e.getStartTime())
                .param("endTime", e.getEndTime())
                .param("rating", e.getRating())
                .param("summary", e.getSummary())
                .param("status", e.getStatus())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(CertificateEvaluationRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(CertificateEvaluationEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("courseId", e.getCourseId())
                .param("userId", e.getUserId())
                .param("cohortId", e.getCohortId())
                .param("certificateRequestId", e.getCertificateRequestId())
                .param("evaluatorUserId", e.getEvaluatorUserId())
                .param("evaluationDate", e.getEvaluationDate())
                .param("startTime", e.getStartTime())
                .param("endTime", e.getEndTime())
                .param("rating", e.getRating())
                .param("summary", e.getSummary())
                .param("status", e.getStatus())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(CertificateEvaluationEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.certificate_request row exists (live). */
    public boolean existsCertificateRequestId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CERTIFICATE_REQUEST_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether the referenced lms.cohort row exists (live). */
    public boolean existsCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COHORT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.course row exists (live). */
    public boolean existsCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COURSE_ID).param("id", id).query(Boolean.class).single());
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

    private static List<Object> sortValues(SortSpec sort, CertificateEvaluationEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static CertificateEvaluationEntity map(ResultSet rs, int row) throws SQLException {
        return CertificateEvaluationEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .certificateRequestId(rs.getObject("certificate_request_id", UUID.class))
                .evaluatorUserId(rs.getObject("evaluator_user_id", UUID.class))
                .evaluationDate(rs.getObject("evaluation_date", LocalDate.class))
                .startTime(rs.getObject("start_time", LocalTime.class))
                .endTime(rs.getObject("end_time", LocalTime.class))
                .rating(rs.getBigDecimal("rating"))
                .summary(rs.getString("summary"))
                .status(rs.getString("status"))
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
