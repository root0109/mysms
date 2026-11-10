package com.mysms.finance.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.domain.CouponEntity;
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
 * Plain-JDBC access to finance.coupon (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live
 * rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class CouponRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("code", "r.code", ValueType.STRING),
            field("discountType", "r.discount_type", ValueType.STRING),
            field("percentageDiscount", "r.percentage_discount", ValueType.DECIMAL),
            field("fixedDiscount", "r.fixed_discount", ValueType.DECIMAL),
            field("expiresOn", "r.expires_on", ValueType.DATE),
            field("usageLimit", "r.usage_limit", ValueType.INTEGER),
            field("redemptionCount", "r.redemption_count", ValueType.INTEGER),
            field("isEnabled", "r.is_enabled", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("code", "r.code", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<CouponEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", CouponEntity::getCreatedAt),
            Map.entry("updateTime", CouponEntity::getUpdatedAt),
            Map.entry("code", CouponEntity::getCode));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.code, r.discount_type, r.percentage_discount, r.fixed_discount, r.expires_on,
                   r.usage_limit, r.redemption_count, r.is_enabled, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM finance.coupon r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO finance.coupon
            (id, tenant_id, code, discount_type, percentage_discount, fixed_discount, expires_on, usage_limit,
             redemption_count, is_enabled, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :code, :discountType, :percentageDiscount, :fixedDiscount, :expiresOn,
                    :usageLimit, COALESCE(:redemptionCount, 0), COALESCE(:isEnabled, true), :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, code, discount_type, percentage_discount, fixed_discount, expires_on, usage_limit,
                      redemption_count, is_enabled, created_at, created_by, updated_at, updated_by, deleted_at,
                      row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE finance.coupon
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE finance.coupon
               SET code = :code,
                   discount_type = :discountType,
                   percentage_discount = :percentageDiscount,
                   fixed_discount = :fixedDiscount,
                   expires_on = :expiresOn,
                   usage_limit = :usageLimit,
                   redemption_count = :redemptionCount,
                   is_enabled = :isEnabled,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_COUPON_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM finance.coupon
                            WHERE tenant_id = :tenantId
                              AND code = :code AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String REFERENCED_BY_COUPON_SCOPE_COUPON_ID = """
            SELECT EXISTS (SELECT 1 FROM finance.coupon_scope WHERE tenant_id = :tenantId
                AND coupon_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_PAYMENT_COUPON_ID = """
            SELECT EXISTS (SELECT 1 FROM finance.payment WHERE tenant_id = :tenantId
                AND coupon_id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public CouponRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CouponEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(CouponRepository::map).optional();
    }

    public Page<CouponEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<CouponEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(CouponRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), CouponEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public CouponEntity insert(CouponEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("code", e.getCode())
                .param("discountType", e.getDiscountType())
                .param("percentageDiscount", e.getPercentageDiscount())
                .param("fixedDiscount", e.getFixedDiscount())
                .param("expiresOn", e.getExpiresOn())
                .param("usageLimit", e.getUsageLimit())
                .param("redemptionCount", e.getRedemptionCount())
                .param("isEnabled", e.getIsEnabled())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(CouponRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(CouponEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("code", e.getCode())
                .param("discountType", e.getDiscountType())
                .param("percentageDiscount", e.getPercentageDiscount())
                .param("fixedDiscount", e.getFixedDiscount())
                .param("expiresOn", e.getExpiresOn())
                .param("usageLimit", e.getUsageLimit())
                .param("redemptionCount", e.getRedemptionCount())
                .param("isEnabled", e.getIsEnabled())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(CouponEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_coupon_1: code where (deleted_at IS NULL). */
    public boolean takenCode1(CouponEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_COUPON_1_TAKEN).param("code", e.getCode()).param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live finance.coupon_scope rows still reference the row. */
    public boolean referencedByCouponScopeCouponId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_COUPON_SCOPE_COUPON_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live finance.payment rows still reference the row. */
    public boolean referencedByPaymentCouponId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PAYMENT_COUPON_ID).param("id", id).query(Boolean.class)
                .single());
    }

    private static List<Object> sortValues(SortSpec sort, CouponEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static CouponEntity map(ResultSet rs, int row) throws SQLException {
        return CouponEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .code(rs.getString("code"))
                .discountType(rs.getString("discount_type"))
                .percentageDiscount(rs.getBigDecimal("percentage_discount"))
                .fixedDiscount(rs.getBigDecimal("fixed_discount"))
                .expiresOn(rs.getObject("expires_on", LocalDate.class))
                .usageLimit(rs.getObject("usage_limit", Integer.class))
                .redemptionCount(rs.getObject("redemption_count", Integer.class))
                .isEnabled(rs.getObject("is_enabled", Boolean.class))
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
