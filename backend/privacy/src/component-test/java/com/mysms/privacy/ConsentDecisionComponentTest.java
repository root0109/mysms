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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 * Consent decisions as another module's service asks for them, against real PostgreSQL: a stored answer, the rule's
 * answer for a subject who decided nothing, an activity on a non-consent basis (allowed) or unknown (denied, fail
 * closed), the order of a batch, a width the purpose does not take (422), the tenant's purpose status, the subjects a
 * grant allows, and each operation's own permission.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsentDecisionComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String DECISIONS = "/v1/privacy/consent-decisions";
    private static final String MARKETING_ACTIVITY = "comms.marketing_broadcast";
    private static final String SERVICE = TOKENS.service("comms", "privacy.consent_decision.check",
            "privacy.consent_decision.batch_check", "privacy.consent_decision.get_purpose_status",
            "privacy.consent_decision.list_subjects");

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

    /** A tenant with marketing switched on, one adult who granted it and one who decided nothing. */
    private record World(UUID tenant, UUID granted, UUID silent, String noticeVersion) {
    }

    private World world() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID granted = UUID.randomUUID();
        UUID silent = UUID.randomUUID();
        fixtures.adult(tenant, granted);
        fixtures.adult(tenant, silent);
        String version = fixtures.enable(tenant, MARKETING);
        PrivacyCalls.Reply grant = calls.post(MY_CONSENTS, calls.person(tenant, granted, SELF_SERVICE),
                decision(granted, MARKETING, "tenant", "granted", version, "en"));
        assertThat(grant.status()).as(grant.body().toString()).isEqualTo(201);
        return new World(tenant, granted, silent, version);
    }

    private JsonNode check(UUID tenant, String activity, UUID subject, String scope) throws Exception {
        PrivacyCalls.Reply reply = calls.asService("GET", DECISIONS + ":check?activityCode=" + activity
                + "&subjectPersonId=" + subject + "&scope=" + scope, SERVICE, tenant, null);
        assertThat(reply.status()).as(reply.body().toString()).isEqualTo(200);
        return reply.body();
    }

    @Test
    void aStoredAnswerTheRulesAnswerANonConsentBasisAndAnUnknownActivity() throws Exception {
        World world = world();

        JsonNode allowed = check(world.tenant(), MARKETING_ACTIVITY, world.granted(), "tenant");
        assertThat(allowed.path("allowed").asBoolean()).isTrue();
        assertThat(allowed.path("reason").asString()).isEqualTo("consent");
        assertThat(allowed.path("legalBasis").asString()).isEqualTo("consent");
        assertThat(Long.parseLong(allowed.path("stateVersion").asString())).isPositive();
        JsonNode silent = check(world.tenant(), MARKETING_ACTIVITY, world.silent(), "tenant");
        assertThat(silent.path("allowed").asBoolean()).isFalse();
        assertThat(silent.path("reason").asString()).isEqualTo("not_answered");
        assertThat(check(UUID.randomUUID(), MARKETING_ACTIVITY, world.granted(), "tenant").path("reason").asString())
                .as("another tenant has not switched the purpose on").isEqualTo("purpose_disabled");

        JsonNode operational = check(world.tenant(), "comms.operational_notice", world.silent(), "tenant");
        assertThat(operational.path("allowed").asBoolean()).isTrue();
        assertThat(operational.path("reason").asString()).isEqualTo("non_consent_basis");
        assertThat(operational.path("legalBasis").asString()).isEqualTo("legitimate_use");
        JsonNode unknown = check(world.tenant(), "comms.nothing_like_it", world.granted(), "tenant");
        assertThat(unknown.path("allowed").asBoolean()).isFalse();
        assertThat(unknown.path("reason").asString()).isEqualTo("unknown_activity");

        PrivacyCalls.Reply notItsWidth = calls.asService("GET", DECISIONS + ":check?activityCode=" + MARKETING_ACTIVITY
                + "&subjectPersonId=" + world.granted() + "&scope=class", SERVICE, world.tenant(), null);
        assertThat(PrivacyCalls.fieldErrors(notItsWidth)).containsExactly("scope:SCOPE_NOT_APPLICABLE");
    }

    @Test
    void aBatchAnswersInTheOrderAsked() throws Exception {
        World world = world();
        PrivacyCalls.Reply batch = calls.asService("POST", DECISIONS + ":batchCheck", SERVICE, world.tenant(),
                Map.of("activityCode", MARKETING_ACTIVITY, "scope", "tenant",
                        "subjectPersonIds", List.of(world.silent().toString(), world.granted().toString())));

        assertThat(batch.status()).as(batch.body().toString()).isEqualTo(200);
        assertThat(batch.body().path("decisions").findValuesAsString("subjectPersonId"))
                .containsExactly(world.silent().toString(), world.granted().toString());
        assertThat(batch.body().path("decisions").findValuesAsString("reason"))
                .containsExactly("not_answered", "consent");
    }

    @Test
    void thePurposeStatusAndTheSubjectsAGrantAllows() throws Exception {
        World world = world();
        PrivacyCalls.Reply status = calls.asService("GET", DECISIONS + ":purposeStatus?activityCode="
                + MARKETING_ACTIVITY, SERVICE, world.tenant(), null);
        assertThat(status.status()).as(status.body().toString()).isEqualTo(200);
        assertThat(status.text("purposeCode")).isEqualTo(MARKETING);
        assertThat(status.body().path("enabled").asBoolean()).isTrue();
        assertThat(status.text("activeNoticeVersionId")).isEqualTo(world.noticeVersion());
        assertThat(calls.asService("GET", DECISIONS + ":purposeStatus?activityCode=comms.nothing_like_it", SERVICE,
                world.tenant(), null).status()).isEqualTo(404);

        PrivacyCalls.Reply allowed = calls.asService("GET", DECISIONS + ":subjects?activityCode=" + MARKETING_ACTIVITY
                + "&scope=tenant", SERVICE, world.tenant(), null);
        assertThat(allowed.status()).as(allowed.body().toString()).isEqualTo(200);
        assertThat(allowed.body().path("data").findValuesAsString("subjectPersonId"))
                .containsExactly(world.granted().toString());
        assertThat(PrivacyCalls.fieldErrors(calls.asService("GET", DECISIONS
                + ":subjects?activityCode=comms.operational_notice&scope=tenant", SERVICE, world.tenant(), null)))
                .containsExactly("activityCode:ACTIVITY_NOT_CONSENT_BASED");
    }

    @Test
    void eachOperationNeedsItsOwnPermission() throws Exception {
        World world = world();
        String checkOnly = TOKENS.service("comms", "privacy.consent_decision.check");
        assertThat(calls.asService("POST", DECISIONS + ":batchCheck", checkOnly, world.tenant(),
                Map.of("activityCode", MARKETING_ACTIVITY, "scope", "tenant",
                        "subjectPersonIds", List.of(world.granted().toString()))).status()).isEqualTo(403);
        assertThat(calls.asService("GET", DECISIONS + ":purposeStatus?activityCode=" + MARKETING_ACTIVITY, checkOnly,
                world.tenant(), null).status()).isEqualTo(403);
    }

    @Test
    void onlyAServiceInATenantAsksAndWithAWellFormedQuestion() throws Exception {
        World world = world();
        String path = DECISIONS + ":check?activityCode=" + MARKETING_ACTIVITY + "&subjectPersonId=" + world.granted()
                + "&scope=tenant";
        assertThat(calls.get(path, calls.person(world.tenant(), world.granted(), SELF_SERVICE)).status())
                .as("a person's token never reaches an internal operation").isEqualTo(403);
        PrivacyCalls.Reply noTenant = calls.asService("GET", path, SERVICE, null, null);
        assertThat(noTenant.status()).as(noTenant.body().toString()).isEqualTo(400);

        assertThat(PrivacyCalls.fieldErrors(calls.asService("GET", path.replace("scope=tenant", "scope=galaxy"),
                SERVICE, world.tenant(), null))).containsExactly("scope:INVALID_ENUM");
        List<String> tooMany = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            tooMany.add(UUID.randomUUID().toString());
        }
        assertThat(batch(world.tenant(), Map.of("activityCode", MARKETING_ACTIVITY, "scope", "tenant",
                "subjectPersonIds", tooMany)).status()).isEqualTo(422);
        assertThat(batch(world.tenant(), Map.of("activityCode", MARKETING_ACTIVITY, "scope", "tenant",
                "subjectPersonIds", List.of())).status()).isEqualTo(422);
        assertThat(batch(world.tenant(), Map.of("activityCode", MARKETING_ACTIVITY,
                "subjectPersonIds", List.of(world.granted().toString()))).status()).as("no scope").isEqualTo(422);
    }

    @Test
    void theSubjectsAGrantAllowsComePageByPage() throws Exception {
        World world = world();
        UUID another = UUID.randomUUID();
        fixtures.adult(world.tenant(), another);
        assertThat(calls.post(MY_CONSENTS, calls.person(world.tenant(), another, SELF_SERVICE),
                decision(another, MARKETING, "tenant", "granted", world.noticeVersion(), "en")).status()).isEqualTo(201);

        PrivacyCalls.Reply first = calls.asService("GET", DECISIONS + ":subjects?activityCode=" + MARKETING_ACTIVITY
                + "&scope=tenant&size=1", SERVICE, world.tenant(), null);
        assertThat(first.body().path("data")).hasSize(1);
        String next = first.body().path("paging").path("next").asString();
        assertThat(next).contains("page=2").contains("cursor=");
        PrivacyCalls.Reply second = calls.asService("GET", next, SERVICE, world.tenant(), null);
        assertThat(second.status()).as(second.body().toString()).isEqualTo(200);
        List<String> both = new ArrayList<>(first.body().path("data").findValuesAsString("subjectPersonId"));
        both.addAll(second.body().path("data").findValuesAsString("subjectPersonId"));
        assertThat(both).containsExactlyInAnyOrder(world.granted().toString(), another.toString());
    }

    private PrivacyCalls.Reply batch(UUID tenant, Map<String, ?> body) throws Exception {
        return calls.asService("POST", DECISIONS + ":batchCheck", SERVICE, tenant, body);
    }
}
