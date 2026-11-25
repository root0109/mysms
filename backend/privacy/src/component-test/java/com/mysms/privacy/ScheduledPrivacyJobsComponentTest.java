package com.mysms.privacy;

import static com.mysms.privacy.ConsentFixtures.MARKETING;
import static com.mysms.privacy.ConsentFixtures.MY_CONSENTS;
import static com.mysms.privacy.ConsentFixtures.PHOTOS;
import static com.mysms.privacy.ConsentFixtures.SELF_SERVICE;
import static com.mysms.privacy.ConsentFixtures.decision;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.privacy.business.ConsentExpiryJob;
import com.mysms.privacy.business.MajorityJob;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.events.GuardianLinkSync;
import com.mysms.privacy.events.SubjectSync;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The scheduled privacy jobs against real PostgreSQL, run as the job runtime runs them (in the tenant's call
 * context): a minor whose majority date has passed becomes an adult and their guardian's grant lapses, a decision past
 * its last day expires — each leaving evidence, an event and true answers, and changing nothing when run again — and a
 * tenant is enrolled in each job by the work that needs it. Dates are moved back in the test's own database, since
 * the jobs read the real clock.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "mysms.jobs.enabled=false")
class ScheduledPrivacyJobsComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final LocalDate YESTERDAY = LocalDate.now(ZoneOffset.UTC).minusDays(1);

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
    private MajorityJob majority;

    @Autowired
    private ConsentExpiryJob expiry;

    private PrivacyCalls calls;
    private ConsentFixtures fixtures;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "privacy");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @BeforeEach
    void connect() {
        calls = new PrivacyCalls(port, TOKENS);
        fixtures = new ConsentFixtures(calls, database, subjects, links);
    }

    @Test
    void aMinorWhoseMajorityDatePassedBecomesAnAdultAndTheGuardiansGrantLapses() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID parent = UUID.randomUUID();
        fixtures.minor(tenant, child);
        fixtures.guardian(tenant, parent, child);
        String version = fixtures.enable(tenant, PHOTOS);
        assertThat(calls.post(MY_CONSENTS, calls.person(tenant, parent, SELF_SERVICE),
                decision(child, PHOTOS, "institution", "granted", version, "en")).status()).isEqualTo(201);
        assertThat(enrolled(tenant, MajorityJob.CODE)).as("a minor enrols the tenant").isTrue();
        write(tenant, "UPDATE privacy.subject SET majority_date = :day WHERE tenant_id = :tenantId AND person_id = :id",
                child);

        run(tenant, majority::run);

        assertThat(single(tenant, "SELECT s.status FROM privacy.subject s WHERE s.tenant_id = :tenantId "
                + "AND s.person_id = :id", child)).isEqualTo("adult");
        assertThat(evidence(tenant)).containsExactly("recorded", "majority_lapsed");
        assertThat(state(tenant, child, PHOTOS)).containsOnly("class:NOT_ANSWERED", "institution:NOT_ANSWERED",
                "inter_school:NOT_ANSWERED", "public:NOT_ANSWERED");
        assertThat(events(tenant, "privacy.subject.majority_reached")).isEqualTo(1);

        run(tenant, majority::run);
        assertThat(evidence(tenant)).as("a repeated run changes nothing").hasSize(2);
        assertThat(events(tenant, "privacy.subject.majority_reached")).isEqualTo(1);
    }

    @Test
    void aDecisionPastItsLastDayExpiresOnce() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        withValidity(MARKETING, 12);
        String version = fixtures.enable(tenant, MARKETING);
        assertThat(calls.post(MY_CONSENTS, calls.person(tenant, self, SELF_SERVICE),
                decision(self, MARKETING, "tenant", "granted", version, "en")).status()).isEqualTo(201);
        assertThat(enrolled(tenant, ConsentExpiryJob.CODE)).as("a decision with a last day enrols the tenant").isTrue();
        assertThat(state(tenant, self, MARKETING)).containsExactly("tenant:CONSENT");
        write(tenant, "UPDATE privacy.consent_record SET valid_until = :day WHERE tenant_id = :tenantId "
                + "AND subject_person_id = :id", self);

        run(tenant, expiry::run);

        assertThat(evidence(tenant)).containsExactly("recorded", "expired");
        assertThat(state(tenant, self, MARKETING)).containsExactly("tenant:EXPIRED");
        assertThat(events(tenant, "privacy.consent_record.expired")).isEqualTo(1);

        run(tenant, expiry::run);
        assertThat(evidence(tenant)).as("a repeated run changes nothing").hasSize(2);
    }

    @Test
    void aTenantWithoutWorkIsNeverEnrolled() {
        UUID tenant = UUID.randomUUID();
        fixtures.adult(tenant, UUID.randomUUID());
        assertThat(enrolled(tenant, MajorityJob.CODE)).isFalse();
        assertThat(enrolled(tenant, ConsentExpiryJob.CODE)).isFalse();
    }

    /** Gives the catalogue purpose a default validity (no seeded purpose has one), in this test's own database. */
    private void withValidity(String purposeCode, int months) {
        TestTransactions.of(database).writeWithoutTenant(() -> database.jdbc().sql(
                "UPDATE privacy.purpose SET default_valid_months = :months WHERE code = :code")
                .param("months", months).param("code", purposeCode).update());
    }

    private void run(UUID tenant, Runnable job) {
        CallContextHolder.callAs(context(tenant), () -> {
            job.run();
            return null;
        });
    }

    private void write(UUID tenant, String sql, UUID id) {
        CallContextHolder.callAs(context(tenant), () -> TestTransactions.of(database).write(() -> database.jdbc()
                .sql(sql).param("day", YESTERDAY).param("id", id).update()));
    }

    private String single(UUID tenant, String sql, UUID id) {
        return fixtures.read(tenant, () -> database.jdbc().sql(sql).param("id", id).query(String.class).single());
    }

    private boolean enrolled(UUID tenant, String jobCode) {
        return fixtures.read(tenant, () -> database.jdbc().sql("""
                SELECT EXISTS (SELECT 1 FROM privacy.job_schedule j WHERE j.tenant_id = :tenantId AND j.job_code = :job)
                """).param("job", jobCode).query(Boolean.class).single());
    }

    private List<String> evidence(UUID tenant) {
        return fixtures.read(tenant, () -> database.jdbc().sql("""
                SELECT e.event FROM privacy.consent_evidence e WHERE e.tenant_id = :tenantId ORDER BY e.occurred_at
                """).query(String.class).list());
    }

    private List<String> state(UUID tenant, UUID subject, String purposeCode) {
        return fixtures.read(tenant, () -> database.jdbc().sql("""
                SELECT s.scope || ':' || s.reason
                  FROM privacy.consent_state s
                 WHERE s.tenant_id = :tenantId AND s.subject_person_id = :subject AND s.purpose_code = :purpose
                 ORDER BY s.scope
                """).param("subject", subject).param("purpose", purposeCode).query(String.class).list());
    }

    private int events(UUID tenant, String type) {
        return fixtures.read(tenant, () -> database.jdbc().sql("""
                SELECT count(*) FROM privacy.outbox_event o WHERE o.tenant_id = :tenantId AND o.event_type = :type
                """).param("type", type).query(Integer.class).single());
    }

    private static CallContext context(UUID tenant) {
        return CallContext.anonymous(TRACE).withIdentity(tenant, null);
    }
}
