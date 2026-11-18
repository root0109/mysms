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
import com.mysms.lms.domain.AssignmentGroupMemberEntity;
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
 * Plain-JDBC access to lms.assignment_group_member: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class AssignmentGroupMemberRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("assignmentGroupId", "r.assignment_group_id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("contentItemId", "r.content_item_id", ValueType.UUID),
            field("status", "r.status", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<AssignmentGroupMemberEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", AssignmentGroupMemberEntity::getCreatedAt),
            Map.entry("updateTime", AssignmentGroupMemberEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.assignment_group_id, r.user_id, r.content_item_id, r.status, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.assignment_group_member r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.assignment_group_member
            (id, tenant_id, assignment_group_id, user_id, content_item_id, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :assignmentGroupId, :userId, :contentItemId, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, assignment_group_id, user_id, content_item_id, status, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.assignment_group_member
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.assignment_group_member
               SET assignment_group_id = :assignmentGroupId,
                   user_id = :userId,
                   content_item_id = :contentItemId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String IX_ASSIGNMENT_GROUP_MEMBER_2_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment_group_member
                            WHERE tenant_id = :tenantId
                              AND assignment_group_id = :assignmentGroupId AND user_id = :userId AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_ASSIGNMENT_GROUP_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment_group WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM lms.assignment_group p0 JOIN lms.cohort p1 ON p1.tenant_id = :tenantId AND
                p1.id = p0.cohort_id WHERE p0.tenant_id = :tenantId
                AND p0.id
            = :ref
            """;

    /** lms.AcceptGroupRequest: allowed while status is requested. */
    private static final String TRANSITION_ACCEPT_GROUP_REQUEST = """
            UPDATE lms.assignment_group_member
               SET status = 'active',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** lms.RejectGroupRequest: allowed while status is requested. */
    private static final String TRANSITION_REJECT_GROUP_REQUEST = """
            UPDATE lms.assignment_group_member
               SET status = 'rejected',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.assignment_group_id IN (SELECT p0.id FROM lms.assignment_group p0 WHERE p0.tenant_id =
                 :tenantId
                 AND (p0.cohort_id IN
             (SELECT p1.id FROM lms.cohort p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.institution_id = ANY(:scopeIds) OR p1.institution_id IS
             NULL)))))
            """;

    private final JdbcClient jdbc;

    public AssignmentGroupMemberRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AssignmentGroupMemberEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AssignmentGroupMemberRepository::map).optional();
    }

    public Page<AssignmentGroupMemberEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AssignmentGroupMemberEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(AssignmentGroupMemberRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), AssignmentGroupMemberEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public AssignmentGroupMemberEntity insert(AssignmentGroupMemberEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("assignmentGroupId", e.getAssignmentGroupId())
                .param("userId", e.getUserId())
                .param("contentItemId", e.getContentItemId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(AssignmentGroupMemberRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(AssignmentGroupMemberEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("assignmentGroupId", e.getAssignmentGroupId())
                .param("userId", e.getUserId())
                .param("contentItemId", e.getContentItemId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(AssignmentGroupMemberEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** ix_assignment_group_member_2: assignment_group_id, user_id where (deleted_at IS NULL). */
    public boolean takenAssignmentGroupIdUserId2(AssignmentGroupMemberEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(IX_ASSIGNMENT_GROUP_MEMBER_2_TAKEN)
                .param("assignmentGroupId", e.getAssignmentGroupId())
                .param("userId", e.getUserId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced lms.assignment_group row exists (live). */
    public boolean existsAssignmentGroupId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ASSIGNMENT_GROUP_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.content_item row exists (live). */
    public boolean existsContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CONTENT_ITEM_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through assignment_group_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** lms.AcceptGroupRequest; 0 rows means the row changed (or vanished) since it was read. */
    public int acceptGroupRequest(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_ACCEPT_GROUP_REQUEST)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** lms.RejectGroupRequest; 0 rows means the row changed (or vanished) since it was read. */
    public int rejectGroupRequest(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_REJECT_GROUP_REQUEST)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static final String INSERT_WITH_STATUS = """
            INSERT INTO lms.assignment_group_member
            (id, tenant_id, assignment_group_id, user_id, content_item_id, status, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :assignmentGroupId, :userId, :contentItemId, :status, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, assignment_group_id, user_id, content_item_id, status, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String OF_GROUP = SELECT_LIVE + " AND r.assignment_group_id = :groupId ORDER BY r.created_at";
    private static final String FIND_IN_GROUP = SELECT_LIVE
            + " AND r.assignment_group_id = :groupId AND r.user_id = :userId";
    private static final String ACTIVE_FOR_ITEM = SELECT_LIVE
            + " AND r.content_item_id = :itemId AND r.user_id = :userId AND r.status = 'active'";
    private static final String SET_STATUS = """
            UPDATE lms.assignment_group_member
               SET status = :status,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** Inserts the row with an explicit membership status (requested on a join request). */
    public AssignmentGroupMemberEntity insertWithStatus(AssignmentGroupMemberEntity e) {
        return jdbc.sql(INSERT_WITH_STATUS)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("assignmentGroupId", e.getAssignmentGroupId())
                .param("userId", e.getUserId())
                .param("contentItemId", e.getContentItemId())
                .param("status", e.getStatus())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(AssignmentGroupMemberRepository::map).single();
    }

    /** The group's live membership rows, oldest first. */
    public List<AssignmentGroupMemberEntity> ofGroup(UUID groupId) {
        return jdbc.sql(OF_GROUP).param("groupId", groupId).query(AssignmentGroupMemberRepository::map).list();
    }

    /** The user's live membership row in the group, whatever its status. */
    public Optional<AssignmentGroupMemberEntity> findInGroup(UUID groupId, UUID userId) {
        return jdbc.sql(FIND_IN_GROUP).param("groupId", groupId).param("userId", userId)
                .query(AssignmentGroupMemberRepository::map).optional();
    }

    /** The user's active membership for the item, in any of its groups. */
    public Optional<AssignmentGroupMemberEntity> activeForItem(UUID itemId, UUID userId) {
        return jdbc.sql(ACTIVE_FOR_ITEM).param("itemId", itemId).param("userId", userId)
                .query(AssignmentGroupMemberRepository::map).optional();
    }

    /** Sets the membership status; 0 rows means the row changed since it was read. */
    public int setStatus(UUID id, String status, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(SET_STATUS).param("id", id).param("status", status).param("expected", expected)
                .param("now", now).param("actor", actor).update();
    }

    private static List<Object> sortValues(SortSpec sort, AssignmentGroupMemberEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static AssignmentGroupMemberEntity map(ResultSet rs, int row) throws SQLException {
        return AssignmentGroupMemberEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .assignmentGroupId(rs.getObject("assignment_group_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .contentItemId(rs.getObject("content_item_id", UUID.class))
                .status(rs.getString("status"))
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
