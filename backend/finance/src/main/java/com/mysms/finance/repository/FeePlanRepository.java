package com.mysms.finance.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.domain.FeePlanEntity;
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
 * Plain-JDBC access to finance.fee_plan (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live
 * rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class FeePlanRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING),
            field("scheduleBasis", "r.schedule_basis", ValueType.STRING),
            field("offsetDays", "r.offset_days", ValueType.INTEGER),
            field("offsetDirection", "r.offset_direction", ValueType.STRING),
            field("discountPercent", "r.discount_percent", ValueType.DECIMAL),
            field("notes", "r.notes", ValueType.STRING),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<FeePlanEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", FeePlanEntity::getCreatedAt),
            Map.entry("updateTime", FeePlanEntity::getUpdatedAt),
            Map.entry("code", FeePlanEntity::getCode),
            Map.entry("name", FeePlanEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.code, r.name, r.schedule_basis, r.offset_days,
                   r.offset_direction, r.discount_percent, r.notes, r.is_active, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM finance.fee_plan r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO finance.fee_plan
            (id, tenant_id, institution_id, code, name, schedule_basis, offset_days, offset_direction,
             discount_percent, notes, is_active, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :code, :name,
                    COALESCE(:scheduleBasis, 'fixed_days'::character varying), :offsetDays, :offsetDirection,
                    :discountPercent, :notes, COALESCE(:isActive, true), :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, code, name, schedule_basis, offset_days, offset_direction,
                      discount_percent, notes, is_active, created_at, created_by, updated_at, updated_by, deleted_at,
                      row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE finance.fee_plan
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE finance.fee_plan
               SET institution_id = :institutionId,
                   code = :code,
                   name = :name,
                   schedule_basis = :scheduleBasis,
                   offset_days = :offsetDays,
                   offset_direction = :offsetDirection,
                   discount_percent = :discountPercent,
                   notes = :notes,
                   is_active = :isActive,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_FEE_PLAN_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM finance.fee_plan
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND code = :code AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String REFERENCED_BY_COURSE_FEE_DEFAULT_FEE_PLAN_ID = """
            SELECT EXISTS (SELECT 1 FROM finance.course_fee_default WHERE tenant_id = :tenantId
                AND fee_plan_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_FEE_PLAN_INSTALLMENT_FEE_PLAN_ID = """
            SELECT EXISTS (SELECT 1 FROM finance.fee_plan_installment WHERE tenant_id = :tenantId
                AND fee_plan_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_FEE_STRUCTURE_FEE_PLAN_ID = """
            SELECT EXISTS (SELECT 1 FROM finance.fee_structure WHERE tenant_id = :tenantId
                AND fee_plan_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_PROGRAM_ENROLLMENT_FEE_FEE_PLAN_ID = """
            SELECT EXISTS (SELECT 1 FROM finance.program_enrollment_fee WHERE tenant_id = :tenantId
                AND fee_plan_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public FeePlanRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<FeePlanEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(FeePlanRepository::map).optional();
    }

    public Page<FeePlanEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<FeePlanEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(FeePlanRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), FeePlanEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public FeePlanEntity insert(FeePlanEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("scheduleBasis", e.getScheduleBasis())
                .param("offsetDays", e.getOffsetDays())
                .param("offsetDirection", e.getOffsetDirection())
                .param("discountPercent", e.getDiscountPercent())
                .param("notes", e.getNotes())
                .param("isActive", e.getIsActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(FeePlanRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(FeePlanEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("scheduleBasis", e.getScheduleBasis())
                .param("offsetDays", e.getOffsetDays())
                .param("offsetDirection", e.getOffsetDirection())
                .param("discountPercent", e.getDiscountPercent())
                .param("notes", e.getNotes())
                .param("isActive", e.getIsActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(FeePlanEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_fee_plan_1: institution_id, code where (deleted_at IS NULL). */
    public boolean takenInstitutionIdCode1(FeePlanEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_FEE_PLAN_1_TAKEN)
                .param("institutionId", e.getInstitutionId())
                .param("code", e.getCode())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live finance.course_fee_default rows still reference the row. */
    public boolean referencedByCourseFeeDefaultFeePlanId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_COURSE_FEE_DEFAULT_FEE_PLAN_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live finance.fee_plan_installment rows still reference the row. */
    public boolean referencedByFeePlanInstallmentFeePlanId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_FEE_PLAN_INSTALLMENT_FEE_PLAN_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live finance.fee_structure rows still reference the row. */
    public boolean referencedByFeeStructureFeePlanId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_FEE_STRUCTURE_FEE_PLAN_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live finance.program_enrollment_fee rows still reference the row. */
    public boolean referencedByProgramEnrollmentFeeFeePlanId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PROGRAM_ENROLLMENT_FEE_FEE_PLAN_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, FeePlanEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static FeePlanEntity map(ResultSet rs, int row) throws SQLException {
        return FeePlanEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .scheduleBasis(rs.getString("schedule_basis"))
                .offsetDays(rs.getObject("offset_days", Integer.class))
                .offsetDirection(rs.getString("offset_direction"))
                .discountPercent(rs.getBigDecimal("discount_percent"))
                .notes(rs.getString("notes"))
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
