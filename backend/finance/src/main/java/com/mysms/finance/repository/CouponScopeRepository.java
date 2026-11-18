package com.mysms.finance.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.domain.CouponScopeEntity;
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
 * Plain-JDBC access to finance.coupon_scope: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class CouponScopeRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("couponId", "r.coupon_id", ValueType.UUID),
            field("lmsCourseId", "r.lms_course_id", ValueType.UUID),
            field("lmsCohortId", "r.lms_cohort_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<CouponScopeEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", CouponScopeEntity::getCreatedAt),
            Map.entry("updateTime", CouponScopeEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.coupon_id, r.lms_course_id, r.lms_cohort_id, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM finance.coupon_scope r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO finance.coupon_scope
            (id, tenant_id, coupon_id, lms_course_id, lms_cohort_id, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :couponId, :lmsCourseId, :lmsCohortId, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, coupon_id, lms_course_id, lms_cohort_id, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE finance.coupon_scope
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE finance.coupon_scope
               SET lms_course_id = :lmsCourseId,
                   lms_cohort_id = :lmsCohortId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_COUPON_ID = """
            SELECT EXISTS (SELECT 1 FROM finance.coupon WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public CouponScopeRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CouponScopeEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(CouponScopeRepository::map).optional();
    }

    public Page<CouponScopeEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<CouponScopeEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(CouponScopeRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), CouponScopeEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public CouponScopeEntity insert(CouponScopeEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("couponId", e.getCouponId())
                .param("lmsCourseId", e.getLmsCourseId())
                .param("lmsCohortId", e.getLmsCohortId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(CouponScopeRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(CouponScopeEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("lmsCourseId", e.getLmsCourseId())
                .param("lmsCohortId", e.getLmsCohortId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(CouponScopeEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced finance.coupon row exists (live). */
    public boolean existsCouponId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COUPON_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.coupon_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, CouponScopeEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static CouponScopeEntity map(ResultSet rs, int row) throws SQLException {
        return CouponScopeEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .couponId(rs.getObject("coupon_id", UUID.class))
                .lmsCourseId(rs.getObject("lms_course_id", UUID.class))
                .lmsCohortId(rs.getObject("lms_cohort_id", UUID.class))
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
