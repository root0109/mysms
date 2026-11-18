package com.mysms.comms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.domain.PostCommentEntity;
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
 * Plain-JDBC access to comms.post_comment: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class PostCommentRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("postId", "r.post_id", ValueType.UUID),
            field("parentCommentId", "r.parent_comment_id", ValueType.UUID),
            field("authorUserId", "r.author_user_id", ValueType.UUID),
            field("body", "r.body", ValueType.STRING),
            field("attachmentFileId", "r.attachment_file_id", ValueType.UUID),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<PostCommentEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PostCommentEntity::getCreatedAt),
            Map.entry("updateTime", PostCommentEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.post_id, r.parent_comment_id, r.author_user_id, r.body, r.attachment_file_id,
                   r.is_active, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM comms.post_comment r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO comms.post_comment
            (id, tenant_id, post_id, parent_comment_id, author_user_id, body, attachment_file_id, is_active,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :postId, :parentCommentId, :authorUserId, :body, :attachmentFileId,
                    COALESCE(:isActive, true), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, post_id, parent_comment_id, author_user_id, body, attachment_file_id, is_active,
                      created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE comms.post_comment
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE comms.post_comment
               SET parent_comment_id = :parentCommentId,
                   author_user_id = :authorUserId,
                   body = :body,
                   attachment_file_id = :attachmentFileId,
                   is_active = :isActive,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_PARENT_COMMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.post_comment WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_POST_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.post WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_POST_COMMENT_PARENT_COMMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.post_comment WHERE tenant_id = :tenantId
                AND parent_comment_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_REACTION_COMMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.reaction WHERE tenant_id = :tenantId
                AND comment_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM comms.post p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.post_id IN (SELECT p0.id FROM comms.post p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public PostCommentRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PostCommentEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PostCommentRepository::map).optional();
    }

    public Page<PostCommentEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PostCommentEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PostCommentRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PostCommentEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public PostCommentEntity insert(PostCommentEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("postId", e.getPostId())
                .param("parentCommentId", e.getParentCommentId())
                .param("authorUserId", e.getAuthorUserId())
                .param("body", e.getBody())
                .param("attachmentFileId", e.getAttachmentFileId())
                .param("isActive", e.getIsActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(PostCommentRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(PostCommentEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("parentCommentId", e.getParentCommentId())
                .param("authorUserId", e.getAuthorUserId())
                .param("body", e.getBody())
                .param("attachmentFileId", e.getAttachmentFileId())
                .param("isActive", e.getIsActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(PostCommentEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced comms.post_comment row exists (live). */
    public boolean existsParentCommentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PARENT_COMMENT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced comms.post row exists (live). */
    public boolean existsPostId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_POST_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live comms.post_comment rows still reference the row. */
    public boolean referencedByPostCommentParentCommentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_POST_COMMENT_PARENT_COMMENT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live comms.reaction rows still reference the row. */
    public boolean referencedByReactionCommentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_REACTION_COMMENT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** The institution owning the row reached through post_id (empty: a tenant-level row, or none). */
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
        return new SqlFragment("r.post_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, PostCommentEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PostCommentEntity map(ResultSet rs, int row) throws SQLException {
        return PostCommentEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .postId(rs.getObject("post_id", UUID.class))
                .parentCommentId(rs.getObject("parent_comment_id", UUID.class))
                .authorUserId(rs.getObject("author_user_id", UUID.class))
                .body(rs.getString("body"))
                .attachmentFileId(rs.getObject("attachment_file_id", UUID.class))
                .isActive(rs.getObject("is_active", Boolean.class))
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
