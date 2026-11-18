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
import com.mysms.lms.domain.ForumReadStateEntity;
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
 * Plain-JDBC access to lms.forum_read_state: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ForumReadStateRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("forumTopicId", "r.forum_topic_id", ValueType.UUID),
            field("forumPostId", "r.forum_post_id", ValueType.UUID),
            field("readAt", "r.read_at", ValueType.TIMESTAMP),
            field("isFlagged", "r.is_flagged", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ForumReadStateEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ForumReadStateEntity::getCreatedAt),
            Map.entry("updateTime", ForumReadStateEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.user_id, r.forum_topic_id, r.forum_post_id, r.read_at, r.is_flagged,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.forum_read_state r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.forum_read_state
            (id, tenant_id, user_id, forum_topic_id, forum_post_id, read_at, is_flagged, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :userId, :forumTopicId, :forumPostId, COALESCE(:readAt, now()),
                    COALESCE(:isFlagged, false), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, user_id, forum_topic_id, forum_post_id, read_at, is_flagged, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.forum_read_state
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.forum_read_state
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_FORUM_POST_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_post WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_FORUM_TOPIC_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_topic WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM lms.forum_topic p0 JOIN lms.discussion_prompt p1 ON p1.tenant_id = :tenantId
                AND p1.id =
            p0.discussion_prompt_id WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.forum_topic_id IN (SELECT p0.id FROM lms.forum_topic p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.discussion_prompt_id IS
             NULL OR p0.discussion_prompt_id IN (SELECT p1.id FROM lms.discussion_prompt p1 WHERE p1.tenant_id =
                 :tenantId
                 AND (p1.institution_id =
             ANY(:scopeIds) OR p1.institution_id IS NULL)))))
            """;

    private final JdbcClient jdbc;

    public ForumReadStateRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ForumReadStateEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ForumReadStateRepository::map).optional();
    }

    public Page<ForumReadStateEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ForumReadStateEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ForumReadStateRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ForumReadStateEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ForumReadStateEntity insert(ForumReadStateEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("userId", e.getUserId())
                .param("forumTopicId", e.getForumTopicId())
                .param("forumPostId", e.getForumPostId())
                .param("readAt", e.getReadAt())
                .param("isFlagged", e.getIsFlagged())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ForumReadStateRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ForumReadStateEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ForumReadStateEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.forum_post row exists (live). */
    public boolean existsForumPostId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_FORUM_POST_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.forum_topic row exists (live). */
    public boolean existsForumTopicId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_FORUM_TOPIC_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through forum_topic_id (empty: a tenant-level row, or none). */
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
        return new SqlFragment("r.forum_topic_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, ForumReadStateEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ForumReadStateEntity map(ResultSet rs, int row) throws SQLException {
        return ForumReadStateEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .forumTopicId(rs.getObject("forum_topic_id", UUID.class))
                .forumPostId(rs.getObject("forum_post_id", UUID.class))
                .readAt(rs.getObject("read_at", OffsetDateTime.class))
                .isFlagged(rs.getObject("is_flagged", Boolean.class))
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
