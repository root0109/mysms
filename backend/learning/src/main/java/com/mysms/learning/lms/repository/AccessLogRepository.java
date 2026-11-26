package com.mysms.learning.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC reads of lms.access_log (partitioned by month on {@code accessed_at}): a list always names its time range,
 * so only those partitions are read. Every statement filters the caller's tenant; row-level security is the backstop.
 */
@Repository
public class AccessLogRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("accessedAt", "r.accessed_at", ValueType.TIMESTAMP),
            field("operation", "r.operation", ValueType.STRING),
            field("isAsync", "r.is_async", ValueType.BOOLEAN),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("roleName", "r.role_name", ValueType.STRING),
            field("courseWeek", "r.course_week", ValueType.STRING));

    /** The partition key; NOT NULL, so keyset paging can compare it. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("accessedAt", "r.accessed_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "accessedAt desc";
    public static final String ID_COLUMN = "r.id";
    public static final String RANGE_FIELD = "accessedAt";

    private static final String SELECT = """
            SELECT r.id, r.user_id, r.accessed_at, r.operation, r.url, r.params, r.is_async, r.session_ref,
                   r.cohort_id, r.role_name, r.course_week, r.analytics_link_ref, r.user_agent, r.referer,
                   host(r.ip_address) AS ip_address
              FROM lms.access_log r
             WHERE r.tenant_id = :tenantId
            """;
    private static final String FIND_BY_ID = SELECT + " AND r.id = :id";

    private final JdbcClient jdbc;

    public AccessLogRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AccessLogRow> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AccessLogRepository::map).optional();
    }

    public Page<AccessLogRow> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AccessLogRow> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(AccessLogRepository::map).list();
        return page.toPage(rows, row -> List.of(row.accessedAt()), AccessLogRow::id);
    }

    private static AccessLogRow map(ResultSet rs, int row) throws SQLException {
        return new AccessLogRow(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                rs.getObject("accessed_at", OffsetDateTime.class), rs.getString("operation"), rs.getString("url"),
                rs.getString("params"), rs.getBoolean("is_async"), rs.getString("session_ref"),
                rs.getObject("cohort_id", UUID.class), rs.getString("role_name"), rs.getString("course_week"),
                rs.getString("analytics_link_ref"), rs.getString("user_agent"), rs.getString("referer"),
                rs.getString("ip_address"));
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
