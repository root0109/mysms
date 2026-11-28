package com.mysms.hrms.timeattendance.repository;
import com.mysms.hrms.timeattendance.config.TimeAttendanceDatabaseConfig;
import com.mysms.hrms.timeattendance.domain.StaffAttendanceEntity;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
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
 * Plain-JDBC access to time_attendance.staff_attendance: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class StaffAttendanceRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("staffId", "r.staff_id", ValueType.UUID),
            field("attendanceDate", "r.attendance_date", ValueType.DATE),
            field("status", "r.status", ValueType.STRING),
            field("checkInAt", "r.check_in_at", ValueType.TIMESTAMP),
            field("checkOutAt", "r.check_out_at", ValueType.TIMESTAMP),
            field("source", "r.source", ValueType.STRING),
            field("leaveApplicationId", "r.leave_application_id", ValueType.UUID),
            field("recordedByUserId", "r.recorded_by_user_id", ValueType.UUID),
            field("remark", "r.remark", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<StaffAttendanceEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", StaffAttendanceEntity::getCreatedAt),
            Map.entry("updateTime", StaffAttendanceEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.staff_id, r.attendance_date, r.status, r.check_in_at, r.check_out_at,
                   r.source, r.leave_application_id, r.recorded_by_user_id, r.remark, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM time_attendance.staff_attendance r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO time_attendance.staff_attendance
            (id, tenant_id, staff_id, attendance_date, status, check_in_at, check_out_at, leave_application_id,
             remark, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :staffId, :attendanceDate, :status, :checkInAt, :checkOutAt, :leaveApplicationId,
                    :remark, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, staff_id, attendance_date, status, check_in_at, check_out_at, source,
                      leave_application_id, recorded_by_user_id, remark, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE time_attendance.staff_attendance
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE time_attendance.staff_attendance
               SET status = :status,
                   check_in_at = :checkInAt,
                   check_out_at = :checkOutAt,
                   leave_application_id = :leaveApplicationId,
                   remark = :remark,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_STAFF_ATTENDANCE_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM time_attendance.staff_attendance
                            WHERE tenant_id = :tenantId
                              AND staff_id = :staffId AND attendance_date = :attendanceDate AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;

    private final JdbcClient jdbc;

    public StaffAttendanceRepository(@Qualifier(TimeAttendanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<StaffAttendanceEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(StaffAttendanceRepository::map).optional();
    }

    public Page<StaffAttendanceEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<StaffAttendanceEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(StaffAttendanceRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), StaffAttendanceEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public StaffAttendanceEntity insert(StaffAttendanceEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("staffId", e.getStaffId())
                .param("attendanceDate", e.getAttendanceDate())
                .param("status", e.getStatus())
                .param("checkInAt", e.getCheckInAt())
                .param("checkOutAt", e.getCheckOutAt())
                .param("leaveApplicationId", e.getLeaveApplicationId())
                .param("remark", e.getRemark())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(StaffAttendanceRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(StaffAttendanceEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("status", e.getStatus())
                .param("checkInAt", e.getCheckInAt())
                .param("checkOutAt", e.getCheckOutAt())
                .param("leaveApplicationId", e.getLeaveApplicationId())
                .param("remark", e.getRemark())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(StaffAttendanceEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_staff_attendance_1: staff_id, attendance_date where (deleted_at IS NULL). */
    public boolean takenStaffIdAttendanceDate1(StaffAttendanceEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_STAFF_ATTENDANCE_1_TAKEN)
                .param("staffId", e.getStaffId())
                .param("attendanceDate", e.getAttendanceDate())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, StaffAttendanceEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static StaffAttendanceEntity map(ResultSet rs, int row) throws SQLException {
        return StaffAttendanceEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .staffId(rs.getObject("staff_id", UUID.class))
                .attendanceDate(rs.getObject("attendance_date", LocalDate.class))
                .status(rs.getString("status"))
                .checkInAt(rs.getObject("check_in_at", OffsetDateTime.class))
                .checkOutAt(rs.getObject("check_out_at", OffsetDateTime.class))
                .source(rs.getString("source"))
                .leaveApplicationId(rs.getObject("leave_application_id", UUID.class))
                .recordedByUserId(rs.getObject("recorded_by_user_id", UUID.class))
                .remark(rs.getString("remark"))
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
