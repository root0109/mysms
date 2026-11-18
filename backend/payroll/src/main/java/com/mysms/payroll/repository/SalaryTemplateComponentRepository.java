package com.mysms.payroll.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.payroll.config.PayrollDatabaseConfig;
import com.mysms.payroll.domain.SalaryTemplateComponentEntity;
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
 * Plain-JDBC access to payroll.salary_template_component: live rows only, keyset paging, and the row_version guard on
 * every update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class SalaryTemplateComponentRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("salaryTemplateId", "r.salary_template_id", ValueType.UUID),
            field("payComponentId", "r.pay_component_id", ValueType.UUID),
            field("payCategoryId", "r.pay_category_id", ValueType.UUID),
            field("value", "r.value", ValueType.DECIMAL),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<SalaryTemplateComponentEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", SalaryTemplateComponentEntity::getCreatedAt),
            Map.entry("updateTime", SalaryTemplateComponentEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.salary_template_id, r.pay_component_id, r.pay_category_id, r.value,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM payroll.salary_template_component r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO payroll.salary_template_component
            (id, tenant_id, salary_template_id, pay_component_id, pay_category_id, value, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :salaryTemplateId, :payComponentId, :payCategoryId, :value, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, salary_template_id, pay_component_id, pay_category_id, value, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE payroll.salary_template_component
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE payroll.salary_template_component
               SET pay_component_id = :payComponentId,
                   pay_category_id = :payCategoryId,
                   value = :value,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_SALARY_TEMPLATE_COMPONENT_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM payroll.salary_template_component
                            WHERE tenant_id = :tenantId
                              AND salary_template_id = :salaryTemplateId AND pay_component_id = :payComponentId AND id
                                  <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_PAY_CATEGORY_ID = """
            SELECT EXISTS (SELECT 1 FROM payroll.pay_category WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_PAY_COMPONENT_ID = """
            SELECT EXISTS (SELECT 1 FROM payroll.pay_component WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_SALARY_TEMPLATE_ID = """
            SELECT EXISTS (SELECT 1 FROM payroll.salary_template WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_STAFF_SALARY_COMPONENT_SALARY_TEMPLATE_COMPONENT_ID = """
            SELECT EXISTS (SELECT 1 FROM payroll.staff_salary_component WHERE tenant_id = :tenantId
                AND salary_template_component_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM payroll.salary_template p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.salary_template_id IN (SELECT p0.id FROM payroll.salary_template p0 WHERE p0.tenant_id =
                 :tenantId
                 AND
             (p0.institution_id = ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public SalaryTemplateComponentRepository(@Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<SalaryTemplateComponentEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(SalaryTemplateComponentRepository::map).optional();
    }

    public Page<SalaryTemplateComponentEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<SalaryTemplateComponentEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(SalaryTemplateComponentRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), SalaryTemplateComponentEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public SalaryTemplateComponentEntity insert(SalaryTemplateComponentEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("salaryTemplateId", e.getSalaryTemplateId())
                .param("payComponentId", e.getPayComponentId())
                .param("payCategoryId", e.getPayCategoryId())
                .param("value", e.getValue())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(SalaryTemplateComponentRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(SalaryTemplateComponentEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("payComponentId", e.getPayComponentId())
                .param("payCategoryId", e.getPayCategoryId())
                .param("value", e.getValue())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(SalaryTemplateComponentEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_salary_template_component_1: salary_template_id, pay_component_id where (deleted_at IS NULL). */
    public boolean takenSalaryTemplateIdPayComponentId1(SalaryTemplateComponentEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_SALARY_TEMPLATE_COMPONENT_1_TAKEN)
                .param("salaryTemplateId", e.getSalaryTemplateId())
                .param("payComponentId", e.getPayComponentId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced payroll.pay_category row exists (live). */
    public boolean existsPayCategoryId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PAY_CATEGORY_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced payroll.pay_component row exists (live). */
    public boolean existsPayComponentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PAY_COMPONENT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced payroll.salary_template row exists (live). */
    public boolean existsSalaryTemplateId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_SALARY_TEMPLATE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live payroll.staff_salary_component rows still reference the row. */
    public boolean referencedByStaffSalaryComponentSalaryTemplateComponentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_STAFF_SALARY_COMPONENT_SALARY_TEMPLATE_COMPONENT_ID)
                .param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through salary_template_id (empty: a tenant-level row, or none). */
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

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.salary_template_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, SalaryTemplateComponentEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static SalaryTemplateComponentEntity map(ResultSet rs, int row) throws SQLException {
        return SalaryTemplateComponentEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .salaryTemplateId(rs.getObject("salary_template_id", UUID.class))
                .payComponentId(rs.getObject("pay_component_id", UUID.class))
                .payCategoryId(rs.getObject("pay_category_id", UUID.class))
                .value(rs.getBigDecimal("value"))
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
