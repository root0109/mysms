package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.model.AccessSummarySnapshot;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plain-JDBC reads of the processing register (privacy.processing_activity with its purpose and data categories):
 * a global catalogue, the same for every tenant, so no statement filters a tenant.
 */
@Repository
public class ProcessingActivityRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "a.id", ValueType.UUID),
            field("module", "a.module", ValueType.STRING),
            field("code", "a.code", ValueType.STRING),
            field("purposeCode", "p.code", ValueType.STRING),
            field("legalBasis", "a.legal_basis", ValueType.STRING),
            field("targetsMinors", "a.targets_minors", ValueType.BOOLEAN),
            field("crossBorder", "a.cross_border", ValueType.BOOLEAN));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("code", "a.code", ValueType.STRING),
            field("module", "a.module", ValueType.STRING));

    public static final String DEFAULT_ORDER = "code";
    public static final String ID_COLUMN = "a.id";

    private static final Map<String, Function<ProcessingActivityView, Object>> SORT_VALUES = Map.of(
            "code", ProcessingActivityView::code, "module", ProcessingActivityView::module);

    private static final String SELECT = """
            SELECT a.id, a.module, a.code, p.code AS purpose_code, a.legal_basis, a.education_exemption_clause,
                   a.targets_minors, a.cross_border, a.retention_policy_code, a.version,
                   ARRAY(SELECT c.code
                           FROM privacy.processing_activity_category ac
                           JOIN privacy.data_category c ON c.id = ac.category_id
                          WHERE ac.activity_id = a.id
                          ORDER BY c.code) AS categories
              FROM privacy.processing_activity a
              JOIN privacy.purpose p ON p.id = a.purpose_id
            """;

    /** The whole register for its export, by activity code. */
    private static final String REGISTER = """
            SELECT a.code, a.module, p.code AS purpose_code, a.legal_basis, a.education_exemption_clause,
                   a.targets_minors, a.audiences, a.recipients::text AS recipients, a.cross_border,
                   a.retention_policy_code,
                   ARRAY(SELECT c.code
                           FROM privacy.processing_activity_category ac
                           JOIN privacy.data_category c ON c.id = ac.category_id
                          WHERE ac.activity_id = a.id
                          ORDER BY c.code) AS categories
              FROM privacy.processing_activity a
              JOIN privacy.purpose p ON p.id = a.purpose_id
             ORDER BY a.code
            """;

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public ProcessingActivityRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper json) {
        this.jdbc = database.jdbc();
        this.json = json;
    }

    /** Every activity of the register, by code. */
    public List<RegisterEntryView> register() {
        return jdbc.sql(REGISTER).query(this::entry).list();
    }

    public Page<ProcessingActivityView> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT + " WHERE " + where.sql() + " ORDER BY " + page.orderByClause() + page.limitClause();
        List<ProcessingActivityView> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ProcessingActivityRepository::map).list();
        return page.toPage(rows, v -> sortValues(page.sort(), v), ProcessingActivityView::id);
    }

    private static List<Object> sortValues(SortSpec sort, ProcessingActivityView v) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(v)).toList();
    }

    private static ProcessingActivityView map(ResultSet rs, int row) throws SQLException {
        return new ProcessingActivityView(
                rs.getObject("id", UUID.class),
                rs.getString("module"),
                rs.getString("code"),
                rs.getString("purpose_code"),
                rs.getString("legal_basis"),
                rs.getString("education_exemption_clause"),
                rs.getBoolean("targets_minors"),
                rs.getBoolean("cross_border"),
                rs.getString("retention_policy_code"),
                rs.getInt("version"),
                codes(rs.getArray("categories")));
    }

    private RegisterEntryView entry(ResultSet rs, int row) throws SQLException {
        return new RegisterEntryView(
                rs.getString("code"),
                rs.getString("module"),
                rs.getString("purpose_code"),
                rs.getString("legal_basis"),
                rs.getString("education_exemption_clause"),
                rs.getBoolean("targets_minors"),
                codes(rs.getArray("audiences")),
                codes(rs.getArray("categories")),
                List.of(json.readValue(rs.getString("recipients"), AccessSummarySnapshot.Recipient[].class)),
                rs.getBoolean("cross_border"),
                rs.getString("retention_policy_code"));
    }

    private static List<String> codes(Array array) throws SQLException {
        return List.of((String[]) array.getArray());
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
