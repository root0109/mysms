package com.mysms.core.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.env.MockEnvironment;

/** Real PostgreSQL: the job connects as the schema owner and creates, checks and drops monthly partitions. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PartitionMaintenanceComponentTest {

    private static final UUID TENANT = UUID.fromString("0190b0a0-0000-7000-8000-00000000000a");

    private static TestDatabase database;
    private static ModuleDatabase db;

    @BeforeAll
    static void open() {
        database = TestDatabase.create();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("mysms.parttest.datasource.url", database.jdbcUrl())
                .withProperty("mysms.parttest.datasource.username", database.username())
                .withProperty("mysms.parttest.datasource.password", database.password())
                .withProperty("mysms.parttest.datasource.migrate-username", database.migrateUsername())
                .withProperty("mysms.parttest.datasource.migrate-password", database.migratePassword());
        db = ModuleDatabase.open("parttest", environment, true);
    }

    @AfterAll
    static void close() {
        db.close();
        database.close();
    }

    @AfterEach
    void clear() {
        CallContextHolder.clear();
    }

    private static List<String> partitions(String table) {
        return JdbcClient.create(db.ownerDataSource()).sql("""
                SELECT ch.relname || ' ' || pg_get_expr(ch.relpartbound, ch.oid)
                  FROM pg_inherits i JOIN pg_class ch ON ch.oid = i.inhrelid JOIN pg_class p ON p.oid = i.inhparent
                 WHERE p.relname = :table ORDER BY ch.relname
                """).param("table", table).query(String.class).list();
    }

    @Test
    @Order(1)
    void createsTheCurrentMonthAndTheMonthsAheadOnce() {
        PartitionMaintenance.Report report = PartitionMaintenance.run("parttest", db.ownerDataSource(),
                LocalDate.parse("2026-11-15"), 2, 0);
        assertThat(report.created()).containsExactly(
                "parttest.daily_row_2026_11", "parttest.daily_row_2026_12", "parttest.daily_row_2027_01",
                "parttest.event_log_2026_11", "parttest.event_log_2026_12", "parttest.event_log_2027_01");
        assertThat(report.dropped()).isEmpty();
        assertThat(report.defaultNotEmpty()).isEmpty();
        assertThat(report.failed()).isEmpty();
        assertThat(partitions("daily_row")).contains(
                "daily_row_2026_12 FOR VALUES FROM ('2026-12-01') TO ('2027-01-01')");
        assertThat(PartitionMaintenance.run("parttest", db.ownerDataSource(), LocalDate.parse("2026-11-30"), 2, 0)
                .created()).as("a second run finds every month in place").isEmpty();

        // a timestamp at the very start of a UTC month lands in that month, whatever the session's time zone
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(TENANT, TENANT));
        TestTransactions.of(db).write(() -> db.jdbc().sql("INSERT INTO parttest.event_log (tenant_id, created_at) "
                + "VALUES (:tenantId, CAST('2026-12-01T00:00:00Z' AS timestamptz))").update());
        assertThat(JdbcClient.create(db.ownerDataSource()).sql(
                "SELECT pg_relation_size('parttest.event_log_2026_12') > 0").query(Boolean.class).single()).isTrue();
    }

    @Test
    @Order(2)
    void reportsRowsInTheDefaultPartitionAndCannotAttachTheirMonth() {
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(TENANT, TENANT));
        TestTransactions.of(db).write(() -> db.jdbc().sql("INSERT INTO parttest.daily_row (tenant_id, happened_on) "
                + "VALUES (:tenantId, DATE '2030-01-05')").update());
        PartitionMaintenance.Report report = PartitionMaintenance.run("parttest", db.ownerDataSource(),
                LocalDate.parse("2029-12-20"), 1, 0);
        assertThat(report.defaultNotEmpty()).containsExactly("parttest.daily_row_default");
        assertThat(report.created()).contains("parttest.daily_row_2029_12", "parttest.event_log_2030_01")
                .doesNotContain("parttest.daily_row_2030_01");
        assertThat(report.failed()).containsExactly("parttest.daily_row_2030_01");
    }

    @Test
    @Order(3)
    void dropsMonthsPastTheRetentionOnlyWhenOneIsSet() {
        assertThat(PartitionMaintenance.run("parttest", db.ownerDataSource(), LocalDate.parse("2027-11-10"), 0, 0)
                .dropped()).isEmpty();
        assertThatThrownBy(() -> PartitionMaintenance.run("parttest", db.ownerDataSource(),
                LocalDate.parse("2027-11-10"), 0, 4)).as("a retention below a year is refused")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PartitionMaintenance.run("parttest", db.ownerDataSource(),
                LocalDate.parse("2027-11-10"), PartitionMaintenance.MAX_MONTHS_AHEAD + 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        PartitionMaintenance.Report report = PartitionMaintenance.run("parttest", db.ownerDataSource(),
                LocalDate.parse("2027-11-10"), 0, 12);
        // kept: November 2026 onwards (twelve months before November 2027); dropped: every month that ended by then
        assertThat(report.dropped()).containsExactlyInAnyOrder("parttest.daily_row_2026_10",
                "parttest.event_log_2026_09");
        assertThat(partitions("daily_row")).noneMatch(p -> p.startsWith("daily_row_2026_10"))
                .anyMatch(p -> p.startsWith("daily_row_2026_11"));
    }

    @Test
    @Order(4)
    void theJobRunsEveryModuleAndRefusesBadSettings() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-03T10:00:00Z"), ZoneOffset.UTC);
        PartitionMaintenanceJob job = new PartitionMaintenanceJob(List.of(db), clock, Duration.ofHours(24), 3, 0);
        List<PartitionMaintenance.Report> reports = job.runOnce();
        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).created()).contains("parttest.daily_row_2027_02");
        assertThat(job.isRunning()).isFalse();
        job.start();
        job.start();
        assertThat(job.isRunning()).isTrue();
        job.stop();
        job.stop();
        assertThat(job.isRunning()).isFalse();
        assertThatThrownBy(() -> new PartitionMaintenanceJob(List.of(db), clock, Duration.ZERO, 3, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PartitionMaintenance.run("Bad-Schema", db.ownerDataSource(), LocalDate.now(), 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PartitionMaintenance.run("parttest", db.ownerDataSource(), LocalDate.now(), -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(PartitionMaintenance.range("DEFAULT")).isEmpty();
        assertThat(PartitionMaintenance.range("FOR VALUES FROM ('2026-01-01 00:00:00+00') TO ('2026-02-01 00:00:00+00')"))
                .get().extracting(r -> r[1]).isEqualTo(LocalDate.parse("2026-02-01"));
    }
}
