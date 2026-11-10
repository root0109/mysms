package com.mysms.core.db;

import java.time.Duration;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.SqlProvider;
import org.springframework.jdbc.support.KeyHolder;

/**
 * The module's {@link JdbcTemplate}, which names every statement that takes longer than the module's limit
 * ({@code mysms.<module>.datasource.slow-statement}) in the log — the statement text with its placeholders, never a
 * bound value, so no data reaches the log. A Decorator of the three template methods the list, single-row and update
 * statements of a module's {@code JdbcClient} pass through (queries, updates and updates that return keys). Streamed
 * results and batches take other paths and are not timed.
 */
final class SlowStatementJdbcTemplate extends JdbcTemplate {

    /** As much of a statement as a log line carries. */
    static final int MAX_LOGGED = 300;
    private static final Logger LOG = LoggerFactory.getLogger(SlowStatementJdbcTemplate.class);

    private final String module;
    private final long limitNanos;

    SlowStatementJdbcTemplate(DataSource dataSource, String module, Duration limit) {
        super(dataSource);
        this.module = module;
        this.limitNanos = limit.toNanos();
    }

    @Override
    public <T> T query(PreparedStatementCreator psc, PreparedStatementSetter pss, ResultSetExtractor<T> rse) {
        long started = System.nanoTime();
        try {
            return super.query(psc, pss, rse);
        } finally {
            report(psc, started);
        }
    }

    @Override
    protected int update(PreparedStatementCreator psc, PreparedStatementSetter pss) {
        long started = System.nanoTime();
        try {
            return super.update(psc, pss);
        } finally {
            report(psc, started);
        }
    }

    @Override
    public int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder) {
        long started = System.nanoTime();
        try {
            return super.update(psc, generatedKeyHolder);
        } finally {
            report(psc, started);
        }
    }

    private void report(Object statement, long started) {
        long took = System.nanoTime() - started;
        if (took < limitNanos || !LOG.isWarnEnabled()) {
            return;
        }
        String sql = statement instanceof SqlProvider provider ? provider.getSql() : null;
        LOG.warn("slow statement in {}: {} ms: {}", module, Duration.ofNanos(took).toMillis(), abbreviate(sql));
    }

    /** The statement on one line, cut to {@link #MAX_LOGGED} characters. */
    static String abbreviate(String sql) {
        if (sql == null) {
            return "(statement text not available)";
        }
        String oneLine = sql.strip().replaceAll("\\s+", " ");
        return oneLine.length() <= MAX_LOGGED ? oneLine : oneLine.substring(0, MAX_LOGGED) + "…";
    }
}
