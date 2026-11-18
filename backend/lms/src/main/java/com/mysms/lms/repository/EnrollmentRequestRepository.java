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
import com.mysms.lms.domain.EnrollmentRequestEntity;
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
 * Plain-JDBC access to lms.enrollment_request: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class EnrollmentRequestRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("courseId", "r.course_id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("requestedAt", "r.requested_at", ValueType.TIMESTAMP),
            field("status", "r.status", ValueType.STRING),
            field("decidedBy", "r.decided_by", ValueType.UUID),
            field("decidedAt", "r.decided_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<EnrollmentRequestEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", EnrollmentRequestEntity::getCreatedAt),
            Map.entry("updateTime", EnrollmentRequestEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.course_id, r.cohort_id, r.user_id, r.requested_at, r.status, r.decided_by,
                   r.decided_at, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.enrollment_request r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.enrollment_request
            (id, tenant_id, course_id, cohort_id, user_id, requested_at, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :courseId, :cohortId, :userId, COALESCE(:requestedAt, now()), :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, course_id, cohort_id, user_id, requested_at, status, decided_by, decided_at,
                      created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.enrollment_request
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.enrollment_request
               SET cohort_id = :cohortId,
                   user_id = :userId,
                   requested_at = :requestedAt,
                   updated_at = :updatedAt,
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
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.course p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;

    /** lms.RejectEnrollmentRequest: allowed while status is pending. */
    private static final String TRANSITION_REJECT_ENROLLMENT_REQUEST = """
            UPDATE lms.enrollment_request
               SET status = 'rejected',
                   decided_by = :actor,
                   decided_at = :now,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.course_id IN (SELECT p0.id FROM lms.course p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)))
            """;

    private final JdbcClient jdbc;

    public EnrollmentRequestRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<EnrollmentRequestEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(EnrollmentRequestRepository::map).optional();
    }

    public Page<EnrollmentRequestEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<EnrollmentRequestEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(EnrollmentRequestRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), EnrollmentRequestEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public EnrollmentRequestEntity insert(EnrollmentRequestEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("courseId", e.getCourseId())
                .param("cohortId", e.getCohortId())
                .param("userId", e.getUserId())
                .param("requestedAt", e.getRequestedAt())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(EnrollmentRequestRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(EnrollmentRequestEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("cohortId", e.getCohortId())
                .param("userId", e.getUserId())
                .param("requestedAt", e.getRequestedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(EnrollmentRequestEntity e, int expectedVersion) {
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

    /** The institution owning the row reached through course_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** lms.RejectEnrollmentRequest; 0 rows means the row changed (or vanished) since it was read. */
    public int rejectEnrollmentRequest(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_REJECT_ENROLLMENT_REQUEST)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.course_id = :parentId", Map.of("parentId", parentId));
    }

    /** lms.ApproveEnrollmentRequest: allowed while status is pending. */
    private static final String TRANSITION_APPROVE_ENROLLMENT_REQUEST = """
            UPDATE lms.enrollment_request
               SET status = 'approved',
                   decided_by = :actor,
                   decided_at = :now,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** Approves the pending request; 0 rows means it changed since it was read. */
    public int approve(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_APPROVE_ENROLLMENT_REQUEST).param("id", id).param("expected", expected)
                .param("now", now).param("actor", actor).update();
    }

    private static List<Object> sortValues(SortSpec sort, EnrollmentRequestEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static EnrollmentRequestEntity map(ResultSet rs, int row) throws SQLException {
        return EnrollmentRequestEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .requestedAt(rs.getObject("requested_at", OffsetDateTime.class))
                .status(rs.getString("status"))
                .decidedBy(rs.getObject("decided_by", UUID.class))
                .decidedAt(rs.getObject("decided_at", OffsetDateTime.class))
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
