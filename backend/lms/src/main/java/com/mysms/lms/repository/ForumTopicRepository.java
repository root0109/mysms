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
import com.mysms.lms.domain.ForumTopicEntity;
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
 * Plain-JDBC access to lms.forum_topic: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ForumTopicRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("forumSectionId", "r.forum_section_id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("description", "r.description", ValueType.STRING),
            field("topicType", "r.topic_type", ValueType.STRING),
            field("opensAt", "r.opens_at", ValueType.TIMESTAMP),
            field("closesAt", "r.closes_at", ValueType.TIMESTAMP),
            field("contentItemId", "r.content_item_id", ValueType.UUID),
            field("discussionPromptId", "r.discussion_prompt_id", ValueType.UUID),
            field("linkedEntityType", "r.linked_entity_type", ValueType.STRING),
            field("linkedEntityId", "r.linked_entity_id", ValueType.UUID),
            field("individualUserId", "r.individual_user_id", ValueType.UUID),
            field("isGraded", "r.is_graded", ValueType.BOOLEAN),
            field("pointsPossible", "r.points_possible", ValueType.DECIMAL),
            field("gradeWeightPercent", "r.grade_weight_percent", ValueType.DECIMAL),
            field("gradingDueAt", "r.grading_due_at", ValueType.TIMESTAMP),
            field("isGradingCompleted", "r.is_grading_completed", ValueType.BOOLEAN),
            field("isHidden", "r.is_hidden", ValueType.BOOLEAN),
            field("isTeacherTopic", "r.is_teacher_topic", ValueType.BOOLEAN),
            field("isSystemGenerated", "r.is_system_generated", ValueType.BOOLEAN),
            field("lastPostId", "r.last_post_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ForumTopicEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ForumTopicEntity::getCreatedAt),
            Map.entry("updateTime", ForumTopicEntity::getUpdatedAt),
            Map.entry("title", ForumTopicEntity::getTitle));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.forum_section_id, r.title, r.description, r.topic_type, r.opens_at,
                   r.closes_at, r.content_item_id, r.discussion_prompt_id, r.linked_entity_type, r.linked_entity_id,
                   r.individual_user_id, r.is_graded, r.points_possible, r.grade_weight_percent, r.grading_due_at,
                   r.is_grading_completed, r.is_hidden, r.is_teacher_topic, r.is_system_generated, r.last_post_id,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.forum_topic r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.forum_topic
            (id, tenant_id, forum_section_id, title, description, topic_type, opens_at, closes_at, content_item_id,
             discussion_prompt_id, linked_entity_type, linked_entity_id, individual_user_id, is_graded,
             points_possible, grade_weight_percent, grading_due_at, is_grading_completed, is_hidden,
             is_teacher_topic, is_system_generated, last_post_id, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :forumSectionId, :title, :description,
                    COALESCE(:topicType, 'discussion'::character varying), :opensAt, :closesAt, :contentItemId,
                    :discussionPromptId, :linkedEntityType, :linkedEntityId, :individualUserId,
                    COALESCE(:isGraded, false), :pointsPossible, :gradeWeightPercent, :gradingDueAt,
                    COALESCE(:isGradingCompleted, false), COALESCE(:isHidden, false),
                    COALESCE(:isTeacherTopic, false), COALESCE(:isSystemGenerated, false), :lastPostId, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, forum_section_id, title, description, topic_type, opens_at, closes_at,
                      content_item_id, discussion_prompt_id, linked_entity_type, linked_entity_id, individual_user_id,
                      is_graded, points_possible, grade_weight_percent, grading_due_at, is_grading_completed, is_hidden,
                      is_teacher_topic, is_system_generated, last_post_id, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.forum_topic
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.forum_topic
               SET forum_section_id = :forumSectionId,
                   title = :title,
                   description = :description,
                   topic_type = :topicType,
                   opens_at = :opensAt,
                   closes_at = :closesAt,
                   content_item_id = :contentItemId,
                   discussion_prompt_id = :discussionPromptId,
                   linked_entity_type = :linkedEntityType,
                   linked_entity_id = :linkedEntityId,
                   individual_user_id = :individualUserId,
                   is_graded = :isGraded,
                   points_possible = :pointsPossible,
                   grade_weight_percent = :gradeWeightPercent,
                   grading_due_at = :gradingDueAt,
                   is_grading_completed = :isGradingCompleted,
                   is_hidden = :isHidden,
                   is_teacher_topic = :isTeacherTopic,
                   is_system_generated = :isSystemGenerated,
                   last_post_id = :lastPostId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_DISCUSSION_PROMPT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.discussion_prompt WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_FORUM_SECTION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_section WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_LAST_POST_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_post WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ANNOTATION_FORUM_TOPIC_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.annotation WHERE tenant_id = :tenantId
                AND forum_topic_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ASSIGNMENT_GROUP_DISCUSSION_TOPIC_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment_group WHERE tenant_id = :tenantId
                AND discussion_topic_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_FORUM_POST_FORUM_TOPIC_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_post WHERE tenant_id = :tenantId
                AND forum_topic_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_FORUM_READ_STATE_FORUM_TOPIC_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_read_state WHERE tenant_id = :tenantId
                AND forum_topic_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_GRADEBOOK_ENTRY_FORUM_TOPIC_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.gradebook_entry WHERE tenant_id = :tenantId
                AND forum_topic_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.discussion_prompt p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;

    /** lms.HideForumTopic: allowed while is_hidden is false. */
    private static final String TRANSITION_HIDE_FORUM_TOPIC = """
            UPDATE lms.forum_topic
               SET is_hidden = true,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** lms.UnhideForumTopic: allowed while is_hidden is true. */
    private static final String TRANSITION_UNHIDE_FORUM_TOPIC = """
            UPDATE lms.forum_topic
               SET is_hidden = false,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.discussion_prompt_id IN (SELECT p0.id FROM lms.discussion_prompt p0 WHERE p0.tenant_id =
                 :tenantId
                 AND
             (p0.institution_id = ANY(:scopeIds) OR p0.institution_id IS NULL)) OR r.discussion_prompt_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public ForumTopicRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ForumTopicEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ForumTopicRepository::map).optional();
    }

    public Page<ForumTopicEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ForumTopicEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ForumTopicRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ForumTopicEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ForumTopicEntity insert(ForumTopicEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("forumSectionId", e.getForumSectionId())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("topicType", e.getTopicType())
                .param("opensAt", e.getOpensAt())
                .param("closesAt", e.getClosesAt())
                .param("contentItemId", e.getContentItemId())
                .param("discussionPromptId", e.getDiscussionPromptId())
                .param("linkedEntityType", e.getLinkedEntityType())
                .param("linkedEntityId", e.getLinkedEntityId())
                .param("individualUserId", e.getIndividualUserId())
                .param("isGraded", e.getIsGraded())
                .param("pointsPossible", e.getPointsPossible())
                .param("gradeWeightPercent", e.getGradeWeightPercent())
                .param("gradingDueAt", e.getGradingDueAt())
                .param("isGradingCompleted", e.getIsGradingCompleted())
                .param("isHidden", e.getIsHidden())
                .param("isTeacherTopic", e.getIsTeacherTopic())
                .param("isSystemGenerated", e.getIsSystemGenerated())
                .param("lastPostId", e.getLastPostId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ForumTopicRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ForumTopicEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("forumSectionId", e.getForumSectionId())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("topicType", e.getTopicType())
                .param("opensAt", e.getOpensAt())
                .param("closesAt", e.getClosesAt())
                .param("contentItemId", e.getContentItemId())
                .param("discussionPromptId", e.getDiscussionPromptId())
                .param("linkedEntityType", e.getLinkedEntityType())
                .param("linkedEntityId", e.getLinkedEntityId())
                .param("individualUserId", e.getIndividualUserId())
                .param("isGraded", e.getIsGraded())
                .param("pointsPossible", e.getPointsPossible())
                .param("gradeWeightPercent", e.getGradeWeightPercent())
                .param("gradingDueAt", e.getGradingDueAt())
                .param("isGradingCompleted", e.getIsGradingCompleted())
                .param("isHidden", e.getIsHidden())
                .param("isTeacherTopic", e.getIsTeacherTopic())
                .param("isSystemGenerated", e.getIsSystemGenerated())
                .param("lastPostId", e.getLastPostId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ForumTopicEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.content_item row exists (live). */
    public boolean existsContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CONTENT_ITEM_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.discussion_prompt row exists (live). */
    public boolean existsDiscussionPromptId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_DISCUSSION_PROMPT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.forum_section row exists (live). */
    public boolean existsForumSectionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_FORUM_SECTION_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.forum_post row exists (live). */
    public boolean existsLastPostId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LAST_POST_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.annotation rows still reference the row. */
    public boolean referencedByAnnotationForumTopicId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ANNOTATION_FORUM_TOPIC_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.assignment_group rows still reference the row. */
    public boolean referencedByAssignmentGroupDiscussionTopicId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ASSIGNMENT_GROUP_DISCUSSION_TOPIC_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.forum_post rows still reference the row. */
    public boolean referencedByForumPostForumTopicId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_FORUM_POST_FORUM_TOPIC_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.forum_read_state rows still reference the row. */
    public boolean referencedByForumReadStateForumTopicId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_FORUM_READ_STATE_FORUM_TOPIC_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.gradebook_entry rows still reference the row. */
    public boolean referencedByGradebookEntryForumTopicId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_GRADEBOOK_ENTRY_FORUM_TOPIC_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through discussion_prompt_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** lms.HideForumTopic; 0 rows means the row changed (or vanished) since it was read. */
    public int hideForumTopic(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_HIDE_FORUM_TOPIC)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** lms.UnhideForumTopic; 0 rows means the row changed (or vanished) since it was read. */
    public int unhideForumTopic(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_UNHIDE_FORUM_TOPIC)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, ForumTopicEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ForumTopicEntity map(ResultSet rs, int row) throws SQLException {
        return ForumTopicEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .forumSectionId(rs.getObject("forum_section_id", UUID.class))
                .title(rs.getString("title"))
                .description(rs.getString("description"))
                .topicType(rs.getString("topic_type"))
                .opensAt(rs.getObject("opens_at", OffsetDateTime.class))
                .closesAt(rs.getObject("closes_at", OffsetDateTime.class))
                .contentItemId(rs.getObject("content_item_id", UUID.class))
                .discussionPromptId(rs.getObject("discussion_prompt_id", UUID.class))
                .linkedEntityType(rs.getString("linked_entity_type"))
                .linkedEntityId(rs.getObject("linked_entity_id", UUID.class))
                .individualUserId(rs.getObject("individual_user_id", UUID.class))
                .isGraded(rs.getObject("is_graded", Boolean.class))
                .pointsPossible(rs.getBigDecimal("points_possible"))
                .gradeWeightPercent(rs.getBigDecimal("grade_weight_percent"))
                .gradingDueAt(rs.getObject("grading_due_at", OffsetDateTime.class))
                .isGradingCompleted(rs.getObject("is_grading_completed", Boolean.class))
                .isHidden(rs.getObject("is_hidden", Boolean.class))
                .isTeacherTopic(rs.getObject("is_teacher_topic", Boolean.class))
                .isSystemGenerated(rs.getObject("is_system_generated", Boolean.class))
                .lastPostId(rs.getObject("last_post_id", UUID.class))
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
