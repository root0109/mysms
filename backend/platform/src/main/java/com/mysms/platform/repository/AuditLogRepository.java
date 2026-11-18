package com.mysms.platform.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.platform.config.PlatformDatabaseConfig;
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
 * Read access to the tenant's audit trail, platform.audit_log (written only by events/PlatformAuditTrail). The table is
 * partitioned by month on occurred_at: a list always carries an occurredAt range (the service requires it), so only
 * those months are read; keyset paging runs on occurred_at, newest first by default.
 */
@Repository
public class AuditLogRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("occurredAt", "r.occurred_at", ValueType.TIMESTAMP),
            field("logName", "r.log_name", ValueType.STRING),
            field("event", "r.event", ValueType.STRING),
            field("subjectType", "r.subject_type", ValueType.STRING),
            field("subjectId", "r.subject_id", ValueType.UUID),
            field("causerId", "r.causer_id", ValueType.UUID),
            field("batchId", "r.batch_id", ValueType.UUID));

    /** The partition key; NOT NULL, so keyset paging can compare it. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("occurredAt", "r.occurred_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "occurredAt desc";
    public static final String ID_COLUMN = "r.id";
    public static final String RANGE_FIELD = "occurredAt";

    private static final String SELECT = """
            SELECT r.id, r.log_name, r.description, r.event, r.subject_type, r.subject_id, r.causer_type,
                   r.causer_id, CAST(r.properties AS text) AS properties, r.batch_id,
                   host(r.ip_address) AS ip_address, r.occurred_at
              FROM platform.audit_log r
             WHERE r.tenant_id = :tenantId
            """;
    private static final String FIND_BY_ID = SELECT + " AND r.id = :id";
    /** One partition range of the trail, oldest first (the audit-log report). */
    private static final String EXPORT = SELECT + """
             AND r.occurred_at >= :from AND r.occurred_at < :to
            ORDER BY r.occurred_at, r.id
            LIMIT :limit
            """;

    private final JdbcClient jdbc;

    public AuditLogRepository(@Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AuditLogRow> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AuditLogRepository::map).optional();
    }

    public Page<AuditLogRow> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AuditLogRow> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(AuditLogRepository::map).list();
        return page.toPage(rows, row -> List.of(row.occurredAt()), AuditLogRow::id);
    }

    /** At most {@code limit} records of [from, to), oldest first. */
    public List<AuditLogRow> export(OffsetDateTime from, OffsetDateTime to, int limit) {
        return jdbc.sql(EXPORT).param("from", from).param("to", to).param("limit", limit)
                .query(AuditLogRepository::map).list();
    }

    private static AuditLogRow map(ResultSet rs, int row) throws SQLException {
        return new AuditLogRow(rs.getObject("id", UUID.class), rs.getString("log_name"), rs.getString("description"),
                rs.getString("event"), rs.getString("subject_type"), rs.getObject("subject_id", UUID.class),
                rs.getString("causer_type"), rs.getObject("causer_id", UUID.class), rs.getString("properties"),
                rs.getObject("batch_id", UUID.class), rs.getString("ip_address"),
                rs.getObject("occurred_at", OffsetDateTime.class));
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
