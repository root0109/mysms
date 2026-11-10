package com.mysms.attendance.repository;
import com.mysms.attendance.config.AttendanceDatabaseConfig;
import com.mysms.attendance.domain.AttendanceRecordEntity;
import com.mysms.authclient.DataScope;
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
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
/**
 * Plain-JDBC access to attendance.attendance_record (JAVA-CODING-RULES §6): schema-qualified constant SQL, named
 * parameters, live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class AttendanceRecordRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("leaveRequestId", "r.leave_request_id", ValueType.UUID),
            field("sheetId", "r.sheet_id", ValueType.UUID),
            field("studentId", "r.student_id", ValueType.UUID),
            field("status", "r.status", ValueType.STRING),
            field("attendanceTypeId", "r.attendance_type_id", ValueType.UUID),
            field("absenceReasonId", "r.absence_reason_id", ValueType.UUID),
            field("remark", "r.remark", ValueType.STRING),
            field("recordedByUserId", "r.recorded_by_user_id", ValueType.UUID),
            field("attendanceDate", "r.attendance_date", ValueType.DATE),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<AttendanceRecordEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", AttendanceRecordEntity::getCreatedAt),
            Map.entry("updateTime", AttendanceRecordEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.leave_request_id, r.sheet_id, r.student_id, r.status, r.attendance_type_id,
                   r.absence_reason_id, r.remark, r.recorded_by_user_id, r.attendance_date, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM attendance.attendance_record r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO attendance.attendance_record
            (id, tenant_id, sheet_id, student_id, status, attendance_type_id, absence_reason_id, remark,
             recorded_by_user_id, attendance_date, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :sheetId, :studentId, :status, :attendanceTypeId, :absenceReasonId, :remark,
                    :recordedByUserId,
                    (SELECT p.attendance_date FROM attendance.attendance_sheet p WHERE p.tenant_id = :tenantId
                        AND p.id = :sheetId), :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, leave_request_id, sheet_id, student_id, status, attendance_type_id,
                      absence_reason_id, remark, recorded_by_user_id, attendance_date, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE attendance.attendance_record
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE attendance.attendance_record
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_ATTENDANCE_RECORD_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM attendance.attendance_record
                            WHERE tenant_id = :tenantId
                              AND sheet_id = :sheetId AND student_id = :studentId AND attendance_date = :attendanceDate
                                  AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_ABSENCE_REASON_ID = """
            SELECT EXISTS (SELECT 1 FROM attendance.absence_reason WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_ATTENDANCE_TYPE_ID = """
            SELECT EXISTS (SELECT 1 FROM attendance.attendance_type WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_SHEET_ID = """
            SELECT EXISTS (SELECT 1 FROM attendance.attendance_sheet WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM attendance.attendance_sheet p0 JOIN attendance.attendance_register p1 ON
                p1.tenant_id = :tenantId AND p1.id
            = p0.register_id WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.sheet_id IN (SELECT p0.id FROM attendance.attendance_sheet p0 WHERE p0.tenant_id =
                 :tenantId
                 AND (p0.register_id IN
             (SELECT p1.id FROM attendance.attendance_register p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.institution_id = ANY(:scopeIds))))))
            """;

    /** Every live mark of one sheet; the sheet's date keeps the read inside one partition. */
    private static final String FIND_BY_SHEET = SELECT_LIVE
            + " AND r.sheet_id = :sheetId AND r.attendance_date = :attendanceDate ORDER BY r.created_at, r.id";
    /**
     * attendance.RecordAttendance: corrects an existing mark (the workflow's only write to these columns), under the
     * version the mark was read with.
     */
    private static final String UPDATE_MARK = """
            UPDATE attendance.attendance_record
               SET status = :status,
                   attendance_type_id = :attendanceTypeId,
                   absence_reason_id = :absenceReasonId,
                   remark = :remark,
                   recorded_by_user_id = :recordedByUserId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND attendance_date = :attendanceDate AND deleted_at IS NULL
               AND row_version = :expected
            RETURNING id, tenant_id, leave_request_id, sheet_id, student_id, status, attendance_type_id,
                      absence_reason_id, remark, recorded_by_user_id, attendance_date, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    /**
     * The first marks of many students of one sheet in one statement (JAVA-CODING-RULES §6 "Batch"): the
     * application-generated ids paired with each student and mark.
     */
    private static final String INSERT_MARKS = """
            INSERT INTO attendance.attendance_record
            (id, tenant_id, sheet_id, student_id, status, attendance_type_id, absence_reason_id, remark,
             recorded_by_user_id, attendance_date, created_at, created_by, updated_at, updated_by, row_version)
            SELECT mark.id, :tenantId, :sheetId, mark.student_id, mark.status, mark.attendance_type_id,
                   mark.absence_reason_id, mark.remark, :actor, :attendanceDate, :now, :actor, :now, :actor, 1
              FROM unnest(CAST(:ids AS uuid[]), CAST(:studentIds AS uuid[]), CAST(:statuses AS varchar[]),
                          CAST(:attendanceTypeIds AS uuid[]), CAST(:absenceReasonIds AS uuid[]),
                          CAST(:remarks AS varchar[]))
                   AS mark (id, student_id, status, attendance_type_id, absence_reason_id, remark)
            RETURNING id, tenant_id, leave_request_id, sheet_id, student_id, status, attendance_type_id,
                      absence_reason_id, remark, recorded_by_user_id, attendance_date, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String FIND_MARK_TYPES = """
            SELECT id, institution_id, counts_present, counts_absent, counts_excused, counts_late
              FROM attendance.attendance_type
             WHERE tenant_id = :tenantId
               AND deleted_at IS NULL AND is_active AND id = ANY(:ids)
            """;
    private static final String FIND_ACTIVE_ABSENCE_REASONS = """
            SELECT id
              FROM attendance.absence_reason
             WHERE tenant_id = :tenantId
               AND deleted_at IS NULL AND is_active AND id = ANY(:ids)
            """;

    private final JdbcClient jdbc;

    public AttendanceRecordRepository(@Qualifier(AttendanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /**
     * A configured attendance mark as the workflow checks it (a projection, JAVA-CODING-RULES §5.4): where it may be
     * used and which normalised status it stands for.
     */
    public record MarkType(UUID id, UUID institutionId, boolean countsPresent, boolean countsAbsent,
                           boolean countsExcused, boolean countsLate) {

        /** Whether this mark may be stored with the normalised status. */
        public boolean standsFor(String status) {
            return switch (status) {
                case "present" -> countsPresent;
                case "absent" -> countsAbsent;
                case "excused" -> countsExcused;
                case "late" -> countsLate;
                default -> false;
            };
        }
    }

    public List<AttendanceRecordEntity> findBySheet(UUID sheetId, LocalDate attendanceDate) {
        return jdbc.sql(FIND_BY_SHEET).param("sheetId", sheetId).param("attendanceDate", attendanceDate)
                .query(AttendanceRecordRepository::map).list();
    }

    /**
     * Stores the first marks of these students on one sheet, all in one statement; every entity carries its own id,
     * and the sheet, date, tenant, actor and time are those of the first (they are one call's marks of one sheet).
     */
    public List<AttendanceRecordEntity> insertMarks(List<AttendanceRecordEntity> marks) {
        if (marks.isEmpty()) {
            return List.of();
        }
        AttendanceRecordEntity first = marks.get(0);
        return jdbc.sql(INSERT_MARKS)
                .param("tenantId", first.getTenantId())
                .param("sheetId", first.getSheetId())
                .param("attendanceDate", first.getAttendanceDate())
                .param("actor", first.getCreatedBy())
                .param("now", first.getCreatedAt())
                .param("ids", marks.stream().map(AttendanceRecordEntity::getId).toArray(UUID[]::new))
                .param("studentIds", marks.stream().map(AttendanceRecordEntity::getStudentId).toArray(UUID[]::new))
                .param("statuses", marks.stream().map(AttendanceRecordEntity::getStatus).toArray(String[]::new))
                .param("attendanceTypeIds",
                        marks.stream().map(AttendanceRecordEntity::getAttendanceTypeId).toArray(UUID[]::new))
                .param("absenceReasonIds",
                        marks.stream().map(AttendanceRecordEntity::getAbsenceReasonId).toArray(UUID[]::new))
                .param("remarks", marks.stream().map(AttendanceRecordEntity::getRemark).toArray(String[]::new))
                .query(AttendanceRecordRepository::map).list();
    }

    /**
     * Sets the mark of an existing record under the version it was read with and returns the row as stored; empty
     * when the row changed (or vanished) since.
     */
    public Optional<AttendanceRecordEntity> updateMark(AttendanceRecordEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE_MARK)
                .param("expected", expectedVersion)
                .param("id", e.getId())
                .param("attendanceDate", e.getAttendanceDate())
                .param("status", e.getStatus())
                .param("attendanceTypeId", e.getAttendanceTypeId())
                .param("absenceReasonId", e.getAbsenceReasonId())
                .param("remark", e.getRemark())
                .param("recordedByUserId", e.getRecordedByUserId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .query(AttendanceRecordRepository::map).optional();
    }

    /** The active marks among these ids, by id. */
    public Map<UUID, MarkType> findMarkTypes(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return jdbc.sql(FIND_MARK_TYPES).param("ids", ids.toArray(UUID[]::new))
                .query((rs, row) -> new MarkType(rs.getObject("id", UUID.class),
                        rs.getObject("institution_id", UUID.class), rs.getBoolean("counts_present"),
                        rs.getBoolean("counts_absent"), rs.getBoolean("counts_excused"),
                        rs.getBoolean("counts_late")))
                .list().stream().collect(Collectors.toMap(MarkType::id, Function.identity()));
    }

    /** The active absence reasons among these ids. */
    public Set<UUID> findActiveAbsenceReasons(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(jdbc.sql(FIND_ACTIVE_ABSENCE_REASONS).param("ids", ids.toArray(UUID[]::new))
                .query(UUID.class).list());
    }

    public Optional<AttendanceRecordEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AttendanceRecordRepository::map).optional();
    }

    public Page<AttendanceRecordEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AttendanceRecordEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(AttendanceRecordRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), AttendanceRecordEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public AttendanceRecordEntity insert(AttendanceRecordEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("sheetId", e.getSheetId())
                .param("studentId", e.getStudentId())
                .param("status", e.getStatus())
                .param("attendanceTypeId", e.getAttendanceTypeId())
                .param("absenceReasonId", e.getAbsenceReasonId())
                .param("remark", e.getRemark())
                .param("recordedByUserId", e.getRecordedByUserId())
                .param("attendanceDate", e.getAttendanceDate())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(AttendanceRecordRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(AttendanceRecordEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(AttendanceRecordEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_attendance_record_1: sheet_id, student_id, attendance_date where (deleted_at IS NULL). */
    public boolean takenSheetIdStudentIdAttendanceDate1(AttendanceRecordEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_ATTENDANCE_RECORD_1_TAKEN)
                .param("sheetId", e.getSheetId())
                .param("studentId", e.getStudentId())
                .param("attendanceDate", e.getAttendanceDate())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced attendance.absence_reason row exists (live). */
    public boolean existsAbsenceReasonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ABSENCE_REASON_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced attendance.attendance_type row exists (live). */
    public boolean existsAttendanceTypeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ATTENDANCE_TYPE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced attendance.attendance_sheet row exists (live). */
    public boolean existsSheetId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_SHEET_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through sheet_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.sheet_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, AttendanceRecordEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static AttendanceRecordEntity map(ResultSet rs, int row) throws SQLException {
        return AttendanceRecordEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .leaveRequestId(rs.getObject("leave_request_id", UUID.class))
                .sheetId(rs.getObject("sheet_id", UUID.class))
                .studentId(rs.getObject("student_id", UUID.class))
                .status(rs.getString("status"))
                .attendanceTypeId(rs.getObject("attendance_type_id", UUID.class))
                .absenceReasonId(rs.getObject("absence_reason_id", UUID.class))
                .remark(rs.getString("remark"))
                .recordedByUserId(rs.getObject("recorded_by_user_id", UUID.class))
                .attendanceDate(rs.getObject("attendance_date", LocalDate.class))
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
