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
 * Plain-JDBC reads of the purpose catalogue (privacy.purpose, global) joined with the caller's tenant's settings
 * (privacy.tenant_purpose): every purpose is listed, switched off where the tenant has no row. The join filters the
 * caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class PurposeRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "p.id", ValueType.UUID),
            field("code", "p.code", ValueType.STRING),
            field("scopeKind", "p.scope_kind", ValueType.STRING),
            field("fiduciary", "p.fiduciary", ValueType.STRING),
            field("forbiddenForMinors", "p.forbidden_for_minors", ValueType.BOOLEAN),
            field("majorityGrace", "p.majority_grace", ValueType.STRING),
            field("status", "p.status", ValueType.STRING),
            field("enabled", "COALESCE(t.enabled, false)", ValueType.BOOLEAN),
            field("maxScope", "t.max_scope", ValueType.STRING));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("code", "p.code", ValueType.STRING));

    public static final String DEFAULT_ORDER = "code";
    public static final String ID_COLUMN = "p.id";

    private static final Map<String, Function<PurposeView, Object>> SORT_VALUES = Map.of("code", PurposeView::code);

    private static final String SELECT = """
            SELECT p.id, p.code, p.scope_kind, p.fiduciary, p.forbidden_for_minors, p.majority_grace,
                   p.default_valid_months, p.status, p.version, COALESCE(t.enabled, false) AS enabled, t.max_scope,
                   t.notice_id, t.updated_at, COALESCE(t.row_version, 0) AS row_version
              FROM privacy.purpose p
              LEFT JOIN privacy.tenant_purpose t ON t.purpose_id = p.id AND t.tenant_id = :tenantId
            """;
    private static final String FIND_BY_ID = SELECT + " WHERE p.id = :id";
    private static final String FIND_BY_CODE = SELECT + " WHERE p.code = :code";
    private static final String ENABLED = SELECT + " WHERE COALESCE(t.enabled, false) ORDER BY p.code";

    private final JdbcClient jdbc;

    public PurposeRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PurposeView> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PurposeRepository::map).optional();
    }

    /** The catalogue purpose with the code, with the tenant's setting of it. */
    public Optional<PurposeView> findByCode(String code) {
        return jdbc.sql(FIND_BY_CODE).param("code", code).query(PurposeRepository::map).optional();
    }

    /** The purposes the tenant has switched on, by code. */
    public List<PurposeView> enabled() {
        return jdbc.sql(ENABLED).query(PurposeRepository::map).list();
    }

    public Page<PurposeView> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT + " WHERE " + where.sql() + " ORDER BY " + page.orderByClause() + page.limitClause();
        List<PurposeView> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PurposeRepository::map).list();
        return page.toPage(rows, v -> sortValues(page.sort(), v), PurposeView::id);
    }

    private static List<Object> sortValues(SortSpec sort, PurposeView v) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(v)).toList();
    }

    private static PurposeView map(ResultSet rs, int row) throws SQLException {
        return new PurposeView(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getString("scope_kind"),
                rs.getString("fiduciary"),
                rs.getBoolean("forbidden_for_minors"),
                rs.getString("majority_grace"),
                rs.getObject("default_valid_months", Integer.class),
                rs.getString("status"),
                rs.getInt("version"),
                rs.getBoolean("enabled"),
                rs.getString("max_scope"),
                rs.getObject("notice_id", UUID.class),
                rs.getObject("updated_at", OffsetDateTime.class),
                rs.getInt("row_version"));
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
