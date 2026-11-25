package com.mysms.comms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.domain.FeedbackThreadEntity;
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
import java.util.HashMap;
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
 * Plain-JDBC access to comms.feedback_thread: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class FeedbackThreadRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("guardianId", "r.guardian_id", ValueType.UUID),
            field("studentId", "r.student_id", ValueType.UUID),
            field("assignedToUserId", "r.assigned_to_user_id", ValueType.UUID),
            field("status", "r.status", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<FeedbackThreadEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", FeedbackThreadEntity::getCreatedAt),
            Map.entry("updateTime", FeedbackThreadEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.guardian_id, r.student_id, r.assigned_to_user_id, r.status,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM comms.feedback_thread r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO comms.feedback_thread
            (id, tenant_id, institution_id, guardian_id, student_id, assigned_to_user_id, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :guardianId, :studentId, :assignedToUserId, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, guardian_id, student_id, assigned_to_user_id, status, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE comms.feedback_thread
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE comms.feedback_thread
               SET institution_id = :institutionId,
                   guardian_id = :guardianId,
                   student_id = :studentId,
                   assigned_to_user_id = :assignedToUserId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String REFERENCED_BY_FEEDBACK_MESSAGE_THREAD_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.feedback_message WHERE tenant_id = :tenantId
                AND thread_id = :id AND deleted_at IS NULL)
            """;

    /** comms.CloseFeedbackThread: allowed while status is open. */
    private static final String TRANSITION_CLOSE_FEEDBACK_THREAD = """
            UPDATE comms.feedback_thread
               SET status = 'closed',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** comms.ReopenFeedbackThread: allowed while status is closed. */
    private static final String TRANSITION_REOPEN_FEEDBACK_THREAD = """
            UPDATE comms.feedback_thread
               SET status = 'open',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;
    /**
     * The threads the caller reaches: those handled by them, every thread of the institutions where they work on the
     * school side, and those opened as a guardian record that is the caller's account.
     */
    private static final String REACHABLE = """
             (r.assigned_to_user_id = :reachUser
              OR (:schoolSide AND (:schoolAll OR r.institution_id = ANY(:schoolIds)))
              OR r.guardian_id IN (SELECT g.guardian_id FROM comms.guardian_link g
                                    WHERE g.tenant_id = :tenantId AND NOT g.is_deleted AND g.status = 'active'
                                      AND g.guardian_user_id = :reachUser))
            """;
    /** {@link #REACHABLE}, and the threads opened as a guardian record that is the caller's person. */
    private static final String REACHABLE_WITH_PERSON = """
             (r.assigned_to_user_id = :reachUser
              OR (:schoolSide AND (:schoolAll OR r.institution_id = ANY(:schoolIds)))
              OR r.guardian_id IN (SELECT g.guardian_id FROM comms.guardian_link g
                                    WHERE g.tenant_id = :tenantId AND NOT g.is_deleted AND g.status = 'active'
                                      AND (g.guardian_user_id = :reachUser OR g.guardian_person_id = :reachPerson)))
            """;

    private final JdbcClient jdbc;

    public FeedbackThreadRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<FeedbackThreadEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(FeedbackThreadRepository::map).optional();
    }

    public Page<FeedbackThreadEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<FeedbackThreadEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(FeedbackThreadRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), FeedbackThreadEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public FeedbackThreadEntity insert(FeedbackThreadEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("guardianId", e.getGuardianId())
                .param("studentId", e.getStudentId())
                .param("assignedToUserId", e.getAssignedToUserId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(FeedbackThreadRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(FeedbackThreadEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("guardianId", e.getGuardianId())
                .param("studentId", e.getStudentId())
                .param("assignedToUserId", e.getAssignedToUserId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(FeedbackThreadEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether live comms.feedback_message rows still reference the row. */
    public boolean referencedByFeedbackMessageThreadId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_FEEDBACK_MESSAGE_THREAD_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** comms.CloseFeedbackThread; 0 rows means the row changed (or vanished) since it was read. */
    public int closeFeedbackThread(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CLOSE_FEEDBACK_THREAD)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** comms.ReopenFeedbackThread; 0 rows means the row changed (or vanished) since it was read. */
    public int reopenFeedbackThread(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_REOPEN_FEEDBACK_THREAD)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** The threads the account and person reach, the school side's institutions given as a scope. */
    public static SqlFragment reachableBy(UUID user, Optional<UUID> person, DataScope schoolSide) {
        Map<String, Object> params = new HashMap<>(Map.of("reachUser", user, "schoolSide",
                schoolSide.all() || !schoolSide.institutions().isEmpty(), "schoolAll", schoolSide.all(), "schoolIds",
                schoolSide.institutionArray()));
        person.ifPresent(p -> params.put("reachPerson", p));
        return new SqlFragment(person.isPresent() ? REACHABLE_WITH_PERSON : REACHABLE, params);
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, FeedbackThreadEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static FeedbackThreadEntity map(ResultSet rs, int row) throws SQLException {
        return FeedbackThreadEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .guardianId(rs.getObject("guardian_id", UUID.class))
                .studentId(rs.getObject("student_id", UUID.class))
                .assignedToUserId(rs.getObject("assigned_to_user_id", UUID.class))
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
