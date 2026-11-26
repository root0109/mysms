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
import com.mysms.learning.lms.domain.AssignmentGroupEntity;
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
 * Plain-JDBC access to lms.assignment_group: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class AssignmentGroupRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("contentItemId", "r.content_item_id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("name", "r.name", ValueType.STRING),
            field("points", "r.points", ValueType.DECIMAL),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("discussionTopicId", "r.discussion_topic_id", ValueType.UUID),
            field("isReassigned", "r.is_reassigned", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<AssignmentGroupEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", AssignmentGroupEntity::getCreatedAt),
            Map.entry("updateTime", AssignmentGroupEntity::getUpdatedAt),
            Map.entry("name", AssignmentGroupEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.content_item_id, r.cohort_id, r.name, r.points, r.is_active,
                   r.discussion_topic_id, r.is_reassigned, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM lms.assignment_group r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.assignment_group
            (id, tenant_id, content_item_id, cohort_id, name, points, is_active, discussion_topic_id, is_reassigned,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :contentItemId, :cohortId, :name, :points, COALESCE(:isActive, true),
                    :discussionTopicId, COALESCE(:isReassigned, false), :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, content_item_id, cohort_id, name, points, is_active, discussion_topic_id,
                      is_reassigned, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.assignment_group
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.assignment_group
               SET cohort_id = :cohortId,
                   name = :name,
                   points = :points,
                   is_active = :isActive,
                   discussion_topic_id = :discussionTopicId,
                   is_reassigned = :isReassigned,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_ASSIGNMENT_GROUP_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment_group
                            WHERE tenant_id = :tenantId
                              AND content_item_id = :contentItemId AND name = :name AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String EXISTS_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_DISCUSSION_TOPIC_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_topic WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ASSIGNMENT_GROUP_MEMBER_ASSIGNMENT_GROUP_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment_group_member WHERE tenant_id = :tenantId
                AND assignment_group_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ASSIGNMENT_SUBMISSION_GROUP_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment_submission WHERE tenant_id = :tenantId
                AND group_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_GRADEBOOK_ENTRY_GROUP_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.gradebook_entry WHERE tenant_id = :tenantId
                AND group_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ITEM_OVERRIDE_GROUP_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.item_override WHERE tenant_id = :tenantId
                AND group_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_PEER_REVIEW_ASSIGNMENT_GROUP_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.peer_review WHERE tenant_id = :tenantId
                AND assignment_group_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_REASSIGNMENT_LOG_GROUP_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.reassignment_log WHERE tenant_id = :tenantId
                AND group_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RUBRIC_ASSESSMENT_GROUP_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.rubric_assessment WHERE tenant_id = :tenantId
                AND group_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.cohort_id IN (SELECT p0.id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)))
            """;

    private final JdbcClient jdbc;

    public AssignmentGroupRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AssignmentGroupEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AssignmentGroupRepository::map).optional();
    }

    public Page<AssignmentGroupEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AssignmentGroupEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(AssignmentGroupRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), AssignmentGroupEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public AssignmentGroupEntity insert(AssignmentGroupEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("contentItemId", e.getContentItemId())
                .param("cohortId", e.getCohortId())
                .param("name", e.getName())
                .param("points", e.getPoints())
                .param("isActive", e.getIsActive())
                .param("discussionTopicId", e.getDiscussionTopicId())
                .param("isReassigned", e.getIsReassigned())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(AssignmentGroupRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(AssignmentGroupEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("cohortId", e.getCohortId())
                .param("name", e.getName())
                .param("points", e.getPoints())
                .param("isActive", e.getIsActive())
                .param("discussionTopicId", e.getDiscussionTopicId())
                .param("isReassigned", e.getIsReassigned())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(AssignmentGroupEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_assignment_group_1: content_item_id, name where (deleted_at IS NULL). */
    public boolean takenContentItemIdName1(AssignmentGroupEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_ASSIGNMENT_GROUP_1_TAKEN)
                .param("contentItemId", e.getContentItemId())
                .param("name", e.getName())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced lms.cohort row exists (live). */
    public boolean existsCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COHORT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.content_item row exists (live). */
    public boolean existsContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CONTENT_ITEM_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.forum_topic row exists (live). */
    public boolean existsDiscussionTopicId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_DISCUSSION_TOPIC_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.assignment_group_member rows still reference the row. */
    public boolean referencedByAssignmentGroupMemberAssignmentGroupId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ASSIGNMENT_GROUP_MEMBER_ASSIGNMENT_GROUP_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.assignment_submission rows still reference the row. */
    public boolean referencedByAssignmentSubmissionGroupId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ASSIGNMENT_SUBMISSION_GROUP_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.gradebook_entry rows still reference the row. */
    public boolean referencedByGradebookEntryGroupId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_GRADEBOOK_ENTRY_GROUP_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.item_override rows still reference the row. */
    public boolean referencedByItemOverrideGroupId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ITEM_OVERRIDE_GROUP_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.peer_review rows still reference the row. */
    public boolean referencedByPeerReviewAssignmentGroupId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PEER_REVIEW_ASSIGNMENT_GROUP_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.reassignment_log rows still reference the row. */
    public boolean referencedByReassignmentLogGroupId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_REASSIGNMENT_LOG_GROUP_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.rubric_assessment rows still reference the row. */
    public boolean referencedByRubricAssessmentGroupId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RUBRIC_ASSESSMENT_GROUP_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through cohort_id (empty: a tenant-level row, or none). */
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

    private static final String OF_ITEM = SELECT_LIVE + " AND r.content_item_id = :itemId ORDER BY r.created_at";
    private static final String MARK_REASSIGNED = """
            UPDATE lms.assignment_group
               SET is_reassigned = true,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND deleted_at IS NULL
            """;

    /** The item's live groups, oldest first. */
    public List<AssignmentGroupEntity> ofItem(UUID itemId) {
        return jdbc.sql(OF_ITEM).param("itemId", itemId).query(AssignmentGroupRepository::map).list();
    }

    /** Marks the group reassigned for a fresh attempt. */
    public void markReassigned(UUID id, OffsetDateTime now, UUID actor) {
        jdbc.sql(MARK_REASSIGNED).param("id", id).param("now", now).param("actor", actor).update();
    }

    private static List<Object> sortValues(SortSpec sort, AssignmentGroupEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static AssignmentGroupEntity map(ResultSet rs, int row) throws SQLException {
        return AssignmentGroupEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .contentItemId(rs.getObject("content_item_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .name(rs.getString("name"))
                .points(rs.getBigDecimal("points"))
                .isActive(rs.getObject("is_active", Boolean.class))
                .discussionTopicId(rs.getObject("discussion_topic_id", UUID.class))
                .isReassigned(rs.getObject("is_reassigned", Boolean.class))
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
