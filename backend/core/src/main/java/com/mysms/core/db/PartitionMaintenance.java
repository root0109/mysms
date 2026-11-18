package com.mysms.core.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Keeps a module's range-partitioned tables ready: monthly partitions ahead, an alert when a DEFAULT partition holds
 * rows (which also blocks attaching new months), and, only with a retention, dropping months before it. Runs as the
 * schema owner, because only a table's owner may attach partitions; the application role never runs DDL.
 */
public final class PartitionMaintenance {

    /** What one run did, per table: partitions created and dropped, and DEFAULT partitions that hold rows. */
    public record Report(List<String> created, List<String> dropped, List<String> defaultNotEmpty,
                         List<String> failed) {
        public Report {
            created = List.copyOf(created);
            dropped = List.copyOf(dropped);
            defaultNotEmpty = List.copyOf(defaultNotEmpty);
            failed = List.copyOf(failed);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(PartitionMaintenance.class);
    private static final Pattern NAME = Pattern.compile("^[a-z_][a-z0-9_]*$");
    private static final Pattern RANGE = Pattern.compile(
            "FROM \\('(\\d{4}-\\d{2}-\\d{2})[^)]*\\) TO \\('(\\d{4}-\\d{2}-\\d{2})[^)]*\\)");
    private static final DateTimeFormatter SUFFIX = DateTimeFormatter.ofPattern("yyyy_MM");
    private static final int MAX_IDENTIFIER = 63;
    private static final String DUPLICATE_TABLE = "42P07";
    /** The most months a run creates ahead. */
    public static final int MAX_MONTHS_AHEAD = 24;
    /** A retention shorter than this is refused: the audit log and attendance must outlive a school year. */
    public static final int MIN_RETENTION_MONTHS = 12;
    private static final String LOCK_TIMEOUT = "5s";

    /** The schema's range-partitioned tables with one partition column, and that column's type. */
    private static final String TABLES = """
            SELECT c.relname AS table_name, format_type(a.atttypid, a.atttypmod) AS column_type
              FROM pg_partitioned_table pt
              JOIN pg_class c ON c.oid = pt.partrelid
              JOIN pg_namespace n ON n.oid = c.relnamespace
              JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = pt.partattrs[0]
             WHERE n.nspname = :schema AND pt.partstrat = 'r' AND pt.partnatts = 1
             ORDER BY c.relname
            """;
    /** A table's partitions, their bounds and whether any page was ever written (no row access, so RLS is moot). */
    private static final String PARTITIONS = """
            SELECT ch.relname AS name, pg_get_expr(ch.relpartbound, ch.oid) AS bound,
                   pg_relation_size(ch.oid) > 0 AS written
              FROM pg_inherits i
              JOIN pg_class ch ON ch.oid = i.inhrelid
              JOIN pg_class p ON p.oid = i.inhparent
              JOIN pg_namespace n ON n.oid = p.relnamespace
             WHERE n.nspname = :schema AND p.relname = :table
             ORDER BY ch.relname
            """;

    private PartitionMaintenance() {
    }

    /**
     * One run over the module's schema.
     *
     * @param owner           connections as the schema owner
     * @param today           the current date (UTC)
     * @param monthsAhead     months after the current one that must already have a partition
     * @param retentionMonths months kept before the current one; 0 keeps every month (nothing is ever dropped)
     */
    public static Report run(String schema, DataSource owner, LocalDate today, int monthsAhead, int retentionMonths) {
        if (!NAME.matcher(schema).matches()) {
            throw new IllegalArgumentException("invalid schema name " + schema);
        }
        if (monthsAhead < 0 || monthsAhead > MAX_MONTHS_AHEAD) {
            throw new IllegalArgumentException("monthsAhead must be 0.." + MAX_MONTHS_AHEAD);
        }
        if (retentionMonths != 0 && retentionMonths < MIN_RETENTION_MONTHS) {
            throw new IllegalArgumentException("retentionMonths must be 0 (keep everything) or at least "
                    + MIN_RETENTION_MONTHS);
        }
        try (Connection connection = owner.getConnection()) {
            // one connection: the bounds are read and written in UTC, and DDL waits at most LOCK_TIMEOUT for its lock
            // instead of queueing behind a long query and stalling every request on the table
            JdbcClient jdbc = JdbcClient.create(new SingleConnectionDataSource(connection, true));
            jdbc.sql("SET TIME ZONE 'UTC'").update();
            jdbc.sql("SET lock_timeout = '" + LOCK_TIMEOUT + "'").update();
            return run(schema, jdbc, today, monthsAhead, retentionMonths);
        } catch (SQLException e) {
            throw new IllegalStateException("partition maintenance of " + schema + " could not connect", e);
        }
    }

    private static Report run(String schema, JdbcClient jdbc, LocalDate today, int monthsAhead, int retentionMonths) {
        List<String> created = new ArrayList<>();
        List<String> dropped = new ArrayList<>();
        List<String> defaults = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        YearMonth current = YearMonth.from(today);
        List<Map<String, Object>> tables = jdbc.sql(TABLES).param("schema", schema).query().listOfRows();
        for (Map<String, Object> table : tables) {
            String name = (String) table.get("table_name");
            if (!NAME.matcher(name).matches()) {
                failed.add(schema + "." + name);
                continue;
            }
            boolean timestamp = String.valueOf(table.get("column_type")).startsWith("timestamp with time zone");
            List<Map<String, Object>> partitions = jdbc.sql(PARTITIONS).param("schema", schema).param("table", name)
                    .query().listOfRows();
            Set<LocalDate> starts = new HashSet<>();
            for (Map<String, Object> partition : partitions) {
                String bound = String.valueOf(partition.get("bound"));
                String qualified = schema + "." + partition.get("name");
                if (!NAME.matcher(String.valueOf(partition.get("name"))).matches()) {
                    failed.add(qualified);
                    continue;
                }
                if ("DEFAULT".equals(bound)) {
                    if (Boolean.TRUE.equals(partition.get("written"))) {
                        defaults.add(qualified);
                        LOG.error("partition {} (DEFAULT) holds rows: a month is missing; create it and move the rows",
                                qualified);
                    }
                    continue;
                }
                Optional<LocalDate[]> range = range(bound);
                if (range.isEmpty()) {
                    continue;
                }
                starts.add(range.get()[0]);
                if (retentionMonths > 0
                        && !range.get()[1].isAfter(current.minusMonths(retentionMonths).atDay(1))) {
                    try {
                        // one statement: the partition is detached and dropped together, or not at all
                        jdbc.sql("DROP TABLE " + quote(schema, (String) partition.get("name"))).update();
                        dropped.add(qualified);
                        LOG.info("partition {} dropped: past the {}-month retention", qualified, retentionMonths);
                    } catch (RuntimeException e) {
                        failed.add(qualified);
                        LOG.error("partition {} could not be dropped", qualified, e);
                    }
                }
            }
            for (int i = 0; i <= monthsAhead; i++) {
                YearMonth month = current.plusMonths(i);
                if (starts.contains(month.atDay(1))) {
                    continue;
                }
                String partition = name + "_" + month.format(SUFFIX);
                if (partition.length() > MAX_IDENTIFIER) {
                    failed.add(schema + "." + partition);
                    continue;
                }
                try {
                    jdbc.sql("CREATE TABLE " + quote(schema, partition) + " PARTITION OF " + quote(schema, name)
                            + " FOR VALUES FROM (" + bound(month.atDay(1), timestamp) + ") TO ("
                            + bound(month.plusMonths(1).atDay(1), timestamp) + ")").update();
                    created.add(schema + "." + partition);
                    LOG.info("partition {}.{} created", schema, partition);
                } catch (RuntimeException e) {
                    if (alreadyExists(e)) { // another replica created it a moment ago
                        continue;
                    }
                    failed.add(schema + "." + partition);
                    LOG.error("partition {}.{} could not be created", schema, partition, e);
                }
            }
        }
        return new Report(created, dropped, defaults, failed);
    }

    private static boolean alreadyExists(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && DUPLICATE_TABLE.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    /** The FROM and TO dates of a range bound as PostgreSQL prints it, when both are dates or timestamps. */
    static Optional<LocalDate[]> range(String bound) {
        Matcher m = RANGE.matcher(bound);
        if (!m.find()) {
            return Optional.empty();
        }
        return Optional.of(new LocalDate[] {LocalDate.parse(m.group(1)), LocalDate.parse(m.group(2))});
    }

    /** A bound literal; timestamps are midnight UTC, so the month never depends on the session's time zone. */
    private static String bound(LocalDate day, boolean timestamp) {
        return "'" + day + (timestamp ? " 00:00:00+00" : "") + "'";
    }

    private static String quote(String schema, String name) {
        return '"' + schema + "\".\"" + name + '"';
    }
}
