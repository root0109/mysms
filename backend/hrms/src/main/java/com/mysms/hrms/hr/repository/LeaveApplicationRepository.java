package com.mysms.hrms.hr.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.hrms.hr.config.HrDatabaseConfig;
import com.mysms.hrms.hr.domain.LeaveApplicationEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
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
 * Plain-JDBC access to hr.leave_application: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class LeaveApplicationRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("staffId", "r.staff_id", ValueType.UUID),
            field("leaveTypeId", "r.leave_type_id", ValueType.UUID),
            field("absenceReasonId", "r.absence_reason_id", ValueType.UUID),
            field("classSectionId", "r.class_section_id", ValueType.UUID),
            field("fromAt", "r.from_at", ValueType.TIMESTAMP),
            field("toAt", "r.to_at", ValueType.TIMESTAMP),
            field("dayPart", "r.day_part", ValueType.STRING),
            field("remarks", "r.remarks", ValueType.STRING),
            field("status", "r.status", ValueType.STRING),
            field("decidedByUserId", "r.decided_by_user_id", ValueType.UUID),
            field("decidedOn", "r.decided_on", ValueType.DATE),
            field("decisionComments", "r.decision_comments", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<LeaveApplicationEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", LeaveApplicationEntity::getCreatedAt),
            Map.entry("updateTime", LeaveApplicationEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.academic_year_id, r.staff_id, r.leave_type_id,
                   r.absence_reason_id, r.class_section_id, r.from_at, r.to_at, r.day_part, r.remarks, r.status,
                   r.decided_by_user_id, r.decided_on, r.decision_comments, r.counted_days, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM hr.leave_application r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO hr.leave_application
            (id, tenant_id, institution_id, academic_year_id, staff_id, leave_type_id, absence_reason_id,
             class_section_id, from_at, to_at, day_part, remarks, decision_comments, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :academicYearId, :staffId, :leaveTypeId, :absenceReasonId,
                    :classSectionId, :fromAt, :toAt, COALESCE(:dayPart, 'full_day'::character varying), :remarks,
                    :decisionComments, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, academic_year_id, staff_id, leave_type_id, absence_reason_id,
                      class_section_id, from_at, to_at, day_part, remarks, status, decided_by_user_id, decided_on,
                      decision_comments, counted_days, created_at, created_by, updated_at, updated_by, deleted_at,
                      row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE hr.leave_application
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE hr.leave_application
               SET institution_id = :institutionId,
                   academic_year_id = :academicYearId,
                   staff_id = :staffId,
                   leave_type_id = :leaveTypeId,
                   absence_reason_id = :absenceReasonId,
                   class_section_id = :classSectionId,
                   from_at = :fromAt,
                   to_at = :toAt,
                   day_part = :dayPart,
                   remarks = :remarks,
                   decision_comments = :decisionComments,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_LEAVE_TYPE_ID = """
            SELECT EXISTS (SELECT 1 FROM hr.leave_type WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_STAFF_ID = """
            SELECT EXISTS (SELECT 1 FROM hr.staff WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;

    /** hr.RejectLeave: allowed while status is pending. */
    private static final String TRANSITION_REJECT_LEAVE = """
            UPDATE hr.leave_application
               SET status = 'rejected',
                   decided_by_user_id = :actor,
                   decided_on = :today,
                   decision_comments = :comments,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** hr.ApproveLeave: allowed while status is pending. */
    private static final String TRANSITION_APPROVE_LEAVE = """
            UPDATE hr.leave_application
               SET status = 'approved',
                   decided_by_user_id = :actor,
                   decided_on = :today,
                   decision_comments = :comments,
                   counted_days = :countedDays,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** hr.CancelLeave: a pending application, or an approved one before it starts (the service checks which). */
    private static final String TRANSITION_CANCEL_LEAVE = """
            UPDATE hr.leave_application
               SET status = 'cancelled',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public LeaveApplicationRepository(@Qualifier(HrDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<LeaveApplicationEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(LeaveApplicationRepository::map).optional();
    }

    public Page<LeaveApplicationEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<LeaveApplicationEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(LeaveApplicationRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), LeaveApplicationEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public LeaveApplicationEntity insert(LeaveApplicationEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("staffId", e.getStaffId())
                .param("leaveTypeId", e.getLeaveTypeId())
                .param("absenceReasonId", e.getAbsenceReasonId())
                .param("classSectionId", e.getClassSectionId())
                .param("fromAt", e.getFromAt())
                .param("toAt", e.getToAt())
                .param("dayPart", e.getDayPart())
                .param("remarks", e.getRemarks())
                .param("decisionComments", e.getDecisionComments())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(LeaveApplicationRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(LeaveApplicationEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("staffId", e.getStaffId())
                .param("leaveTypeId", e.getLeaveTypeId())
                .param("absenceReasonId", e.getAbsenceReasonId())
                .param("classSectionId", e.getClassSectionId())
                .param("fromAt", e.getFromAt())
                .param("toAt", e.getToAt())
                .param("dayPart", e.getDayPart())
                .param("remarks", e.getRemarks())
                .param("decisionComments", e.getDecisionComments())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(LeaveApplicationEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced hr.leave_type row exists (live). */
    public boolean existsLeaveTypeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LEAVE_TYPE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced hr.staff row exists (live). */
    public boolean existsStaffId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_STAFF_ID).param("id", id).query(Boolean.class).single());
    }

    /** hr.RejectLeave; 0 rows means the row changed (or vanished) since it was read. */
    public int rejectLeave(UUID id, int expected, OffsetDateTime now, UUID actor, LocalDate today,
            String comments) {
        return jdbc.sql(TRANSITION_REJECT_LEAVE)
                .param("comments", comments)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor)
                .param("today", today).update();
    }

    /** hr.ApproveLeave; 0 rows means the row changed (or vanished) since it was read. */
    public int approveLeave(UUID id, int expected, OffsetDateTime now, UUID actor, LocalDate today, String comments,
            BigDecimal countedDays) {
        return jdbc.sql(TRANSITION_APPROVE_LEAVE)
                .param("countedDays", countedDays)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor)
                .param("today", today)
                .param("comments", comments).update();
    }

    /** hr.CancelLeave; 0 rows means the row changed (or vanished) since it was read. */
    public int cancelLeave(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CANCEL_LEAVE)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, LeaveApplicationEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static LeaveApplicationEntity map(ResultSet rs, int row) throws SQLException {
        return LeaveApplicationEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .staffId(rs.getObject("staff_id", UUID.class))
                .leaveTypeId(rs.getObject("leave_type_id", UUID.class))
                .absenceReasonId(rs.getObject("absence_reason_id", UUID.class))
                .classSectionId(rs.getObject("class_section_id", UUID.class))
                .fromAt(rs.getObject("from_at", OffsetDateTime.class))
                .toAt(rs.getObject("to_at", OffsetDateTime.class))
                .dayPart(rs.getString("day_part"))
                .remarks(rs.getString("remarks"))
                .status(rs.getString("status"))
                .decidedByUserId(rs.getObject("decided_by_user_id", UUID.class))
                .decidedOn(rs.getObject("decided_on", LocalDate.class))
                .decisionComments(rs.getString("decision_comments"))
                .countedDays(rs.getBigDecimal("counted_days"))
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
