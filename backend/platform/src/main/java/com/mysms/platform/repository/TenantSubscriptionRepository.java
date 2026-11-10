package com.mysms.platform.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.domain.TenantSubscriptionEntity;
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
 * Plain-JDBC access to platform.tenant_subscription (JAVA-CODING-RULES §6): schema-qualified constant SQL, named
 * parameters, live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class TenantSubscriptionRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("maxMembers", "r.max_members", ValueType.INTEGER),
            field("price", "r.price", ValueType.DECIMAL),
            field("currencyId", "r.currency_id", ValueType.UUID),
            field("billingCycleMonths", "r.billing_cycle_months", ValueType.INTEGER),
            field("gatewayName", "r.gateway_name", ValueType.STRING),
            field("gatewayTxnRef", "r.gateway_txn_ref", ValueType.STRING),
            field("amountPaid", "r.amount_paid", ValueType.DECIMAL),
            field("gatewayStatus", "r.gateway_status", ValueType.STRING),
            field("paidAt", "r.paid_at", ValueType.TIMESTAMP),
            field("payerName", "r.payer_name", ValueType.STRING),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("planId", "r.plan_id", ValueType.UUID),
            field("requestedByUserId", "r.requested_by_user_id", ValueType.UUID),
            field("status", "r.status", ValueType.STRING),
            field("startDate", "r.start_date", ValueType.DATE),
            field("endDate", "r.end_date", ValueType.DATE),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<TenantSubscriptionEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", TenantSubscriptionEntity::getCreatedAt),
            Map.entry("updateTime", TenantSubscriptionEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.max_members, r.price, r.currency_id, r.billing_cycle_months, r.gateway_name,
                   r.gateway_txn_ref, r.amount_paid, r.gateway_status, r.paid_at, r.payer_name, r.institution_id,
                   r.plan_id, r.requested_by_user_id, r.status, r.start_date, r.end_date, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM platform.tenant_subscription r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO platform.tenant_subscription
            (id, tenant_id, institution_id, plan_id, requested_by_user_id, start_date, end_date, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :planId, :requestedByUserId, :startDate, :endDate, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, max_members, price, currency_id, billing_cycle_months, gateway_name,
                      gateway_txn_ref, amount_paid, gateway_status, paid_at, payer_name, institution_id, plan_id,
                      requested_by_user_id, status, start_date, end_date, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE platform.tenant_subscription
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE platform.tenant_subscription
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_PLAN_ID = """
            SELECT EXISTS (SELECT 1 FROM platform.saas_plan WHERE id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public TenantSubscriptionRepository(@Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<TenantSubscriptionEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(TenantSubscriptionRepository::map).optional();
    }

    public Page<TenantSubscriptionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<TenantSubscriptionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(TenantSubscriptionRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), TenantSubscriptionEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public TenantSubscriptionEntity insert(TenantSubscriptionEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("planId", e.getPlanId())
                .param("requestedByUserId", e.getRequestedByUserId())
                .param("startDate", e.getStartDate())
                .param("endDate", e.getEndDate())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(TenantSubscriptionRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(TenantSubscriptionEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(TenantSubscriptionEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced platform.saas_plan row exists (live). */
    public boolean existsPlanId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PLAN_ID).param("id", id).query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, TenantSubscriptionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static TenantSubscriptionEntity map(ResultSet rs, int row) throws SQLException {
        return TenantSubscriptionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .maxMembers(rs.getObject("max_members", Integer.class))
                .price(rs.getBigDecimal("price"))
                .currencyId(rs.getObject("currency_id", UUID.class))
                .billingCycleMonths(rs.getObject("billing_cycle_months", Integer.class))
                .gatewayName(rs.getString("gateway_name"))
                .gatewayTxnRef(rs.getString("gateway_txn_ref"))
                .amountPaid(rs.getBigDecimal("amount_paid"))
                .gatewayStatus(rs.getString("gateway_status"))
                .paidAt(rs.getObject("paid_at", OffsetDateTime.class))
                .payerName(rs.getString("payer_name"))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .planId(rs.getObject("plan_id", UUID.class))
                .requestedByUserId(rs.getObject("requested_by_user_id", UUID.class))
                .status(rs.getString("status"))
                .startDate(rs.getObject("start_date", LocalDate.class))
                .endDate(rs.getObject("end_date", LocalDate.class))
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
