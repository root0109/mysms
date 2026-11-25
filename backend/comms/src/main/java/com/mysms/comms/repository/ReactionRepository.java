package com.mysms.comms.repository;

import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.domain.ReactionEntity;
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
 * Plain-JDBC access to comms.reaction: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Lists are always narrowed to one user's own reactions.
 */
@Repository
public class ReactionRepository {

    /** Fields clients may filter a post's reactions on. */
    public static final Map<String, FieldSpec> POST_FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("reaction", "r.reaction", ValueType.STRING),
            field("isSaved", "r.is_saved", ValueType.BOOLEAN),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may filter comment reactions on. */
    public static final Map<String, FieldSpec> COMMENT_FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("commentId", "r.comment_id", ValueType.UUID),
            field("reaction", "r.reaction", ValueType.STRING),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ReactionEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ReactionEntity::getCreatedAt),
            Map.entry("updateTime", ReactionEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.post_id, r.comment_id, r.user_id, r.reaction, r.is_saved, r.is_active,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM comms.reaction r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String LIST_OWN = SELECT_LIVE + " AND r.user_id = :owner";
    private static final String INSERT = """
            INSERT INTO comms.reaction
            (id, tenant_id, post_id, comment_id, user_id, reaction, is_saved, is_active, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :postId, :commentId, :userId, :reaction, COALESCE(:isSaved, false),
                    COALESCE(:isActive, true), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, post_id, comment_id, user_id, reaction, is_saved, is_active, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String UPDATE = """
            UPDATE comms.reaction
               SET reaction = :reaction,
                   is_saved = :isSaved,
                   is_active = :isActive,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SOFT_DELETE = """
            UPDATE comms.reaction
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String POST_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM comms.reaction
                            WHERE tenant_id = :tenantId
                              AND post_id = :postId AND user_id = :userId AND deleted_at IS NULL)
            """;
    private static final String COMMENT_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM comms.reaction
                            WHERE tenant_id = :tenantId
                              AND comment_id = :commentId AND user_id = :userId AND deleted_at IS NULL)
            """;
    private static final String POST_OF_COMMENT = """
            SELECT c.post_id FROM comms.post_comment c
             WHERE c.tenant_id = :tenantId
               AND c.id = :commentId AND c.is_active AND c.deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public ReactionRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ReactionEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ReactionRepository::map).optional();
    }

    /** One user's own reactions, filtered and paged. */
    public Page<ReactionEntity> listOwn(UUID owner, PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = LIST_OWN + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ReactionEntity> rows = jdbc.sql(sql).param("owner", owner).params(where.params())
                .params(page.limitParams()).query(ReactionRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ReactionEntity::getId);
    }

    /** Inserts the row and returns it as stored. */
    public ReactionEntity insert(ReactionEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("postId", e.getPostId())
                .param("commentId", e.getCommentId())
                .param("userId", e.getUserId())
                .param("reaction", e.getReaction())
                .param("isSaved", e.getIsSaved())
                .param("isActive", e.getIsActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ReactionRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ReactionEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("reaction", e.getReaction())
                .param("isSaved", e.getIsSaved())
                .param("isActive", e.getIsActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ReactionEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the user already has a live reaction to the post (uq_reaction_post). */
    public boolean postTaken(UUID postId, UUID userId) {
        return Boolean.TRUE.equals(jdbc.sql(POST_TAKEN).param("postId", postId).param("userId", userId)
                .query(Boolean.class).single());
    }

    /** Whether the user already has a live reaction to the comment (uq_reaction_comment). */
    public boolean commentTaken(UUID commentId, UUID userId) {
        return Boolean.TRUE.equals(jdbc.sql(COMMENT_TAKEN).param("commentId", commentId).param("userId", userId)
                .query(Boolean.class).single());
    }

    /** The post a live, visible comment belongs to. */
    public Optional<UUID> postOfComment(UUID commentId) {
        return jdbc.sql(POST_OF_COMMENT).param("commentId", commentId).query(UUID.class).optional();
    }

    /** Reactions to this one post only. */
    public static SqlFragment ofPost(UUID postId) {
        return new SqlFragment("r.post_id = :postId", Map.of("postId", postId));
    }

    /** Reactions to comments only. */
    public static SqlFragment onComments() {
        return new SqlFragment("r.comment_id IS NOT NULL", Map.of());
    }

    private static List<Object> sortValues(SortSpec sort, ReactionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ReactionEntity map(ResultSet rs, int row) throws SQLException {
        return ReactionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .postId(rs.getObject("post_id", UUID.class))
                .commentId(rs.getObject("comment_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .reaction(rs.getString("reaction"))
                .isSaved(rs.getBoolean("is_saved"))
                .isActive(rs.getBoolean("is_active"))
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
