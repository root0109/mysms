package com.mysms.core.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.env.MockEnvironment;

/**
 * Real PostgreSQL as the application role: a tenant enrols once, a due run is leased to one runner at a time, a lease
 * that ended is taken over, only the lease's owner ends it, and the runtime runs a job in its tenant's context.
 */
class JobScheduleComponentTest {

    private static final String JOB = "jobtest.count";
    private static final List<String> CODES = List.of(JOB);
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    private static TestDatabase database;
    private static ModuleDatabase db;
    private static JobSchedule schedule;

    @BeforeAll
    static void open() {
        database = TestDatabase.create();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("mysms.jobtest.datasource.url", database.jdbcUrl())
                .withProperty("mysms.jobtest.datasource.username", database.username())
                .withProperty("mysms.jobtest.datasource.password", database.password())
                .withProperty("mysms.jobtest.datasource.migrate-username", database.migrateUsername())
                .withProperty("mysms.jobtest.datasource.migrate-password", database.migratePassword());
        db = ModuleDatabase.open("jobtest", environment, true);
        schedule = new JobSchedule(db);
    }

    @AfterAll
    static void close() {
        db.close();
        database.close();
    }

    @BeforeEach
    void clear() {
        JdbcClient.create(db.ownerDataSource()).sql("DELETE FROM jobtest.job_schedule").update();
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private static void enrol(UUID tenant, OffsetDateTime at) {
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(db).write(() -> {
                    schedule.enrol(JOB, at);
                    return null;
                }));
    }

    @Test
    void aTenantEnrolsOnceAndIsLeasedToOneRunnerAtATime() {
        UUID tenant = UUID.randomUUID();
        OffsetDateTime now = now();
        enrol(tenant, now);
        enrol(tenant, now.plusHours(1));

        UUID first = UUID.randomUUID();
        List<JobSchedule.Claim> claimed = schedule.claim(first, CODES, now, now.plusMinutes(30), 10);
        assertThat(claimed).containsExactly(new JobSchedule.Claim(JOB, tenant, 0));
        assertThat(schedule.claim(UUID.randomUUID(), CODES, now, now.plusMinutes(30), 10)).as("leased").isEmpty();
        assertThat(schedule.complete(claimed.get(0), UUID.randomUUID(), now, now.plusHours(1)))
                .as("only the lease's owner ends it").isFalse();

        assertThat(schedule.complete(claimed.get(0), first, now, now.plusHours(1))).isTrue();
        assertThat(schedule.claim(first, CODES, now.plusMinutes(59), now.plusMinutes(89), 10)).as("not due yet").isEmpty();
        assertThat(schedule.claim(first, CODES, now.plusHours(1), now.plusHours(2), 10)).hasSize(1);
    }

    @Test
    void anEndedLeaseIsTakenOverAndAFailureIsCountedUntilARunFinishes() {
        UUID tenant = UUID.randomUUID();
        OffsetDateTime now = now();
        enrol(tenant, now);
        UUID died = UUID.randomUUID();
        schedule.claim(died, CODES, now, now.plusMinutes(30), 10);

        UUID next = UUID.randomUUID();
        List<JobSchedule.Claim> taken = schedule.claim(next, CODES, now.plusMinutes(31), now.plusMinutes(61), 10);
        assertThat(taken).hasSize(1);
        assertThat(schedule.fail(taken.get(0), next, now.plusMinutes(32), now.plusMinutes(33), "x".repeat(600)))
                .isTrue();
        List<JobSchedule.Claim> retried = schedule.claim(next, CODES, now.plusMinutes(33), now.plusMinutes(63), 10);
        assertThat(retried).containsExactly(new JobSchedule.Claim(JOB, tenant, 1));
        assertThat(schedule.complete(retried.get(0), next, now.plusMinutes(34), now.plusHours(2))).isTrue();
        assertThat(JdbcClient.create(db.ownerDataSource()).sql(
                "SELECT failures FROM jobtest.job_schedule WHERE tenant_id = :tenant").param("tenant", tenant)
                .query(Integer.class).single()).isZero();
    }

    @Test
    void theRuntimeRunsADueJobInItsTenantsContext() {
        UUID tenant = UUID.randomUUID();
        enrol(tenant, now().minusMinutes(1));
        AtomicInteger runs = new AtomicInteger();
        List<UUID> seen = new CopyOnWriteArrayList<>();
        ScheduledJob job = new ScheduledJob() {
            @Override
            public String module() {
                return "jobtest";
            }

            @Override
            public String code() {
                return JOB;
            }

            @Override
            public Duration interval() {
                return Duration.ofHours(1);
            }

            @Override
            public void run() {
                runs.incrementAndGet();
                seen.add(CallContextHolder.require().tenantId().orElseThrow());
            }
        };
        JobRuntime runtime = new JobRuntime(List.of(schedule), List.of(job), Clock.systemUTC(), Duration.ofMinutes(1),
                Duration.ofMinutes(30), 10);

        assertThat(runtime.runOnce()).isEqualTo(1);
        assertThat(runtime.runOnce()).as("due again only after its interval").isZero();
        assertThat(runs).hasValue(1);
        assertThat(seen).containsExactly(tenant);
    }
}
