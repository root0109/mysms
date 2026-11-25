package com.mysms.privacy;

import static com.mysms.privacy.ConsentFixtures.MARKETING;
import static com.mysms.privacy.ConsentFixtures.MY_CONSENTS;
import static com.mysms.privacy.ConsentFixtures.SELF_SERVICE;
import static com.mysms.privacy.ConsentFixtures.decision;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.events.GuardianLinkSync;
import com.mysms.privacy.events.SubjectSync;
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
 * Consent coverage against real PostgreSQL through the real edge: per width, the subjects allowed, denied by reason
 * and not answered, over the tenant or one institution's students — counts only, never who; the whole tenant needs a
 * tenant-wide grant and an institution must be within the caller's.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsentCoverageComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String COVERAGE = "/v1/privacy/coverage?purposeCode=";
    private static final String READ = "privacy.coverage.read";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(PrivacyDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private SubjectSync subjects;

    @Autowired
    private GuardianLinkSync links;

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

    /** A tenant with marketing on: one adult granted it, one refused, one decided nothing; the first is a student. */
    private record World(UUID tenant, UUID granted, UUID institution) {
    }

    private World world() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID granted = UUID.randomUUID();
        UUID refused = UUID.randomUUID();
        UUID institution = UUID.randomUUID();
        fixtures.adult(tenant, granted);
        fixtures.adult(tenant, refused);
        fixtures.adult(tenant, UUID.randomUUID());
        fixtures.student(tenant, granted, institution);
        String version = fixtures.enable(tenant, MARKETING);
        assertThat(calls.post(MY_CONSENTS, calls.person(tenant, granted, SELF_SERVICE),
                decision(granted, MARKETING, "tenant", "granted", version, "en")).status()).isEqualTo(201);
        assertThat(calls.post(MY_CONSENTS, calls.person(tenant, refused, SELF_SERVICE),
                decision(refused, MARKETING, "tenant", "refused", version, "en")).status()).isEqualTo(201);
        return new World(tenant, granted, institution);
    }

    private static long denied(JsonNode scope, String reason) {
        for (JsonNode denial : scope.path("denied")) {
            if (reason.equals(denial.path("reason").asString())) {
                return denial.path("subjects").asLong();
            }
        }
        throw new AssertionError("no " + reason + " entry");
    }

    @Test
    void coverageCountsTheSubjectsOfEachAnswerNeverWhoTheyAre() throws Exception {
        World world = world();
        String admin = calls.administrator(world.tenant(), READ);

        PrivacyCalls.Reply tenantWide = calls.get(COVERAGE + MARKETING, admin);
        assertThat(tenantWide.status()).as(tenantWide.body().toString()).isEqualTo(200);
        assertThat(tenantWide.body().path("subjects").asInt()).isEqualTo(3);
        JsonNode scopes = tenantWide.body().path("scopes");
        assertThat(scopes).hasSize(1);
        JsonNode tenant = scopes.get(0);
        assertThat(tenant.path("scope").asString()).isEqualTo("tenant");
        assertThat(tenant.path("allowed").asInt()).isEqualTo(1);
        assertThat(denied(tenant, "refused")).isEqualTo(1);
        assertThat(tenant.path("notAnswered").asInt()).isEqualTo(1);
        assertThat(tenant.path("denied")).as("every reason, zero included").hasSize(6);
        assertThat(tenantWide.body().toString()).doesNotContain(world.granted().toString());

        JsonNode school = calls.get(COVERAGE + MARKETING + "&institutionId=" + world.institution(),
                calls.schoolAdministratorOf(world.tenant(), world.institution(), READ)).body();
        assertThat(school.path("subjects").asInt()).as("the institution's students only").isEqualTo(1);
        assertThat(school.path("scopes").get(0).path("allowed").asInt()).isEqualTo(1);
        assertThat(school.path("scopes").get(0).path("notAnswered").asInt()).isZero();
    }

    @Test
    void coverageNeedsItsReachAKnownPurposeAndAWidthThePurposeTakes() throws Exception {
        World world = world();
        String school = calls.schoolAdministratorOf(world.tenant(), world.institution(), READ);

        assertThat(calls.get(COVERAGE + MARKETING, school).status()).as("the whole tenant is tenant-wide")
                .isEqualTo(403);
        assertThat(calls.get(COVERAGE + MARKETING + "&institutionId=" + UUID.randomUUID(), school).status())
                .as("another school").isEqualTo(403);
        String admin = calls.administrator(world.tenant(), READ);
        assertThat(calls.get(COVERAGE + "no_such_purpose", admin).status()).isEqualTo(404);
        assertThat(PrivacyCalls.fieldErrors(calls.get(COVERAGE + MARKETING + "&scope=class", admin)))
                .containsExactly("scope:SCOPE_NOT_APPLICABLE");
        assertThat(PrivacyCalls.fieldErrors(calls.get(COVERAGE + MARKETING + "&scope=galaxy", admin)))
                .containsExactly("scope:INVALID_ENUM");
        assertThat(calls.get(COVERAGE + MARKETING, calls.administrator(world.tenant(), "privacy.breach.read"))
                .status()).isEqualTo(403);
    }
}
