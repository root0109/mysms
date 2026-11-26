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
import com.mysms.hrms.hr.domain.LeaveBalanceEntity;
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
 * Plain-JDBC access to hr.leave_balance: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class LeaveBalanceRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("staffId", "r.staff_id", ValueType.UUID),
            field("leaveTypeId", "r.leave_type_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("entitledDays", "r.entitled_days", ValueType.DECIMAL),
            field("usedDays", "r.used_days", ValueType.DECIMAL),
            field("carriedForwardDays", "r.carried_forward_days", ValueType.DECIMAL),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<LeaveBalanceEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", LeaveBalanceEntity::getCreatedAt),
            Map.entry("updateTime", LeaveBalanceEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.staff_id, r.leave_type_id, r.academic_year_id, r.entitled_days, r.used_days,
                   r.carried_forward_days, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM hr.leave_balance r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO hr.leave_balance
            (id, tenant_id, staff_id, leave_type_id, academic_year_id, entitled_days, carried_forward_days,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :staffId, :leaveTypeId, :academicYearId, :entitledDays,
                    COALESCE(:carriedForwardDays, 0), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, staff_id, leave_type_id, academic_year_id, entitled_days, used_days,
                      carried_forward_days, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE hr.leave_balance
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE hr.leave_balance
               SET staff_id = :staffId,
                   leave_type_id = :leaveTypeId,
                   academic_year_id = :academicYearId,
                   entitled_days = :entitledDays,
                   carried_forward_days = :carriedForwardDays,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_LEAVE_BALANCE_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM hr.leave_balance
                            WHERE tenant_id = :tenantId
                              AND staff_id = :staffId AND leave_type_id = :leaveTypeId AND academic_year_id =
                                  :academicYearId AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_LEAVE_TYPE_ID = """
            SELECT EXISTS (SELECT 1 FROM hr.leave_type WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_STAFF_ID = """
            SELECT EXISTS (SELECT 1 FROM hr.staff WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM hr.staff p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.staff_id IN (SELECT p0.id FROM hr.staff p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public LeaveBalanceRepository(@Qualifier(HrDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<LeaveBalanceEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(LeaveBalanceRepository::map).optional();
    }

    public Page<LeaveBalanceEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<LeaveBalanceEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(LeaveBalanceRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), LeaveBalanceEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public LeaveBalanceEntity insert(LeaveBalanceEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("staffId", e.getStaffId())
                .param("leaveTypeId", e.getLeaveTypeId())
                .param("academicYearId", e.getAcademicYearId())
                .param("entitledDays", e.getEntitledDays())
                .param("carriedForwardDays", e.getCarriedForwardDays())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(LeaveBalanceRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(LeaveBalanceEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("staffId", e.getStaffId())
                .param("leaveTypeId", e.getLeaveTypeId())
                .param("academicYearId", e.getAcademicYearId())
                .param("entitledDays", e.getEntitledDays())
                .param("carriedForwardDays", e.getCarriedForwardDays())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(LeaveBalanceEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_leave_balance_1: staff_id, leave_type_id, academic_year_id where (deleted_at IS NULL). */
    public boolean takenStaffIdLeaveTypeIdAcademicYearId1(LeaveBalanceEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_LEAVE_BALANCE_1_TAKEN)
                .param("staffId", e.getStaffId())
                .param("leaveTypeId", e.getLeaveTypeId())
                .param("academicYearId", e.getAcademicYearId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced hr.leave_type row exists (live). */
    public boolean existsLeaveTypeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LEAVE_TYPE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced hr.staff row exists (live). */
    public boolean existsStaffId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_STAFF_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through staff_id (empty: a tenant-level row, or none). */
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

    private static List<Object> sortValues(SortSpec sort, LeaveBalanceEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static LeaveBalanceEntity map(ResultSet rs, int row) throws SQLException {
        return LeaveBalanceEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .staffId(rs.getObject("staff_id", UUID.class))
                .leaveTypeId(rs.getObject("leave_type_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .entitledDays(rs.getBigDecimal("entitled_days"))
                .usedDays(rs.getBigDecimal("used_days"))
                .carriedForwardDays(rs.getBigDecimal("carried_forward_days"))
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
