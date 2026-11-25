package com.mysms.privacy;

import static com.mysms.privacy.ConsentFixtures.MY_CONSENTS;
import static com.mysms.privacy.ConsentFixtures.PHOTOS;
import static com.mysms.privacy.ConsentFixtures.SELF_SERVICE;
import static com.mysms.privacy.ConsentFixtures.decision;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.events.GuardianLinkSync;
import com.mysms.privacy.events.SubjectSync;
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
 * The consent answers kept true by what changes them besides a decision, against real PostgreSQL: a minor who comes
 * of age, a guardian link that ends, the tenant switching the purpose off and on, and a notice version that asks
 * everyone again — each recomputes the answers in its own transaction and publishes privacy.consent_state.changed.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsentRecomputeComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    /** A guardian's grant at institution width: wider widths are denied by the narrower grant (rule step 4). */
    private static final List<String> GRANTED_AT_INSTITUTION = List.of("class:CONSENT", "institution:CONSENT",
            "inter_school:NARROWER_GRANT", "public:NARROWER_GRANT");

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

    /** A minor whose guardian granted photos at institution width; answers the guardian's person id. */
    private UUID grantedByGuardian(UUID tenant, UUID child) throws Exception {
        UUID parent = UUID.randomUUID();
        fixtures.minor(tenant, child);
        fixtures.guardian(tenant, parent, child);
        String version = fixtures.enable(tenant, PHOTOS);
        PrivacyCalls.Reply granted = calls.post(MY_CONSENTS, calls.person(tenant, parent, SELF_SERVICE),
                decision(child, PHOTOS, "institution", "granted", version, "en"));
        assertThat(granted.status()).as(granted.body().toString()).isEqualTo(201);
        assertThat(state(tenant, child)).containsExactlyElementsOf(GRANTED_AT_INSTITUTION);
        return parent;
    }

    private List<String> state(UUID tenant, UUID subject) {
        return fixtures.read(tenant, () -> database.jdbc().sql("""
                SELECT s.scope || ':' || s.reason
                  FROM privacy.consent_state s
                 WHERE s.tenant_id = :tenantId AND s.subject_person_id = :subject AND s.purpose_code = :purpose
                 ORDER BY s.scope
                """).param("subject", subject).param("purpose", PHOTOS).query(String.class).list());
    }

    /** How many privacy.consent_state.changed events the subject's answers have published. */
    private int changes(UUID tenant, UUID subject) {
        return fixtures.read(tenant, () -> database.jdbc().sql("""
                SELECT count(*)
                  FROM privacy.outbox_event o
                 WHERE o.tenant_id = :tenantId AND o.event_type = 'privacy.consent_state.changed'
                   AND o.aggregate_id = :subject
                """).param("subject", subject).query(Integer.class).single());
    }

    @Test
    void aMinorWhoComesOfAgeNoLongerHasTheGuardiansGrantCount() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        grantedByGuardian(tenant, child);
        int before = changes(tenant, child);

        fixtures.grownUp(tenant, child);

        assertThat(state(tenant, child)).containsOnly("class:NOT_ANSWERED", "institution:NOT_ANSWERED",
                "inter_school:NOT_ANSWERED", "public:NOT_ANSWERED");
        assertThat(changes(tenant, child)).isEqualTo(before + 1);
    }

    @Test
    void aGuardianLinkThatEndsTakesTheGuardiansGrantWithIt() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID parent = UUID.randomUUID();
        fixtures.minor(tenant, child);
        UUID link = fixtures.guardian(tenant, parent, child);
        String version = fixtures.enable(tenant, PHOTOS);
        assertThat(calls.post(MY_CONSENTS, calls.person(tenant, parent, SELF_SERVICE),
                decision(child, PHOTOS, "institution", "granted", version, "en")).status()).isEqualTo(201);
        assertThat(state(tenant, child)).containsExactlyElementsOf(GRANTED_AT_INSTITUTION);

        fixtures.unlink(tenant, link, parent, child);

        assertThat(state(tenant, child)).containsOnly("class:NOT_ANSWERED", "institution:NOT_ANSWERED",
                "inter_school:NOT_ANSWERED", "public:NOT_ANSWERED");
    }

    @Test
    void aPurposeSwitchedOffDeniesEverySubjectAndSwitchedOnAgainRestoresTheGrant() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        grantedByGuardian(tenant, child);
        String admin = calls.administrator(tenant, "privacy.tenant_purpose.read", "privacy.tenant_purpose.update");
        String photos = "/v1/privacy/purposes/" + purposeId(admin);

        PrivacyCalls.Reply off = calls.patch(photos, admin, calls.get(photos, admin).etag(), Map.of("enabled", false));
        assertThat(off.status()).as(off.body().toString()).isEqualTo(200);
        assertThat(state(tenant, child)).containsOnly("class:PURPOSE_DISABLED", "institution:PURPOSE_DISABLED",
                "inter_school:PURPOSE_DISABLED", "public:PURPOSE_DISABLED");

        PrivacyCalls.Reply on = calls.patch(photos, admin, off.etag(), Map.of("enabled", true));
        assertThat(on.status()).as(on.body().toString()).isEqualTo(200);
        assertThat(state(tenant, child)).containsExactlyElementsOf(GRANTED_AT_INSTITUTION);
    }

    @Test
    void aNoticeVersionThatAsksAgainSetsAsideEarlierGrantsOnly() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID parent = grantedByGuardian(tenant, child);

        String asksAgain = fixtures.reconsent(tenant, PHOTOS);

        assertThat(state(tenant, child)).as("a narrower grant restricts even before the new notice").containsExactly(
                "class:RECONSENT_REQUIRED", "institution:RECONSENT_REQUIRED", "inter_school:NARROWER_GRANT",
                "public:NARROWER_GRANT");
        assertThat(calls.post(MY_CONSENTS, calls.person(tenant, parent, SELF_SERVICE),
                decision(child, PHOTOS, "institution", "granted", asksAgain, "en")).status()).isEqualTo(201);
        assertThat(state(tenant, child)).containsExactlyElementsOf(GRANTED_AT_INSTITUTION);
    }

    @Test
    void oneTenantsPurposeSettingNeverTouchesAnothersAnswers() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID otherChild = UUID.randomUUID();
        grantedByGuardian(tenant, child);
        grantedByGuardian(other, otherChild);
        String admin = calls.administrator(tenant, "privacy.tenant_purpose.read", "privacy.tenant_purpose.update");
        String photos = "/v1/privacy/purposes/" + purposeId(admin);

        assertThat(calls.patch(photos, admin, calls.get(photos, admin).etag(), Map.of("enabled", false)).status())
                .isEqualTo(200);

        assertThat(state(tenant, child)).containsOnly("class:PURPOSE_DISABLED", "institution:PURPOSE_DISABLED",
                "inter_school:PURPOSE_DISABLED", "public:PURPOSE_DISABLED");
        assertThat(state(other, otherChild)).containsExactlyElementsOf(GRANTED_AT_INSTITUTION);
    }

    private String purposeId(String admin) throws Exception {
        for (JsonNode purpose : calls.get("/v1/privacy/purposes?size=500", admin).body().path("data")) {
            if (PHOTOS.equals(purpose.path("code").asString())) {
                return purpose.path("id").asString();
            }
        }
        throw new AssertionError("no catalogue purpose " + PHOTOS);
    }
}
