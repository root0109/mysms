package com.mysms.assessment.repository;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.domain.AssignmentApprovalEntity;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
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
 * Plain-JDBC access to assessment.assignment_approval (JAVA-CODING-RULES §6): schema-qualified constant SQL, named
 * parameters, live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class AssignmentApprovalRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("assignmentId", "r.assignment_id", ValueType.UUID),
            field("decision", "r.decision", ValueType.STRING),
            field("comments", "r.comments", ValueType.STRING),
            field("decidedByUserId", "r.decided_by_user_id", ValueType.UUID),
            field("decidedAt", "r.decided_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<AssignmentApprovalEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", AssignmentApprovalEntity::getCreatedAt),
            Map.entry("updateTime", AssignmentApprovalEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.assignment_id, r.decision, r.comments, r.decided_by_user_id, r.decided_at,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM assessment.assignment_approval r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO assessment.assignment_approval
            (id, tenant_id, assignment_id, decision, comments, decided_by_user_id, decided_at, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :assignmentId, COALESCE(:decision, 'pending'::character varying), :comments,
                    :decidedByUserId, :decidedAt, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, assignment_id, decision, comments, decided_by_user_id, decided_at, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE assessment.assignment_approval
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE assessment.assignment_approval
               SET decision = :decision,
                   comments = :comments,
                   decided_by_user_id = :decidedByUserId,
                   decided_at = :decidedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_ASSIGNMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.assignment WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM assessment.assignment p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.assignment_id IN (SELECT p0.id FROM assessment.assignment p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public AssignmentApprovalRepository(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AssignmentApprovalEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AssignmentApprovalRepository::map).optional();
    }

    public Page<AssignmentApprovalEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AssignmentApprovalEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(AssignmentApprovalRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), AssignmentApprovalEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public AssignmentApprovalEntity insert(AssignmentApprovalEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("assignmentId", e.getAssignmentId())
                .param("decision", e.getDecision())
                .param("comments", e.getComments())
                .param("decidedByUserId", e.getDecidedByUserId())
                .param("decidedAt", e.getDecidedAt())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(AssignmentApprovalRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(AssignmentApprovalEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("decision", e.getDecision())
                .param("comments", e.getComments())
                .param("decidedByUserId", e.getDecidedByUserId())
                .param("decidedAt", e.getDecidedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(AssignmentApprovalEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced assessment.assignment row exists (live). */
    public boolean existsAssignmentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ASSIGNMENT_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through assignment_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.assignment_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, AssignmentApprovalEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static AssignmentApprovalEntity map(ResultSet rs, int row) throws SQLException {
        return AssignmentApprovalEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .assignmentId(rs.getObject("assignment_id", UUID.class))
                .decision(rs.getString("decision"))
                .comments(rs.getString("comments"))
                .decidedByUserId(rs.getObject("decided_by_user_id", UUID.class))
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
