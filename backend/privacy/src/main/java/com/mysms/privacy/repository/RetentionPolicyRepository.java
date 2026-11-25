package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import java.sql.ResultSet;
import java.sql.SQLException;
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
 * Plain-JDBC reads of the retention catalogue (privacy.retention_policy, global, seeded by the baseline) with the
 * caller's tenant's setting of each policy (privacy.tenant_retention_policy, joined on the tenant).
 */
@Repository
public class RetentionPolicyRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "p.id", ValueType.UUID),
            field("code", "p.code", ValueType.STRING),
            field("categoryCode", "p.category_code", ValueType.STRING),
            field("module", "p.module", ValueType.STRING),
            field("trigger", "p.trigger_code", ValueType.STRING),
            field("action", "COALESCE(t.action, p.action)", ValueType.STRING),
            field("overridden", "(t.id IS NOT NULL)", ValueType.BOOLEAN));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("code", "p.code", ValueType.STRING),
            field("module", "p.module", ValueType.STRING));

    public static final String DEFAULT_ORDER = "code";
    public static final String ID_COLUMN = "p.id";

    private static final Map<String, Function<RetentionPolicyView, Object>> SORT_VALUES = Map.of(
            "code", RetentionPolicyView::code,
            "module", RetentionPolicyView::module);

    private static final String SELECT = """
            SELECT p.id, p.code, p.category_code, p.module, p.trigger_code, p.minimum_period_days,
                   p.notify_before_days, p.legal_reference, COALESCE(t.period_days, p.period_days) AS period_days,
                   COALESCE(t.action, p.action) AS action, t.id IS NOT NULL AS overridden,
                   COALESCE(t.row_version, 0) AS row_version
              FROM privacy.retention_policy p
              LEFT JOIN privacy.tenant_retention_policy t ON t.policy_id = p.id AND t.tenant_id = :tenantId
            """;
    private static final String FIND_BY_ID = SELECT + " WHERE p.id = :id";
    private static final String ALL = SELECT + " ORDER BY p.code";

    private final JdbcClient jdbc;

    public RetentionPolicyRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<RetentionPolicyView> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(RetentionPolicyRepository::map).optional();
    }

    /** Every policy with the period and action in force for the tenant, by code (the daily retention run). */
    public List<RetentionPolicyView> all() {
        return jdbc.sql(ALL).query(RetentionPolicyRepository::map).list();
    }

    public Page<RetentionPolicyView> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT + " WHERE " + where.sql() + " ORDER BY " + page.orderByClause() + page.limitClause();
        List<RetentionPolicyView> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(RetentionPolicyRepository::map).list();
        return page.toPage(rows, v -> sortValues(page.sort(), v), RetentionPolicyView::id);
    }

    private static List<Object> sortValues(SortSpec sort, RetentionPolicyView view) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(view)).toList();
    }

    private static RetentionPolicyView map(ResultSet rs, int row) throws SQLException {
        return new RetentionPolicyView(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getString("category_code"),
                rs.getString("module"),
                rs.getString("trigger_code"),
                rs.getInt("period_days"),
                rs.getInt("minimum_period_days"),
                rs.getString("action"),
                rs.getObject("notify_before_days", Integer.class),
                rs.getString("legal_reference"),
                rs.getBoolean("overridden"),
                rs.getInt("row_version"));
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
