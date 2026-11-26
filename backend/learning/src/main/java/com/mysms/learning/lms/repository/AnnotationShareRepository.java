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
import com.mysms.learning.lms.domain.AnnotationShareEntity;
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
 * Plain-JDBC access to lms.annotation_share: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class AnnotationShareRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("annotationId", "r.annotation_id", ValueType.UUID),
            field("sharedWithUserId", "r.shared_with_user_id", ValueType.UUID),
            field("sharedAt", "r.shared_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<AnnotationShareEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", AnnotationShareEntity::getCreatedAt),
            Map.entry("updateTime", AnnotationShareEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.annotation_id, r.shared_with_user_id, r.shared_at, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.annotation_share r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.annotation_share
            (id, tenant_id, annotation_id, shared_with_user_id, shared_at, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :annotationId, :sharedWithUserId, COALESCE(:sharedAt, now()), :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, annotation_id, shared_with_user_id, shared_at, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.annotation_share
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.annotation_share
               SET shared_with_user_id = :sharedWithUserId,
                   shared_at = :sharedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_ANNOTATION_SHARE_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.annotation_share
                            WHERE tenant_id = :tenantId
                              AND annotation_id = :annotationId AND shared_with_user_id = :sharedWithUserId AND id <>
                                  :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_ANNOTATION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.annotation WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM lms.annotation p0 JOIN lms.cohort p1 ON p1.tenant_id = :tenantId AND p1.id =
                p0.cohort_id WHERE p0.tenant_id = :tenantId
                AND p0.id =
            :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.annotation_id IN (SELECT p0.id FROM lms.annotation p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.cohort_id IS NULL OR
             p0.cohort_id IN (SELECT p1.id FROM lms.cohort p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.institution_id = ANY(:scopeIds) OR
             p1.institution_id IS NULL)))))
            """;

    private final JdbcClient jdbc;

    public AnnotationShareRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AnnotationShareEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AnnotationShareRepository::map).optional();
    }

    public Page<AnnotationShareEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AnnotationShareEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(AnnotationShareRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), AnnotationShareEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public AnnotationShareEntity insert(AnnotationShareEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("annotationId", e.getAnnotationId())
                .param("sharedWithUserId", e.getSharedWithUserId())
                .param("sharedAt", e.getSharedAt())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(AnnotationShareRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(AnnotationShareEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("sharedWithUserId", e.getSharedWithUserId())
                .param("sharedAt", e.getSharedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(AnnotationShareEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_annotation_share_1: annotation_id, shared_with_user_id where (deleted_at IS NULL). */
    public boolean takenAnnotationIdSharedWithUserId1(AnnotationShareEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_ANNOTATION_SHARE_1_TAKEN)
                .param("annotationId", e.getAnnotationId())
                .param("sharedWithUserId", e.getSharedWithUserId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced lms.annotation row exists (live). */
    public boolean existsAnnotationId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ANNOTATION_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through annotation_id (empty: a tenant-level row, or none). */
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
        return new SqlFragment("r.annotation_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, AnnotationShareEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static AnnotationShareEntity map(ResultSet rs, int row) throws SQLException {
        return AnnotationShareEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .annotationId(rs.getObject("annotation_id", UUID.class))
                .sharedWithUserId(rs.getObject("shared_with_user_id", UUID.class))
                .sharedAt(rs.getObject("shared_at", OffsetDateTime.class))
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
