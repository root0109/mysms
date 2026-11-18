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
import com.mysms.lms.domain.ForumPostEntity;
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
 * Plain-JDBC access to lms.forum_post: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ForumPostRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("forumTopicId", "r.forum_topic_id", ValueType.UUID),
            field("parentPostId", "r.parent_post_id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("body", "r.body", ValueType.STRING),
            field("authorUserId", "r.author_user_id", ValueType.UUID),
            field("postedAt", "r.posted_at", ValueType.TIMESTAMP),
            field("isDraft", "r.is_draft", ValueType.BOOLEAN),
            field("isTeacherPost", "r.is_teacher_post", ValueType.BOOLEAN),
            field("substantiveFlag", "r.substantive_flag", ValueType.INTEGER),
            field("isHidden", "r.is_hidden", ValueType.BOOLEAN),
            field("isHiddenByParent", "r.is_hidden_by_parent", ValueType.BOOLEAN),
            field("isFlagged", "r.is_flagged", ValueType.BOOLEAN),
            field("showSignature", "r.show_signature", ValueType.BOOLEAN),
            field("usesInlineAuthoring", "r.uses_inline_authoring", ValueType.BOOLEAN),
            field("isAbusive", "r.is_abusive", ValueType.BOOLEAN),
            field("abuseReported", "r.abuse_reported", ValueType.BOOLEAN),
            field("abuseReportCount", "r.abuse_report_count", ValueType.INTEGER),
            field("abuseConfirmedBy", "r.abuse_confirmed_by", ValueType.UUID),
            field("abuseConfirmedAt", "r.abuse_confirmed_at", ValueType.TIMESTAMP),
            field("likeCount", "r.like_count", ValueType.INTEGER),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ForumPostEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ForumPostEntity::getCreatedAt),
            Map.entry("updateTime", ForumPostEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.forum_topic_id, r.parent_post_id, r.title, r.body, r.author_user_id,
                   r.posted_at, r.is_draft, r.is_teacher_post, r.substantive_flag, r.is_hidden, r.is_hidden_by_parent,
                   r.is_flagged, r.show_signature, r.uses_inline_authoring, r.is_abusive, r.abuse_reported,
                   r.abuse_report_count, r.abuse_confirmed_by, r.abuse_confirmed_at, r.like_count, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.forum_post r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.forum_post
            (id, tenant_id, forum_topic_id, parent_post_id, title, body, author_user_id, posted_at, is_draft,
             is_teacher_post, substantive_flag, is_hidden, is_hidden_by_parent, is_flagged, show_signature,
             uses_inline_authoring, is_abusive, abuse_reported, abuse_report_count, abuse_confirmed_by,
             abuse_confirmed_at, like_count, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :forumTopicId, :parentPostId, :title, :body, :authorUserId,
                    COALESCE(:postedAt, now()), COALESCE(:isDraft, false), COALESCE(:isTeacherPost, false),
                    :substantiveFlag, COALESCE(:isHidden, false), COALESCE(:isHiddenByParent, false),
                    COALESCE(:isFlagged, false), COALESCE(:showSignature, false),
                    COALESCE(:usesInlineAuthoring, false), COALESCE(:isAbusive, false),
                    COALESCE(:abuseReported, false), COALESCE(:abuseReportCount, 0), :abuseConfirmedBy,
                    :abuseConfirmedAt, COALESCE(:likeCount, 0), :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, forum_topic_id, parent_post_id, title, body, author_user_id, posted_at, is_draft,
                      is_teacher_post, substantive_flag, is_hidden, is_hidden_by_parent, is_flagged, show_signature,
                      uses_inline_authoring, is_abusive, abuse_reported, abuse_report_count, abuse_confirmed_by,
                      abuse_confirmed_at, like_count, created_at, created_by, updated_at, updated_by, deleted_at,
                      row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.forum_post
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.forum_post
               SET parent_post_id = :parentPostId,
                   title = :title,
                   body = :body,
                   author_user_id = :authorUserId,
                   posted_at = :postedAt,
                   is_draft = :isDraft,
                   is_teacher_post = :isTeacherPost,
                   substantive_flag = :substantiveFlag,
                   is_hidden = :isHidden,
                   is_hidden_by_parent = :isHiddenByParent,
                   is_flagged = :isFlagged,
                   show_signature = :showSignature,
                   uses_inline_authoring = :usesInlineAuthoring,
                   is_abusive = :isAbusive,
                   abuse_reported = :abuseReported,
                   abuse_report_count = :abuseReportCount,
                   abuse_confirmed_by = :abuseConfirmedBy,
                   abuse_confirmed_at = :abuseConfirmedAt,
                   like_count = :likeCount,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_FORUM_TOPIC_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_topic WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_PARENT_POST_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_post WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_FORUM_POST_PARENT_POST_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_post WHERE tenant_id = :tenantId
                AND parent_post_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_FORUM_POST_REPORT_FORUM_POST_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_post_report WHERE tenant_id = :tenantId
                AND forum_post_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_FORUM_POST_VOTE_FORUM_POST_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_post_vote WHERE tenant_id = :tenantId
                AND forum_post_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_FORUM_READ_STATE_FORUM_POST_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_read_state WHERE tenant_id = :tenantId
                AND forum_post_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_FORUM_TOPIC_LAST_POST_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_topic WHERE tenant_id = :tenantId
                AND last_post_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM lms.forum_topic p0 JOIN lms.discussion_prompt p1 ON p1.tenant_id = :tenantId
                AND p1.id =
            p0.discussion_prompt_id WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String COHORT_OF_TOPIC = """
            SELECT COALESCE(f.cohort_id, ci.cohort_id, ch.cohort_id) FROM lms.forum_topic t
              LEFT JOIN lms.forum_section s ON s.id = t.forum_section_id AND s.tenant_id = :tenantId
              LEFT JOIN lms.forum f ON f.id = s.forum_id AND f.tenant_id = :tenantId
              LEFT JOIN lms.content_item ci ON ci.id = t.content_item_id AND ci.tenant_id = :tenantId
              LEFT JOIN lms.chapter ch ON ch.id = ci.chapter_id AND ch.tenant_id = :tenantId
             WHERE t.tenant_id = :tenantId AND t.id = :id AND t.deleted_at IS NULL
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

    public ForumPostRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ForumPostEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ForumPostRepository::map).optional();
    }

    public Page<ForumPostEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ForumPostEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ForumPostRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ForumPostEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ForumPostEntity insert(ForumPostEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("forumTopicId", e.getForumTopicId())
                .param("parentPostId", e.getParentPostId())
                .param("title", e.getTitle())
                .param("body", e.getBody())
                .param("authorUserId", e.getAuthorUserId())
                .param("postedAt", e.getPostedAt())
                .param("isDraft", e.getIsDraft())
                .param("isTeacherPost", e.getIsTeacherPost())
                .param("substantiveFlag", e.getSubstantiveFlag())
                .param("isHidden", e.getIsHidden())
                .param("isHiddenByParent", e.getIsHiddenByParent())
                .param("isFlagged", e.getIsFlagged())
                .param("showSignature", e.getShowSignature())
                .param("usesInlineAuthoring", e.getUsesInlineAuthoring())
                .param("isAbusive", e.getIsAbusive())
                .param("abuseReported", e.getAbuseReported())
                .param("abuseReportCount", e.getAbuseReportCount())
                .param("abuseConfirmedBy", e.getAbuseConfirmedBy())
                .param("abuseConfirmedAt", e.getAbuseConfirmedAt())
                .param("likeCount", e.getLikeCount())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ForumPostRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ForumPostEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("parentPostId", e.getParentPostId())
                .param("title", e.getTitle())
                .param("body", e.getBody())
                .param("authorUserId", e.getAuthorUserId())
                .param("postedAt", e.getPostedAt())
                .param("isDraft", e.getIsDraft())
                .param("isTeacherPost", e.getIsTeacherPost())
                .param("substantiveFlag", e.getSubstantiveFlag())
                .param("isHidden", e.getIsHidden())
                .param("isHiddenByParent", e.getIsHiddenByParent())
                .param("isFlagged", e.getIsFlagged())
                .param("showSignature", e.getShowSignature())
                .param("usesInlineAuthoring", e.getUsesInlineAuthoring())
                .param("isAbusive", e.getIsAbusive())
                .param("abuseReported", e.getAbuseReported())
                .param("abuseReportCount", e.getAbuseReportCount())
                .param("abuseConfirmedBy", e.getAbuseConfirmedBy())
                .param("abuseConfirmedAt", e.getAbuseConfirmedAt())
                .param("likeCount", e.getLikeCount())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ForumPostEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.forum_topic row exists (live). */
    public boolean existsForumTopicId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_FORUM_TOPIC_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.forum_post row exists (live). */
    public boolean existsParentPostId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PARENT_POST_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.forum_post rows still reference the row. */
    public boolean referencedByForumPostParentPostId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_FORUM_POST_PARENT_POST_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.forum_post_report rows still reference the row. */
    public boolean referencedByForumPostReportForumPostId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_FORUM_POST_REPORT_FORUM_POST_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.forum_post_vote rows still reference the row. */
    public boolean referencedByForumPostVoteForumPostId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_FORUM_POST_VOTE_FORUM_POST_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.forum_read_state rows still reference the row. */
    public boolean referencedByForumReadStateForumPostId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_FORUM_READ_STATE_FORUM_POST_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.forum_topic rows still reference the row. */
    public boolean referencedByForumTopicLastPostId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_FORUM_TOPIC_LAST_POST_ID).param("id", id)
                .query(Boolean.class).single());
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

    /** The cohort a topic is discussed in: its forum's, else its content item's (empty: none). */
    public Optional<UUID> cohortOfTopic(UUID topicId) {
        return jdbc.sql(COHORT_OF_TOPIC).param("id", topicId)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class))).optional()
                .flatMap(found -> found);
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.forum_topic_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, ForumPostEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ForumPostEntity map(ResultSet rs, int row) throws SQLException {
        return ForumPostEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .forumTopicId(rs.getObject("forum_topic_id", UUID.class))
                .parentPostId(rs.getObject("parent_post_id", UUID.class))
                .title(rs.getString("title"))
                .body(rs.getString("body"))
                .authorUserId(rs.getObject("author_user_id", UUID.class))
                .postedAt(rs.getObject("posted_at", OffsetDateTime.class))
                .isDraft(rs.getObject("is_draft", Boolean.class))
                .isTeacherPost(rs.getObject("is_teacher_post", Boolean.class))
                .substantiveFlag(rs.getObject("substantive_flag", Integer.class))
                .isHidden(rs.getObject("is_hidden", Boolean.class))
                .isHiddenByParent(rs.getObject("is_hidden_by_parent", Boolean.class))
                .isFlagged(rs.getObject("is_flagged", Boolean.class))
                .showSignature(rs.getObject("show_signature", Boolean.class))
                .usesInlineAuthoring(rs.getObject("uses_inline_authoring", Boolean.class))
                .isAbusive(rs.getObject("is_abusive", Boolean.class))
                .abuseReported(rs.getObject("abuse_reported", Boolean.class))
                .abuseReportCount(rs.getObject("abuse_report_count", Integer.class))
                .abuseConfirmedBy(rs.getObject("abuse_confirmed_by", UUID.class))
                .abuseConfirmedAt(rs.getObject("abuse_confirmed_at", OffsetDateTime.class))
                .likeCount(rs.getObject("like_count", Integer.class))
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
