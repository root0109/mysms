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
import com.mysms.lms.domain.ForumPostVoteEntity;
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
 * Plain-JDBC access to lms.forum_post_vote: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ForumPostVoteRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("forumPostId", "r.forum_post_id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("isLike", "r.is_like", ValueType.BOOLEAN),
            field("votedAt", "r.voted_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ForumPostVoteEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ForumPostVoteEntity::getCreatedAt),
            Map.entry("updateTime", ForumPostVoteEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.forum_post_id, r.user_id, r.is_like, r.voted_at, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.forum_post_vote r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.forum_post_vote
            (id, tenant_id, forum_post_id, user_id, is_like, voted_at, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :forumPostId, :userId, :isLike, COALESCE(:votedAt, now()), :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, forum_post_id, user_id, is_like, voted_at, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.forum_post_vote
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.forum_post_vote
               SET forum_post_id = :forumPostId,
                   user_id = :userId,
                   is_like = :isLike,
                   voted_at = :votedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_FORUM_POST_VOTE_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_post_vote
                            WHERE tenant_id = :tenantId
                              AND forum_post_id = :forumPostId AND user_id = :userId AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String EXISTS_FORUM_POST_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_post WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p2.institution_id FROM lms.forum_post p0 JOIN lms.forum_topic p1 ON p1.tenant_id = :tenantId AND
                p1.id = p0.forum_topic_id JOIN
            lms.discussion_prompt p2 ON p2.tenant_id = :tenantId AND p2.id = p1.discussion_prompt_id WHERE p0.tenant_id
                = :tenantId
                AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.forum_post_id IN (SELECT p0.id FROM lms.forum_post p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.forum_topic_id IN (SELECT
             p1.id FROM lms.forum_topic p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.discussion_prompt_id IS NULL OR p1.discussion_prompt_id IN (SELECT
             p2.id FROM lms.discussion_prompt p2 WHERE p2.tenant_id = :tenantId
                 AND (p2.institution_id = ANY(:scopeIds) OR p2.institution_id IS
             NULL)))))))
            """;

    private final JdbcClient jdbc;

    public ForumPostVoteRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ForumPostVoteEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ForumPostVoteRepository::map).optional();
    }

    public Page<ForumPostVoteEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ForumPostVoteEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ForumPostVoteRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ForumPostVoteEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ForumPostVoteEntity insert(ForumPostVoteEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("forumPostId", e.getForumPostId())
                .param("userId", e.getUserId())
                .param("isLike", e.getIsLike())
                .param("votedAt", e.getVotedAt())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ForumPostVoteRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ForumPostVoteEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("forumPostId", e.getForumPostId())
                .param("userId", e.getUserId())
                .param("isLike", e.getIsLike())
                .param("votedAt", e.getVotedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ForumPostVoteEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_forum_post_vote_1: forum_post_id, user_id where (deleted_at IS NULL). */
    public boolean takenForumPostIdUserId1(ForumPostVoteEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_FORUM_POST_VOTE_1_TAKEN)
                .param("forumPostId", e.getForumPostId())
                .param("userId", e.getUserId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced lms.forum_post row exists (live). */
    public boolean existsForumPostId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_FORUM_POST_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through forum_post_id (empty: a tenant-level row, or none). */
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

    private static List<Object> sortValues(SortSpec sort, ForumPostVoteEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ForumPostVoteEntity map(ResultSet rs, int row) throws SQLException {
        return ForumPostVoteEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .forumPostId(rs.getObject("forum_post_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .isLike(rs.getObject("is_like", Boolean.class))
                .votedAt(rs.getObject("voted_at", OffsetDateTime.class))
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
