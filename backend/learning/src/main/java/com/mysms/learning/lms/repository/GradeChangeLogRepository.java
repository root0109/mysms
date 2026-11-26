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
import com.mysms.learning.lms.domain.GradeChangeLogEntity;
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
 * Plain-JDBC access to lms.grade_change_log: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class GradeChangeLogRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("gradebookEntryId", "r.gradebook_entry_id", ValueType.UUID),
            field("points", "r.points", ValueType.DECIMAL),
            field("autoPoints", "r.auto_points", ValueType.DECIMAL),
            field("manualPoints", "r.manual_points", ValueType.DECIMAL),
            field("comment", "r.comment", ValueType.STRING),
            field("wasPublished", "r.was_published", ValueType.BOOLEAN),
            field("publishedAt", "r.published_at", ValueType.TIMESTAMP),
            field("changedBy", "r.changed_by", ValueType.UUID),
            field("changedAt", "r.changed_at", ValueType.TIMESTAMP),
            field("studentUserId", "r.student_user_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<GradeChangeLogEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", GradeChangeLogEntity::getCreatedAt),
            Map.entry("updateTime", GradeChangeLogEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.gradebook_entry_id, r.points, r.auto_points, r.manual_points, r.comment,
                   r.was_published, r.published_at, r.changed_by, r.changed_at, r.student_user_id, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.grade_change_log r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.grade_change_log
            (id, tenant_id, gradebook_entry_id, points, auto_points, manual_points, comment, was_published,
             published_at, changed_by, changed_at, student_user_id, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :gradebookEntryId, :points, :autoPoints, :manualPoints, :comment, :wasPublished,
                    :publishedAt, :changedBy, :changedAt, :studentUserId, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, gradebook_entry_id, points, auto_points, manual_points, comment, was_published,
                      published_at, changed_by, changed_at, student_user_id, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.grade_change_log
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.grade_change_log
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_GRADEBOOK_ENTRY_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.gradebook_entry WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM lms.gradebook_entry p0 JOIN lms.cohort p1 ON p1.tenant_id = :tenantId AND
                p1.id = p0.cohort_id WHERE p0.tenant_id = :tenantId
                AND p0.id
            = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.gradebook_entry_id IN (SELECT p0.id FROM lms.gradebook_entry p0 WHERE p0.tenant_id =
                 :tenantId
                 AND (p0.cohort_id IN
             (SELECT p1.id FROM lms.cohort p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.institution_id = ANY(:scopeIds) OR p1.institution_id IS
             NULL)))))
            """;

    private final JdbcClient jdbc;

    public GradeChangeLogRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<GradeChangeLogEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(GradeChangeLogRepository::map).optional();
    }

    public Page<GradeChangeLogEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<GradeChangeLogEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(GradeChangeLogRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), GradeChangeLogEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public GradeChangeLogEntity insert(GradeChangeLogEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("gradebookEntryId", e.getGradebookEntryId())
                .param("points", e.getPoints())
                .param("autoPoints", e.getAutoPoints())
                .param("manualPoints", e.getManualPoints())
                .param("comment", e.getComment())
                .param("wasPublished", e.getWasPublished())
                .param("publishedAt", e.getPublishedAt())
                .param("changedBy", e.getChangedBy())
                .param("changedAt", e.getChangedAt())
                .param("studentUserId", e.getStudentUserId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(GradeChangeLogRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(GradeChangeLogEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(GradeChangeLogEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.gradebook_entry row exists (live). */
    public boolean existsGradebookEntryId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_GRADEBOOK_ENTRY_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through gradebook_entry_id (empty: a tenant-level row, or none). */
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

    private static List<Object> sortValues(SortSpec sort, GradeChangeLogEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static GradeChangeLogEntity map(ResultSet rs, int row) throws SQLException {
        return GradeChangeLogEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .gradebookEntryId(rs.getObject("gradebook_entry_id", UUID.class))
                .points(rs.getBigDecimal("points"))
                .autoPoints(rs.getBigDecimal("auto_points"))
                .manualPoints(rs.getBigDecimal("manual_points"))
                .comment(rs.getString("comment"))
                .wasPublished(rs.getObject("was_published", Boolean.class))
                .publishedAt(rs.getObject("published_at", OffsetDateTime.class))
                .changedBy(rs.getObject("changed_by", UUID.class))
                .changedAt(rs.getObject("changed_at", OffsetDateTime.class))
                .studentUserId(rs.getObject("student_user_id", UUID.class))
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
