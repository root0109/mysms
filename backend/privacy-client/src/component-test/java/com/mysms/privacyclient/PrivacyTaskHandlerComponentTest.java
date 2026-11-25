package com.mysms.privacyclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.privacyclient.testing.TestLegalHolds;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The Template Method a module extends, against real PostgreSQL as the application role (a test schema shaped like a
 * module's): a task is recorded once and run by the job, erases the subject's rows and answers through the outbox; a
 * held subject's rows are kept and counted, and so are the rows kept under a retention policy, with its code; privacy
 * unreachable means nothing is removed; a task dispatched again is answered again or retried; export, restriction,
 * another module's task, malformed events; a retention sweep erases only what is due, and refuses a policy it does
 * not sweep.
 */
class PrivacyTaskHandlerComponentTest {

    private static final String MODULE = "ptasktest";
    private static final String TABLE = "ptasktest.person";
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String POLICY = "ptasktest.old_people";
    private static final String KEPT_POLICY = "ptasktest.after_leaving";
    private static final String PAYSLIP = "ptasktest.payslip";
    private static final String CONTRACT = "ptasktest.contract";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final String SELECT = """
            SELECT id FROM ptasktest.person
             WHERE tenant_id = :tenantId AND subject_id = :subjectId AND name <> 'Erased'
             ORDER BY id
            """;
    private static final String SELECT_DUE = """
            SELECT id FROM ptasktest.person
             WHERE tenant_id = :tenantId AND subject_id = :subjectId AND name <> 'Erased' AND created_at < :cutoff
             ORDER BY id
            """;
    private static final String ERASE = """
            UPDATE ptasktest.person SET name = 'Erased', updated_at = :now
             WHERE tenant_id = :tenantId AND id = :id AND name <> 'Erased'
            """;
    private static final String SUBJECTS_DUE = """
            SELECT DISTINCT subject_id FROM ptasktest.person
             WHERE tenant_id = :tenantId AND created_at < :cutoff AND name <> 'Erased' AND subject_id > :after
             ORDER BY subject_id
             LIMIT :limit
            """;
    private static final String PAYSLIP_KEPT = """
            SELECT count(*) FROM ptasktest.payslip WHERE tenant_id = :tenantId AND subject_id = :subjectId
            """;
    private static final String CONTRACT_KEPT = """
            SELECT count(*) FROM ptasktest.contract WHERE tenant_id = :tenantId AND subject_id = :subjectId
            """;

    private static TestDatabase database;
    private static ModuleDatabase db;
    private static Handler handler;

    /**
     * The module's handler: one personal-data table, anonymised in place, two tables kept under retention policies,
     * and one retention policy swept.
     */
    static final class Handler extends PrivacyTaskHandler {

        Handler(ModuleDatabase db, OutboxWriter outbox, AuditWriter audit, JobSchedule schedule) {
            super(MODULE, db, db.transactionManager(), outbox, audit, schedule, new TestLegalHolds(), JSON,
                    Clock.systemUTC(), Duration.ofMinutes(5));
        }

        @Override
        protected List<ErasureStep> erasureSteps() {
            return List.of(new ErasureStep(TABLE, "identity", SELECT, ERASE, false, List.of("name")));
        }

        @Override
        protected List<KeptRows> keptRows() {
            return List.of(new KeptRows(PAYSLIP, PAYSLIP_KEPT, KEPT_POLICY),
                    new KeptRows(CONTRACT, CONTRACT_KEPT, POLICY));
        }

        @Override
        protected Map<String, RetentionSweep> retentionSweeps() {
            return Map.of(POLICY, new RetentionSweep(SUBJECTS_DUE,
                    List.of(new ErasureStep(TABLE, "identity", SELECT_DUE, ERASE, true, List.of("name")))));
        }
    }

    @BeforeAll
    static void open() {
        database = TestDatabase.create();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("mysms.ptasktest.datasource.url", database.jdbcUrl())
                .withProperty("mysms.ptasktest.datasource.username", database.username())
                .withProperty("mysms.ptasktest.datasource.password", database.password())
                .withProperty("mysms.ptasktest.datasource.migrate-username", database.migrateUsername())
                .withProperty("mysms.ptasktest.datasource.migrate-password", database.migratePassword());
        db = ModuleDatabase.open(MODULE, environment, true);
        OutboxWriter outbox = new OutboxWriter(MODULE, db.jdbc(), JSON);
        handler = new Handler(db, outbox, new AuditWriter(outbox, Clock.systemUTC()), new JobSchedule(db));
    }

    @AfterAll
    static void close() {
        db.close();
        database.close();
    }

    private static <T> T as(UUID tenant, Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), work);
    }

    private static <T> T write(UUID tenant, Supplier<T> work) {
        return as(tenant, () -> TestTransactions.of(db).write(work::get));
    }

    private static UUID person(UUID tenant, UUID subject, String name, OffsetDateTime created) {
        UUID id = UUID.randomUUID();
        write(tenant, () -> db.jdbc().sql("""
                INSERT INTO ptasktest.person (id, tenant_id, subject_id, name, created_at)
                VALUES (:id, :tenantId, :subject, :name, :created)
                """).param("id", id).param("subject", subject).param("name", name).param("created", created)
                .update());
        return id;
    }

    private static void kept(UUID tenant, String table, UUID subject) {
        write(tenant, () -> db.jdbc().sql("INSERT INTO " + table + " (id, tenant_id, subject_id)"
                + " VALUES (:id, :tenantId, :subject)").param("id", UUID.randomUUID()).param("subject", subject)
                .update());
    }

    private static int count(UUID tenant, String table, UUID subject) {
        return write(tenant, () -> db.jdbc().sql("SELECT count(*) FROM " + table + " WHERE tenant_id = :tenantId"
                + " AND subject_id = :subject").param("subject", subject).query(Integer.class).single());
    }

    private static String name(UUID tenant, UUID id) {
        return write(tenant, () -> db.jdbc().sql("SELECT name FROM ptasktest.person WHERE tenant_id = :tenantId"
                + " AND id = :id").param("id", id).query(String.class).single());
    }

    private static String receipt(UUID tenant, UUID id) {
        return write(tenant, () -> db.jdbc().sql("SELECT status FROM ptasktest.privacy_task_receipt"
                + " WHERE tenant_id = :tenantId AND task_id = :id").param("id", id).query(String.class).optional()
                .orElse(null));
    }

    /** The payloads of the module's events of one type, oldest first. */
    private static List<JsonNode> events(UUID tenant, String type) {
        return write(tenant, () -> db.jdbc().sql("SELECT payload FROM ptasktest.outbox_event"
                + " WHERE tenant_id = :tenantId AND event_type = :type ORDER BY id").param("type", type)
                .query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    private static String task(UUID taskId, String action, UUID subject, String module) {
        return "{\"taskId\":\"" + taskId + "\",\"requestId\":\"" + UUID.randomUUID() + "\",\"type\":\"erasure\","
                + "\"action\":\"" + action + "\",\"subjectPersonId\":\"" + subject + "\",\"module\":\"" + module
                + "\",\"wave\":2,\"deadline\":\"2030-01-01T00:00:00Z\"}";
    }

    private static String retention(UUID runId, String policy, String action, OffsetDateTime cutoff) {
        return "{\"runId\":\"" + runId + "\",\"policyCode\":\"" + policy + "\",\"categoryCode\":\"identity\","
                + "\"module\":\"" + MODULE + "\",\"cutoff\":\"" + cutoff + "\",\"action\":\"" + action + "\"}";
    }

    private static void deliver(UUID tenant, String type, UUID id, String data) {
        String aggregate = type.substring(0, type.lastIndexOf('.'));
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), "privacy", type, Optional.of(tenant), aggregate,
                id, Instant.now(), data, Optional.empty());
        write(tenant, () -> {
            handler.receive(event);
            return null;
        });
    }

    private static void runJob(UUID tenant) {
        as(tenant, () -> {
            handler.run();
            return null;
        });
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    @Test
    void anErasureTaskIsRecordedOnceRunByTheJobAndAnsweredAgainWhenDispatchedAgain() {
        UUID tenant = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        UUID first = person(tenant, subject, "Asha Rao", now());
        UUID second = person(tenant, subject, "Asha R.", now());
        UUID other = person(tenant, UUID.randomUUID(), "Kabir", now());
        UUID taskId = UUID.randomUUID();

        deliver(tenant, PrivacyTaskHandler.TASK_ASSIGNED, taskId, task(taskId, "erase", subject, MODULE));
        assertThat(receipt(tenant, taskId)).isEqualTo("received");
        assertThat(name(tenant, first)).as("nothing runs inside the inbox's transaction").isEqualTo("Asha Rao");

        runJob(tenant);
        assertThat(receipt(tenant, taskId)).isEqualTo("done");
        assertThat(name(tenant, first)).isEqualTo("Erased");
        assertThat(name(tenant, second)).isEqualTo("Erased");
        assertThat(name(tenant, other)).isEqualTo("Kabir");
        assertThat(events(tenant, TABLE + ".updated")).hasSize(2);
        List<JsonNode> answers = events(tenant, MODULE + ".privacy_task.completed");
        assertThat(answers).hasSize(1);
        assertThat(answers.get(0).path("status").asString()).isEqualTo("done");
        assertThat(answers.get(0).path("counts").path(TABLE).asInt()).isEqualTo(2);
        assertThat(answers.get(0).toString()).doesNotContain("Asha");

        deliver(tenant, PrivacyTaskHandler.TASK_ASSIGNED, taskId, task(taskId, "erase", subject, MODULE));
        assertThat(events(tenant, MODULE + ".privacy_task.completed")).as("answered again from the receipt")
                .hasSize(2);
        runJob(tenant);
        assertThat(events(tenant, TABLE + ".updated")).as("never erased twice").hasSize(2);
    }

    @Test
    void aHeldSubjectsRowsAreKeptAndCounted() {
        UUID tenant = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        UUID row = person(tenant, subject, "Meera", now());
        TestLegalHolds.holdSubject(subject);
        UUID taskId = UUID.randomUUID();

        deliver(tenant, PrivacyTaskHandler.TASK_ASSIGNED, taskId, task(taskId, "anonymise", subject, MODULE));
        runJob(tenant);

        assertThat(name(tenant, row)).isEqualTo("Meera");
        JsonNode answer = events(tenant, MODULE + ".privacy_task.completed").get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("heldItems").asInt()).isEqualTo(1);
    }

    @Test
    void theRowsKeptUnderARetentionPolicyAreCountedWithThePolicyThatKeptThemAndExported() {
        UUID tenant = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        UUID row = person(tenant, subject, "Nisha", now());
        kept(tenant, PAYSLIP, subject);
        kept(tenant, PAYSLIP, subject);
        kept(tenant, CONTRACT, UUID.randomUUID());
        UUID erase = UUID.randomUUID();
        UUID export = UUID.randomUUID();

        deliver(tenant, PrivacyTaskHandler.TASK_ASSIGNED, erase, task(erase, "erase", subject, MODULE));
        runJob(tenant);
        deliver(tenant, PrivacyTaskHandler.TASK_ASSIGNED, export, task(export, "export", subject, MODULE));
        runJob(tenant);

        assertThat(name(tenant, row)).isEqualTo("Erased");
        assertThat(count(tenant, PAYSLIP, subject)).as("kept rows are untouched").isEqualTo(2);
        List<JsonNode> answers = events(tenant, MODULE + ".privacy_task.completed");
        JsonNode erased = answers.get(0);
        assertThat(erased.path("status").asString()).isEqualTo("partial");
        assertThat(erased.path("counts").path(TABLE).asInt()).isEqualTo(1);
        assertThat(erased.path("heldItems").asInt()).isEqualTo(2);
        assertThat(erased.path("policyCodes").size()).as("only the policies that kept a row").isEqualTo(1);
        assertThat(erased.path("policyCodes").get(0).asString()).isEqualTo(KEPT_POLICY);
        JsonNode exported = answers.get(1);
        assertThat(exported.path("counts").path(PAYSLIP).asInt()).isEqualTo(2);
        assertThat(exported.path("counts").path(CONTRACT).asInt()).isZero();
        assertThat(exported.path("counts").path(TABLE).asInt()).as("the erased row is no longer the subject's")
                .isZero();
    }

    @Test
    void whenPrivacyCannotSayWhichHoldsApplyNothingIsRemovedAndTheTaskIsRetriedWhenDispatchedAgain() {
        UUID tenant = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        UUID row = person(tenant, subject, "Arjun", now());
        TestLegalHolds.unavailableFor(subject);
        UUID taskId = UUID.randomUUID();

        deliver(tenant, PrivacyTaskHandler.TASK_ASSIGNED, taskId, task(taskId, "erase", subject, MODULE));
        runJob(tenant);
        assertThat(name(tenant, row)).isEqualTo("Arjun");
        assertThat(receipt(tenant, taskId)).isEqualTo("failed");
        assertThat(events(tenant, MODULE + ".privacy_task.completed").get(0).path("errorCode").asString())
                .isEqualTo(PrivacyTaskHandler.HOLDS_UNAVAILABLE);

        deliver(tenant, PrivacyTaskHandler.TASK_ASSIGNED, taskId, task(taskId, "erase", subject, MODULE));
        assertThat(receipt(tenant, taskId)).as("a failed task runs again").isEqualTo("received");
    }

    @Test
    void anExportCountsTheRowsARestrictionIsNotApplicableAndAnotherModulesTaskIsIgnored() {
        UUID tenant = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        UUID row = person(tenant, subject, "Ravi", now());
        UUID export = UUID.randomUUID();
        UUID restrict = UUID.randomUUID();
        UUID elsewhere = UUID.randomUUID();

        deliver(tenant, PrivacyTaskHandler.TASK_ASSIGNED, export, task(export, "export", subject, MODULE));
        deliver(tenant, PrivacyTaskHandler.TASK_ASSIGNED, restrict, task(restrict, "restrict", subject, MODULE));
        deliver(tenant, PrivacyTaskHandler.TASK_ASSIGNED, elsewhere, task(elsewhere, "erase", subject, "sis"));
        runJob(tenant);

        assertThat(receipt(tenant, export)).isEqualTo("done");
        assertThat(receipt(tenant, restrict)).isEqualTo("not_applicable");
        assertThat(receipt(tenant, elsewhere)).isNull();
        assertThat(name(tenant, row)).isEqualTo("Ravi");
        assertThat(events(tenant, MODULE + ".privacy_task.completed").get(0).path("counts").path(TABLE).asInt())
                .isEqualTo(1);
    }

    @Test
    void refusesAMalformedTaskAndOneAboutAnotherId() {
        UUID tenant = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        assertThatThrownBy(() -> deliver(tenant, PrivacyTaskHandler.TASK_ASSIGNED, taskId,
                task(taskId, "shred", UUID.randomUUID(), MODULE))).isInstanceOf(RejectedEventException.class);
        assertThatThrownBy(() -> deliver(tenant, PrivacyTaskHandler.TASK_ASSIGNED, UUID.randomUUID(),
                task(taskId, "erase", UUID.randomUUID(), MODULE))).isInstanceOf(RejectedEventException.class);
    }

    @Test
    void aRetentionSweepErasesOnlyWhatIsDueAndRefusesAPolicyItDoesNotSweep() {
        UUID tenant = UUID.randomUUID();
        OffsetDateTime cutoff = now().minusDays(30);
        UUID subject = UUID.randomUUID();
        UUID old = person(tenant, subject, "Old record", cutoff.minusDays(1));
        UUID recent = person(tenant, subject, "Recent record", now());
        UUID heldSubject = UUID.randomUUID();
        UUID held = person(tenant, heldSubject, "Held record", cutoff.minusDays(2));
        TestLegalHolds.holdSubject(heldSubject);
        UUID run = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        UUID archive = UUID.randomUUID();

        deliver(tenant, PrivacyTaskHandler.RETENTION_DUE, run, retention(run, POLICY, "anonymise", cutoff));
        deliver(tenant, PrivacyTaskHandler.RETENTION_DUE, unknown, retention(unknown, "ptasktest.other", "delete",
                cutoff));
        deliver(tenant, PrivacyTaskHandler.RETENTION_DUE, archive, retention(archive, POLICY, "archive", cutoff));
        runJob(tenant);

        assertThat(name(tenant, old)).isEqualTo("Erased");
        assertThat(name(tenant, recent)).isEqualTo("Recent record");
        assertThat(name(tenant, held)).isEqualTo("Held record");
        assertThat(events(tenant, TABLE + ".deleted")).hasSize(1);
        assertThat(receipt(tenant, run)).isEqualTo("partial");
        assertThat(receipt(tenant, unknown)).isEqualTo("failed");
        assertThat(receipt(tenant, archive)).isEqualTo("failed");
        JsonNode answer = events(tenant, MODULE + ".privacy_retention.completed").stream()
                .filter(e -> run.toString().equals(e.path("runId").asString())).findFirst().orElseThrow();
        assertThat(answer.path("affected").asInt()).isEqualTo(1);
        assertThat(answer.path("held").asInt()).isEqualTo(1);
    }
}
