package com.mysms.hr.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.hr.config.HrDatabaseConfig;
import com.mysms.hr.domain.LeaveTypeEntity;
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
 * Plain-JDBC access to hr.leave_type (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live rows
 * only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class LeaveTypeRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("code", "r.code", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("name", "r.name", ValueType.STRING),
            field("maxDaysPerYear", "r.max_days_per_year", ValueType.DECIMAL),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("code", "r.code", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<LeaveTypeEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", LeaveTypeEntity::getCreatedAt),
            Map.entry("updateTime", LeaveTypeEntity::getUpdatedAt),
            Map.entry("code", LeaveTypeEntity::getCode),
            Map.entry("sortOrder", LeaveTypeEntity::getSortOrder),
            Map.entry("name", LeaveTypeEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.code, r.sort_order, r.institution_id, r.academic_year_id, r.name,
                   r.max_days_per_year, r.is_active, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM hr.leave_type r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO hr.leave_type
            (id, tenant_id, code, sort_order, institution_id, academic_year_id, name, max_days_per_year, is_active,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :code, COALESCE(:sortOrder, 0), :institutionId, :academicYearId, :name,
                    :maxDaysPerYear, COALESCE(:isActive, true), :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, code, sort_order, institution_id, academic_year_id, name, max_days_per_year,
                      is_active, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE hr.leave_type
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE hr.leave_type
               SET code = :code,
                   sort_order = :sortOrder,
                   institution_id = :institutionId,
                   academic_year_id = :academicYearId,
                   name = :name,
                   max_days_per_year = :maxDaysPerYear,
                   is_active = :isActive,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_LEAVE_TYPE_SCOPE_CODE_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM hr.leave_type
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND code = :code AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String REFERENCED_BY_LEAVE_APPLICATION_LEAVE_TYPE_ID = """
            SELECT EXISTS (SELECT 1 FROM hr.leave_application WHERE tenant_id = :tenantId
                AND leave_type_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_LEAVE_BALANCE_LEAVE_TYPE_ID = """
            SELECT EXISTS (SELECT 1 FROM hr.leave_balance WHERE tenant_id = :tenantId
                AND leave_type_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public LeaveTypeRepository(@Qualifier(HrDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<LeaveTypeEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(LeaveTypeRepository::map).optional();
    }

    public Page<LeaveTypeEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<LeaveTypeEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(LeaveTypeRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), LeaveTypeEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public LeaveTypeEntity insert(LeaveTypeEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("code", e.getCode())
                .param("sortOrder", e.getSortOrder())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("name", e.getName())
                .param("maxDaysPerYear", e.getMaxDaysPerYear())
                .param("isActive", e.getIsActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(LeaveTypeRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(LeaveTypeEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("code", e.getCode())
                .param("sortOrder", e.getSortOrder())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("name", e.getName())
                .param("maxDaysPerYear", e.getMaxDaysPerYear())
                .param("isActive", e.getIsActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(LeaveTypeEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_leave_type_scope_code: institution_id, code where (deleted_at IS NULL). */
    public boolean takenInstitutionIdCodeCode(LeaveTypeEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_LEAVE_TYPE_SCOPE_CODE_TAKEN)
                .param("institutionId", e.getInstitutionId())
                .param("code", e.getCode())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live hr.leave_application rows still reference the row. */
    public boolean referencedByLeaveApplicationLeaveTypeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LEAVE_APPLICATION_LEAVE_TYPE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live hr.leave_balance rows still reference the row. */
    public boolean referencedByLeaveBalanceLeaveTypeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LEAVE_BALANCE_LEAVE_TYPE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, LeaveTypeEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static LeaveTypeEntity map(ResultSet rs, int row) throws SQLException {
        return LeaveTypeEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .code(rs.getString("code"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .name(rs.getString("name"))
                .maxDaysPerYear(rs.getBigDecimal("max_days_per_year"))
                .isActive(rs.getObject("is_active", Boolean.class))
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
