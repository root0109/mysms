package com.mysms.students.sis.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.students.sis.config.SisDatabaseConfig;
import com.mysms.students.sis.domain.StudentLeaveRequestEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
 * Plain-JDBC access to sis.student_leave_request: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class StudentLeaveRequestRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("studentId", "r.student_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("fromDate", "r.from_date", ValueType.DATE),
            field("toDate", "r.to_date", ValueType.DATE),
            field("dayPart", "r.day_part", ValueType.STRING),
            field("reason", "r.reason", ValueType.STRING),
            field("status", "r.status", ValueType.STRING),
            field("requestedByUserId", "r.requested_by_user_id", ValueType.UUID),
            field("decidedByUserId", "r.decided_by_user_id", ValueType.UUID),
            field("decidedAt", "r.decided_at", ValueType.TIMESTAMP),
            field("decisionNote", "r.decision_note", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<StudentLeaveRequestEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", StudentLeaveRequestEntity::getCreatedAt),
            Map.entry("updateTime", StudentLeaveRequestEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.student_id, r.academic_year_id, r.from_date, r.to_date, r.day_part, r.reason,
                   r.status, r.requested_by_user_id, r.decided_by_user_id, r.decided_at, r.decision_note,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM sis.student_leave_request r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO sis.student_leave_request
            (id, tenant_id, student_id, academic_year_id, from_date, to_date, day_part, reason, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :studentId, :academicYearId, :fromDate, :toDate,
                    COALESCE(:dayPart, 'full_day'::character varying), :reason, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, student_id, academic_year_id, from_date, to_date, day_part, reason, status,
                      requested_by_user_id, decided_by_user_id, decided_at, decision_note, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE sis.student_leave_request
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE sis.student_leave_request
               SET student_id = :studentId,
                   academic_year_id = :academicYearId,
                   from_date = :fromDate,
                   to_date = :toDate,
                   day_part = :dayPart,
                   reason = :reason,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_STUDENT_ID = """
            SELECT EXISTS (SELECT 1 FROM sis.student WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM sis.student p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;

    /** sis.ApproveStudentLeaveRequest: allowed while status is pending. */
    private static final String TRANSITION_APPROVE_STUDENT_LEAVE_REQUEST = """
            UPDATE sis.student_leave_request
               SET status = 'approved',
                   decided_by_user_id = :actor,
                   decided_at = :now,
                   decision_note = :pDecisionNote,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** sis.RejectStudentLeaveRequest: allowed while status is pending. */
    private static final String TRANSITION_REJECT_STUDENT_LEAVE_REQUEST = """
            UPDATE sis.student_leave_request
               SET status = 'rejected',
                   decided_by_user_id = :actor,
                   decided_at = :now,
                   decision_note = :pDecisionNote,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.student_id IN (SELECT p0.id FROM sis.student p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public StudentLeaveRequestRepository(@Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<StudentLeaveRequestEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(StudentLeaveRequestRepository::map).optional();
    }

    public Page<StudentLeaveRequestEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<StudentLeaveRequestEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(StudentLeaveRequestRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), StudentLeaveRequestEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public StudentLeaveRequestEntity insert(StudentLeaveRequestEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("studentId", e.getStudentId())
                .param("academicYearId", e.getAcademicYearId())
                .param("fromDate", e.getFromDate())
                .param("toDate", e.getToDate())
                .param("dayPart", e.getDayPart())
                .param("reason", e.getReason())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(StudentLeaveRequestRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(StudentLeaveRequestEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("studentId", e.getStudentId())
                .param("academicYearId", e.getAcademicYearId())
                .param("fromDate", e.getFromDate())
                .param("toDate", e.getToDate())
                .param("dayPart", e.getDayPart())
                .param("reason", e.getReason())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(StudentLeaveRequestEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced sis.student row exists (live). */
    public boolean existsStudentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_STUDENT_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through student_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** sis.ApproveStudentLeaveRequest; 0 rows means the row changed (or vanished) since it was read. */
    public int approveStudentLeaveRequest(UUID id, int expected, OffsetDateTime now, UUID actor, String decisionNote) {
        return jdbc.sql(TRANSITION_APPROVE_STUDENT_LEAVE_REQUEST)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor)
                .param("pDecisionNote", decisionNote).update();
    }

    /** sis.RejectStudentLeaveRequest; 0 rows means the row changed (or vanished) since it was read. */
    public int rejectStudentLeaveRequest(UUID id, int expected, OffsetDateTime now, UUID actor, String decisionNote) {
        return jdbc.sql(TRANSITION_REJECT_STUDENT_LEAVE_REQUEST)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor)
                .param("pDecisionNote", decisionNote).update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, StudentLeaveRequestEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static StudentLeaveRequestEntity map(ResultSet rs, int row) throws SQLException {
        return StudentLeaveRequestEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .studentId(rs.getObject("student_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .fromDate(rs.getObject("from_date", LocalDate.class))
                .toDate(rs.getObject("to_date", LocalDate.class))
                .dayPart(rs.getString("day_part"))
                .reason(rs.getString("reason"))
                .status(rs.getString("status"))
                .requestedByUserId(rs.getObject("requested_by_user_id", UUID.class))
                .decidedByUserId(rs.getObject("decided_by_user_id", UUID.class))
                .decidedAt(rs.getObject("decided_at", OffsetDateTime.class))
                .decisionNote(rs.getString("decision_note"))
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
