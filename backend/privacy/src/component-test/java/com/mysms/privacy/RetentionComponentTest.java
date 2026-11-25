package com.mysms.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.privacy.business.RetentionJob;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.events.GuardianLinkSync;
import com.mysms.privacy.events.RetentionAnswerHandler;
import com.mysms.privacy.events.SubjectSync;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

/**
 * Retention against real PostgreSQL through the real edge: the catalogue's policies with the tenant's own period and
 * action, set by a tenant administrator under If-Match and never below the legal floor; and the daily run, which sends
 * each policy's cutoff to the module holding the data once per day — the runs listed, ids and counts only. A tenant is
 * enrolled in the run once privacy learns of one of its persons.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "mysms.jobs.enabled=false")
class RetentionComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String POLICIES = "/v1/privacy/retention-policies";
    private static final String RUNS = "/v1/privacy/retention-runs";
    private static final String[] ADMINISTRATOR = {"privacy.retention_policy.read", "privacy.retention_policy.update",
        "privacy.retention_run.read"};
    private static final String ATTENDANCE = "attendance.after_leaving";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(PrivacyDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private SubjectSync subjects;

    @Autowired
    private GuardianLinkSync links;

    @Autowired
    private RetentionJob retention;

    @Autowired
    private RetentionAnswerHandler reports;

    private PrivacyCalls calls;
    private ConsentFixtures fixtures;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "privacy");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        // every module with a seeded policy, as if each had a handler: one run per policy
        registry.add("mysms.privacy.erasure.modules",
                () -> "admission,assessment,attendance,comms,finance,frontoffice,iam,payroll,platform,sis");
        registry.add("mysms.privacy.erasure.first-wave", () -> "iam");
    }

    @BeforeEach
    void connect() {
        calls = new PrivacyCalls(port, TOKENS);
        fixtures = new ConsentFixtures(calls, database, subjects, links);
    }

    private JsonNode policy(String admin, String code) throws Exception {
        for (JsonNode policy : calls.get(POLICIES + "?size=500", admin).body().path("data")) {
            if (code.equals(policy.path("code").asString())) {
                return policy;
            }
        }
        throw new AssertionError("no policy " + code);
    }

    private static JsonNode ofPolicy(JsonNode runs, String code) {
        for (JsonNode run : runs) {
            if (code.equals(run.path("policyCode").asString())) {
                return run;
            }
        }
        throw new AssertionError("no run of " + code);
    }

    private void runRetention(UUID tenant) {
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            retention.run();
            return null;
        });
    }

    private List<String> dueEvents(UUID tenant) {
        return fixtures.read(tenant, () -> database.jdbc().sql("""
                SELECT o.payload FROM privacy.outbox_event o
                 WHERE o.tenant_id = :tenantId AND o.event_type = 'privacy.retention.due'
                """).query(String.class).list());
    }

    @Test
    void theTenantSetsItsOwnPeriodNeverBelowTheLegalFloor() throws Exception {
        UUID tenant = UUID.randomUUID();
        String admin = calls.administrator(tenant, ADMINISTRATOR);
        JsonNode attendance = policy(admin, ATTENDANCE);
        assertThat(attendance.path("categoryCode").asString()).isEqualTo("attendance");
        assertThat(attendance.path("overridden").asBoolean()).isFalse();
        assertThat(attendance.path("etag").asString()).isEqualTo("\"0\"");
        int floor = attendance.path("minimumPeriodDays").asInt();
        String path = POLICIES + "/" + attendance.path("id").asString();

        PrivacyCalls.Reply belowFloor = calls.patch(path, admin, "\"0\"", Map.of("periodDays", floor - 1));
        assertThat(PrivacyCalls.fieldErrors(belowFloor)).containsExactly("periodDays:RETENTION_BELOW_LEGAL_FLOOR");
        PrivacyCalls.Reply set = calls.patch(path, admin, "\"0\"", Map.of("periodDays", floor, "action", "delete"));
        assertThat(set.status()).as(set.body().toString()).isEqualTo(200);
        assertThat(set.body().path("periodDays").asInt()).isEqualTo(floor);
        assertThat(set.text("action")).isEqualTo("delete");
        assertThat(set.body().path("overridden").asBoolean()).isTrue();
        assertThat(calls.patch(path, admin, "\"0\"", Map.of("periodDays", floor + 1)).status())
                .as("a stale ETag").isEqualTo(412);
        PrivacyCalls.Reply longer = calls.patch(path, admin, set.etag(), Map.of("periodDays", floor + 365));
        assertThat(longer.status()).as(longer.body().toString()).isEqualTo(200);
        assertThat(calls.get(path, admin).body().path("periodDays").asInt()).isEqualTo(floor + 365);

        assertThat(policy(calls.administrator(UUID.randomUUID(), ADMINISTRATOR), ATTENDANCE).path("overridden")
                .asBoolean()).as("another tenant keeps the catalogue's").isFalse();
        assertThat(calls.patch(path, calls.schoolAdministrator(tenant, ADMINISTRATOR), longer.etag(),
                Map.of("periodDays", floor + 400)).status()).as("tenant administrators only").isEqualTo(403);
    }

    @Test
    void theDailyRunSendsEachPolicysCutoffOncePerDay() throws Exception {
        UUID tenant = UUID.randomUUID();
        fixtures.adult(tenant, UUID.randomUUID());
        boolean enrolled = fixtures.read(tenant, () -> database.jdbc().sql("""
                SELECT EXISTS (SELECT 1 FROM privacy.job_schedule j
                                WHERE j.tenant_id = :tenantId AND j.job_code = 'privacy.retention')
                """).query(Boolean.class).single());
        assertThat(enrolled).as("a person of the tenant enrols it").isTrue();
        String admin = calls.administrator(tenant, ADMINISTRATOR);
        JsonNode attendance = policy(admin, ATTENDANCE);
        int days = attendance.path("minimumPeriodDays").asInt() + 10;
        assertThat(calls.patch(POLICIES + "/" + attendance.path("id").asString(), admin, "\"0\"",
                Map.of("periodDays", days, "action", "delete")).status()).isEqualTo(200);

        runRetention(tenant);
        runRetention(tenant);

        JsonNode runs = calls.get(RUNS + "?size=500", admin).body().path("data");
        assertThat(runs.size()).as("one run per policy, once a day").isEqualTo(10);
        assertThat(dueEvents(tenant)).hasSize(10);
        JsonNode run = ofPolicy(runs, ATTENDANCE);
        assertThat(run.path("status").asString()).isEqualTo("dispatched");
        assertThat(run.path("action").asString()).isEqualTo("delete");
        OffsetDateTime today = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS);
        assertThat(OffsetDateTime.parse(run.path("cutoff").asString())).isAtSameInstantAs(today.minusDays(days));
        assertThat(dueEvents(tenant)).anyMatch(e -> e.contains(run.path("id").asString())
                && e.contains("\"module\":\"attendance\"") && e.contains("\"categoryCode\":\"attendance\""));
    }

    @Test
    void aModulesReportFinishesItsRunOnce() throws Exception {
        UUID tenant = UUID.randomUUID();
        fixtures.adult(tenant, UUID.randomUUID());
        runRetention(tenant);
        String admin = calls.administrator(tenant, ADMINISTRATOR);
        JsonNode run = ofPolicy(calls.get(RUNS + "?size=500", admin).body().path("data"), "sis.health_after_leaving");
        UUID runId = UUID.fromString(run.path("id").asString());
        String report = "{\"runId\":\"" + runId + "\",\"status\":\"partial\",\"affected\":3,\"held\":1}";
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());

        assertThatThrownBy(() -> inbox.deliver(report(tenant, "people", runId, report), reports::apply))
                .as("another module's report on the run").isInstanceOf(RejectedEventException.class);
        assertThat(inbox.deliver(report(tenant, "sis", runId, report), reports::apply))
                .isEqualTo(InboxProcessor.Outcome.PROCESSED);
        boolean repeated = CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> reports.apply(report(tenant, "sis", runId, report)));
        assertThat(repeated).as("a repeated report changes nothing").isFalse();

        JsonNode finished = ofPolicy(calls.get(RUNS + "?size=500", admin).body().path("data"),
                "sis.health_after_leaving");
        assertThat(finished.path("status").asString()).isEqualTo("partial");
        assertThat(finished.path("affected").asInt()).isEqualTo(3);
        assertThat(finished.path("held").asInt()).isEqualTo(1);
        assertThat(finished.path("completedAt").isMissingNode()).isFalse();
    }

    private static EventEnvelope report(UUID tenant, String module, UUID runId, String data) {
        return new EventEnvelope(UUID.randomUUID(), module, module + ".privacy_retention.completed",
                Optional.of(tenant), module + ".privacy_retention", runId, Instant.now(), data, Optional.of(TRACE));
    }
}
