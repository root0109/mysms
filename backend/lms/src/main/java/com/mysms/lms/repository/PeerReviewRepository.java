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
import com.mysms.lms.domain.PeerReviewEntity;
import java.math.BigDecimal;
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
 * Plain-JDBC access to lms.peer_review: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class PeerReviewRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("assignmentGroupId", "r.assignment_group_id", ValueType.UUID),
            field("reviewerUserId", "r.reviewer_user_id", ValueType.UUID),
            field("revieweeUserId", "r.reviewee_user_id", ValueType.UUID),
            field("points", "r.points", ValueType.DECIMAL),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<PeerReviewEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PeerReviewEntity::getCreatedAt),
            Map.entry("updateTime", PeerReviewEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.assignment_group_id, r.reviewer_user_id, r.reviewee_user_id, r.points,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.peer_review r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.peer_review
            (id, tenant_id, assignment_group_id, reviewer_user_id, reviewee_user_id, points, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :assignmentGroupId, :reviewerUserId, :revieweeUserId, :points, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, assignment_group_id, reviewer_user_id, reviewee_user_id, points, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.peer_review
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.peer_review
               SET assignment_group_id = :assignmentGroupId,
                   reviewer_user_id = :reviewerUserId,
                   reviewee_user_id = :revieweeUserId,
                   points = :points,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_PEER_REVIEW_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.peer_review
                            WHERE tenant_id = :tenantId
                              AND assignment_group_id = :assignmentGroupId AND reviewer_user_id = :reviewerUserId AND
                                  reviewee_user_id = :revieweeUserId AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_ASSIGNMENT_GROUP_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment_group WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM lms.assignment_group p0 JOIN lms.cohort p1 ON p1.tenant_id = :tenantId AND
                p1.id = p0.cohort_id WHERE p0.tenant_id = :tenantId
                AND p0.id
            = :ref
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

    public PeerReviewRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PeerReviewEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PeerReviewRepository::map).optional();
    }

    public Page<PeerReviewEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PeerReviewEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PeerReviewRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PeerReviewEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public PeerReviewEntity insert(PeerReviewEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("assignmentGroupId", e.getAssignmentGroupId())
                .param("reviewerUserId", e.getReviewerUserId())
                .param("revieweeUserId", e.getRevieweeUserId())
                .param("points", e.getPoints())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(PeerReviewRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(PeerReviewEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("assignmentGroupId", e.getAssignmentGroupId())
                .param("reviewerUserId", e.getReviewerUserId())
                .param("revieweeUserId", e.getRevieweeUserId())
                .param("points", e.getPoints())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(PeerReviewEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_peer_review_1: assignment_group_id, reviewer_user_id, reviewee_user_id where (deleted_at IS NULL). */
    public boolean takenAssignmentGroupIdReviewerUserIdRevieweeUserId1(PeerReviewEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_PEER_REVIEW_1_TAKEN)
                .param("assignmentGroupId", e.getAssignmentGroupId())
                .param("reviewerUserId", e.getReviewerUserId())
                .param("revieweeUserId", e.getRevieweeUserId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced lms.assignment_group row exists (live). */
    public boolean existsAssignmentGroupId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ASSIGNMENT_GROUP_ID).param("id", id).query(Boolean.class).single());
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

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static final String OF_GROUP = SELECT_LIVE + " AND r.assignment_group_id = :groupId ORDER BY r.created_at";
    private static final String OF_REVIEWER = SELECT_LIVE
            + " AND r.assignment_group_id = :groupId AND r.reviewer_user_id = :reviewer ORDER BY r.created_at";
    private static final String FIND_REVIEW = SELECT_LIVE + " AND r.assignment_group_id = :groupId"
            + " AND r.reviewer_user_id = :reviewer AND r.reviewee_user_id = :reviewee";
    private static final String SET_POINTS = """
            UPDATE lms.peer_review
               SET points = :points,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** The group's live reviews, oldest first. */
    public List<PeerReviewEntity> ofGroup(UUID groupId) {
        return jdbc.sql(OF_GROUP).param("groupId", groupId).query(PeerReviewRepository::map).list();
    }

    /** The reviews one member wrote in the group, oldest first. */
    public List<PeerReviewEntity> ofReviewer(UUID groupId, UUID reviewer) {
        return jdbc.sql(OF_REVIEWER).param("groupId", groupId).param("reviewer", reviewer)
                .query(PeerReviewRepository::map).list();
    }

    /** The reviewer's live review of one member in the group. */
    public Optional<PeerReviewEntity> findReview(UUID groupId, UUID reviewer, UUID reviewee) {
        return jdbc.sql(FIND_REVIEW).param("groupId", groupId).param("reviewer", reviewer).param("reviewee", reviewee)
                .query(PeerReviewRepository::map).optional();
    }

    /** Replaces the points given; 0 rows means the row changed since it was read. */
    public int setPoints(UUID id, BigDecimal points, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(SET_POINTS).param("id", id).param("points", points).param("expected", expected)
                .param("now", now).param("actor", actor).update();
    }

    private static List<Object> sortValues(SortSpec sort, PeerReviewEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PeerReviewEntity map(ResultSet rs, int row) throws SQLException {
        return PeerReviewEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .assignmentGroupId(rs.getObject("assignment_group_id", UUID.class))
                .reviewerUserId(rs.getObject("reviewer_user_id", UUID.class))
                .revieweeUserId(rs.getObject("reviewee_user_id", UUID.class))
                .points(rs.getBigDecimal("points"))
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
