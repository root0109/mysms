package com.mysms.academics.assessment.repository;
import com.mysms.academics.assessment.config.AssessmentDatabaseConfig;
import com.mysms.academics.assessment.domain.HomeworkApprovalEntity;
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
 * Plain-JDBC access to assessment.homework_approval: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class HomeworkApprovalRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("homeworkId", "r.homework_id", ValueType.UUID),
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

    private static final Map<String, Function<HomeworkApprovalEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", HomeworkApprovalEntity::getCreatedAt),
            Map.entry("updateTime", HomeworkApprovalEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.homework_id, r.decision, r.comments, r.decided_by_user_id, r.decided_at,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM assessment.homework_approval r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO assessment.homework_approval
            (id, tenant_id, homework_id, decision, comments, decided_by_user_id, decided_at, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :homeworkId, COALESCE(:decision, 'pending'::character varying), :comments,
                    :decidedByUserId, :decidedAt, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, homework_id, decision, comments, decided_by_user_id, decided_at, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    /** The open reviews of one homework (the workflow keeps at most one), locked for the decision. */
    private static final String FIND_PENDING = SELECT_LIVE
            + " AND r.homework_id = :homeworkId AND r.decision = 'pending' ORDER BY r.created_at, r.id FOR UPDATE";
    /** The reviewer's decision on an open review, under the version it was read with. */
    private static final String DECIDE = """
            UPDATE assessment.homework_approval
               SET decision = :decision,
                   comments = COALESCE(CAST(:comments AS text), comments),
                   decided_by_user_id = :actor,
                   decided_at = :now,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND decision = 'pending' AND deleted_at IS NULL
            """;
    private static final String SOFT_DELETE = """
            UPDATE assessment.homework_approval
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE assessment.homework_approval
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
    private static final String EXISTS_HOMEWORK_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.homework WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM assessment.homework p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.homework_id IN (SELECT p0.id FROM assessment.homework p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public HomeworkApprovalRepository(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<HomeworkApprovalEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(HomeworkApprovalRepository::map).optional();
    }

    public Page<HomeworkApprovalEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<HomeworkApprovalEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(HomeworkApprovalRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), HomeworkApprovalEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public List<HomeworkApprovalEntity> findPending(UUID homeworkId) {
        return jdbc.sql(FIND_PENDING).param("homeworkId", homeworkId).query(HomeworkApprovalRepository::map).list();
    }

    /** Decides an open review; 0 rows means it changed (or was decided) since it was read. */
    public int decide(UUID id, int expected, String decision, String comments, UUID actor, OffsetDateTime now) {
        return jdbc.sql(DECIDE).param("id", id).param("expected", expected).param("decision", decision)
                .param("comments", comments).param("actor", actor).param("now", now).update();
    }

    public HomeworkApprovalEntity insert(HomeworkApprovalEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("homeworkId", e.getHomeworkId())
                .param("decision", e.getDecision())
                .param("comments", e.getComments())
                .param("decidedByUserId", e.getDecidedByUserId())
                .param("decidedAt", e.getDecidedAt())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(HomeworkApprovalRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(HomeworkApprovalEntity e, int expectedVersion) {
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

    public int softDelete(HomeworkApprovalEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced assessment.homework row exists (live). */
    public boolean existsHomeworkId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_HOMEWORK_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through homework_id (empty: a tenant-level row, or none). */
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

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.homework_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, HomeworkApprovalEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static HomeworkApprovalEntity map(ResultSet rs, int row) throws SQLException {
        return HomeworkApprovalEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .homeworkId(rs.getObject("homework_id", UUID.class))
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
