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
import com.mysms.lms.domain.ReleaseGrantEntity;
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
 * Plain-JDBC access to lms.release_grant: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ReleaseGrantRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("contentItemId", "r.content_item_id", ValueType.UUID),
            field("chapterId", "r.chapter_id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("isReleased", "r.is_released", ValueType.BOOLEAN),
            field("accessGranted", "r.access_granted", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ReleaseGrantEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ReleaseGrantEntity::getCreatedAt),
            Map.entry("updateTime", ReleaseGrantEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.content_item_id, r.chapter_id, r.user_id, r.is_released, r.access_granted,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.release_grant r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    /** A student's release record of an item. */
    private static final String FIND_OWN = SELECT_LIVE + " AND r.content_item_id = :itemId AND r.user_id = :userId";
    /**
     * Grants (or takes back) manual access to an item for a student: one live row per item and student
     * (uq_release_grant_1 decides between concurrent first calls); the release computed by the rules is kept.
     */
    private static final String GRANT = """
            INSERT INTO lms.release_grant
            (id, tenant_id, content_item_id, chapter_id, user_id, is_released, access_granted, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :itemId, :chapterId, :userId, false, :granted, :now, :actor, :now, :actor, 1)
            ON CONFLICT (content_item_id, user_id) WHERE deleted_at IS NULL
            DO UPDATE SET access_granted = EXCLUDED.access_granted, updated_at = :now, updated_by = :actor,
                          row_version = lms.release_grant.row_version + 1
             WHERE lms.release_grant.tenant_id = :tenantId
            """;
    private static final String INSERT = """
            INSERT INTO lms.release_grant
            (id, tenant_id, content_item_id, chapter_id, user_id, is_released, access_granted, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :contentItemId, :chapterId, :userId, COALESCE(:isReleased, false),
                    COALESCE(:accessGranted, false), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, content_item_id, chapter_id, user_id, is_released, access_granted, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.release_grant
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.release_grant
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_RELEASE_GRANT_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.release_grant
                            WHERE tenant_id = :tenantId
                              AND content_item_id = :contentItemId AND user_id = :userId AND id <> :id AND ((deleted_at
                                  IS NULL)))
            """;
    private static final String EXISTS_CHAPTER_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.chapter WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM lms.content_item p0 JOIN lms.course p1 ON p1.tenant_id = :tenantId AND p1.id =
                p0.course_id WHERE p0.tenant_id = :tenantId
                AND p0.id =
            :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.content_item_id IN (SELECT p0.id FROM lms.content_item p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.course_id IS NULL OR
             p0.course_id IN (SELECT p1.id FROM lms.course p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.institution_id = ANY(:scopeIds) OR
             p1.institution_id IS NULL)))))
            """;

    private final JdbcClient jdbc;

    public ReleaseGrantRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ReleaseGrantEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ReleaseGrantRepository::map).optional();
    }

    public Page<ReleaseGrantEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ReleaseGrantEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ReleaseGrantRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ReleaseGrantEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    /** The student's release record of the item. */
    public Optional<ReleaseGrantEntity> findOwn(UUID itemId, UUID userId) {
        return jdbc.sql(FIND_OWN).param("itemId", itemId).param("userId", userId)
                .query(ReleaseGrantRepository::map).optional();
    }

    /** Grants or takes back manual access (see {@link #GRANT}). */
    public void grant(UUID id, UUID tenantId, UUID itemId, UUID chapterId, UUID userId, boolean granted,
            OffsetDateTime now, UUID actor) {
        jdbc.sql(GRANT).param("id", id).param("tenantId", tenantId).param("itemId", itemId)
                .param("chapterId", chapterId).param("userId", userId).param("granted", granted).param("now", now)
                .param("actor", actor).update();
    }

    public ReleaseGrantEntity insert(ReleaseGrantEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("contentItemId", e.getContentItemId())
                .param("chapterId", e.getChapterId())
                .param("userId", e.getUserId())
                .param("isReleased", e.getIsReleased())
                .param("accessGranted", e.getAccessGranted())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ReleaseGrantRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ReleaseGrantEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ReleaseGrantEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_release_grant_1: content_item_id, user_id where (deleted_at IS NULL). */
    public boolean takenContentItemIdUserId1(ReleaseGrantEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_RELEASE_GRANT_1_TAKEN)
                .param("contentItemId", e.getContentItemId())
                .param("userId", e.getUserId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced lms.chapter row exists (live). */
    public boolean existsChapterId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CHAPTER_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.content_item row exists (live). */
    public boolean existsContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CONTENT_ITEM_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through content_item_id (empty: a tenant-level row, or none). */
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
        return new SqlFragment("r.content_item_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, ReleaseGrantEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ReleaseGrantEntity map(ResultSet rs, int row) throws SQLException {
        return ReleaseGrantEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .contentItemId(rs.getObject("content_item_id", UUID.class))
                .chapterId(rs.getObject("chapter_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .isReleased(rs.getObject("is_released", Boolean.class))
                .accessGranted(rs.getObject("access_granted", Boolean.class))
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
