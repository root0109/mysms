package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.domain.RetentionRunEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access to privacy.retention_run (no soft delete: runs are evidence), read with the policy's code and data
 * category. One run per tenant, policy and cutoff: a repeated run of the day inserts nothing. Every statement filters
 * the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class RetentionRunRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("policyCode", "p.code", ValueType.STRING),
            field("categoryCode", "p.category_code", ValueType.STRING),
            field("module", "r.module", ValueType.STRING),
            field("status", "r.status", ValueType.STRING),
            field("startedAt", "r.started_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("startedAt", "r.started_at", ValueType.TIMESTAMP),
            field("policyCode", "p.code", ValueType.STRING));

    /** The newest runs first. */
    public static final String DEFAULT_ORDER = "startedAt,desc";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<RetentionRunView, Object>> SORT_VALUES = Map.of(
            "startedAt", RetentionRunView::startedAt,
            "policyCode", RetentionRunView::policyCode);

    private static final String SELECT = """
            SELECT r.id, p.code AS policy_code, p.category_code, r.module, r.action, r.cutoff, r.status, r.affected,
                   r.held, r.started_at, r.completed_at
              FROM privacy.retention_run r
              JOIN privacy.retention_policy p ON p.id = r.policy_id
             WHERE r.tenant_id = :tenantId
            """;
    private static final String INSERT = """
            INSERT INTO privacy.retention_run
            (id, tenant_id, policy_id, module, action, cutoff, status, started_at, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :policyId, :module, :action, :cutoff, 'dispatched', :startedAt, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            ON CONFLICT (tenant_id, policy_id, cutoff) DO NOTHING
            """;
    private static final String COMPLETE = """
            UPDATE privacy.retention_run
               SET status = :status, affected = :affected, held = :held, completed_at = :now, updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND module = :module AND status = 'dispatched'
            """;
    private static final String EXISTS = """
            SELECT count(*) > 0
              FROM privacy.retention_run
             WHERE tenant_id = :tenantId AND id = :id AND module = :module
            """;

    private final JdbcClient jdbc;

    public RetentionRunRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Page<RetentionRunView> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<RetentionRunView> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(RetentionRunRepository::map).list();
        return page.toPage(rows, v -> sortValues(page.sort(), v), RetentionRunView::id);
    }

    /** Inserts the run unless the policy already ran for this cutoff; whether it was inserted. */
    public boolean insert(RetentionRunEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("policyId", e.getPolicyId())
                .param("module", e.getModule())
                .param("action", e.getAction())
                .param("cutoff", e.getCutoff())
                .param("startedAt", e.getStartedAt())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update() == 1;
    }

    /** A dispatched run of the module finished with the module's counts; 0 when it is not (or no longer) dispatched. */
    public int complete(UUID id, String module, String status, int affected, int held, OffsetDateTime now) {
        return jdbc.sql(COMPLETE).param("id", id).param("module", module).param("status", status)
                .param("affected", affected).param("held", held).param("now", now).update();
    }

    /** Whether the tenant has the run, sent to the module. */
    public boolean exists(UUID id, String module) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS).param("id", id).param("module", module)
                .query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, RetentionRunView view) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(view)).toList();
    }

    private static RetentionRunView map(ResultSet rs, int row) throws SQLException {
        return new RetentionRunView(
                rs.getObject("id", UUID.class),
                rs.getString("policy_code"),
                rs.getString("category_code"),
                rs.getString("module"),
                rs.getString("action"),
                rs.getObject("cutoff", OffsetDateTime.class),
                rs.getString("status"),
                rs.getObject("affected", Integer.class),
                rs.getObject("held", Integer.class),
                rs.getObject("started_at", OffsetDateTime.class),
                rs.getObject("completed_at", OffsetDateTime.class));
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
