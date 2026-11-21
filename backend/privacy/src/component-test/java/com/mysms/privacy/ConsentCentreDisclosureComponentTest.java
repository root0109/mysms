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
 * Design §13 "a guardian never learns another guardian's decision", against real PostgreSQL through the real edge:
 * two legal guardians of one child decide differently, the most restrictive decision wins in the effective state,
 * and each guardian's consent centre shows their own decision only — never the other's record, its id or its answer.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsentCentreDisclosureComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();

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

    /** The caller's centre row of the subject and purpose. */
    private static JsonNode row(PrivacyCalls.Reply centre, UUID subject, String purposeCode) {
        for (JsonNode row : centre.body().path("data")) {
            if (subject.toString().equals(row.path("subjectPersonId").asString())
                    && purposeCode.equals(row.path("purposeCode").asString())) {
                return row;
            }
        }
        throw new AssertionError("no row of " + purposeCode + " in " + centre.body());
    }

    private List<String> state(UUID tenant, UUID subject) {
        return fixtures.read(tenant, () -> database.jdbc().sql("""
                SELECT s.scope || ':' || s.result || ':' || s.reason
                  FROM privacy.consent_state s
                 WHERE s.tenant_id = :tenantId AND s.subject_person_id = :subject AND s.purpose_code = :purpose
                 ORDER BY s.scope
                """).param("subject", subject).param("purpose", PHOTOS).query(String.class).list());
    }

    @Test
    void oneGuardianGrantsTheOtherRefusesAndNeitherSeesTheOthersDecision() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID mother = UUID.randomUUID();
        UUID father = UUID.randomUUID();
        fixtures.minor(tenant, child);
        fixtures.adult(tenant, mother);
        fixtures.adult(tenant, father);
        fixtures.guardian(tenant, mother, child);
        fixtures.guardian(tenant, father, child);
        String version = fixtures.enable(tenant, PHOTOS);
        String motherToken = calls.person(tenant, mother, SELF_SERVICE);
        String fatherToken = calls.person(tenant, father, SELF_SERVICE);

        PrivacyCalls.Reply granted = calls.post(MY_CONSENTS, motherToken,
                decision(child, PHOTOS, "class", "granted", version, "en"));
        assertThat(granted.status()).as(granted.body().toString()).isEqualTo(201);
        assertThat(granted.text("myStatus")).isEqualTo("granted");
        assertThat(granted.text("effectiveWidestScope")).isEqualTo("class");
        assertThat(state(tenant, child)).contains("class:allowed:CONSENT", "institution:denied:NARROWER_GRANT");

        PrivacyCalls.Reply refused = calls.post(MY_CONSENTS, fatherToken,
                decision(child, PHOTOS, "class", "refused", version, "en"));
        assertThat(refused.status()).as(refused.body().toString()).isEqualTo(201);
        assertThat(refused.text("myStatus")).isEqualTo("refused");
        assertThat(refused.body().has("effectiveWidestScope")).isFalse();
        assertThat(state(tenant, child)).containsExactly("class:denied:REFUSED", "institution:denied:REFUSED",
                "inter_school:denied:REFUSED", "public:denied:REFUSED");

        PrivacyCalls.Reply motherCentre = calls.get(MY_CONSENTS, motherToken);
        assertThat(motherCentre.status()).isEqualTo(200);
        JsonNode motherRow = row(motherCentre, child, PHOTOS);
        assertThat(motherRow.path("id").asString()).isEqualTo(granted.text("id"));
        assertThat(motherRow.path("myStatus").asString()).isEqualTo("granted");
        assertThat(motherRow.path("scope").asString()).isEqualTo("class");
        assertThat(motherRow.has("effectiveWidestScope")).as("denied at every width: nothing is shared").isFalse();
        assertThat(motherCentre.body().toString()).doesNotContain(refused.text("id")).doesNotContain("refused");

        PrivacyCalls.Reply fatherCentre = calls.get(MY_CONSENTS, fatherToken);
        JsonNode fatherRow = row(fatherCentre, child, PHOTOS);
        assertThat(fatherRow.path("id").asString()).isEqualTo(refused.text("id"));
        assertThat(fatherRow.path("myStatus").asString()).isEqualTo("refused");
        assertThat(fatherCentre.body().toString()).doesNotContain(granted.text("id")).doesNotContain("\"granted\"");

        assertThat(calls.post(MY_CONSENTS + "/" + refused.text("id") + ":withdraw", motherToken, Map.of()).status())
                .as("another guardian's record is not the caller's to withdraw").isEqualTo(404);
    }

    @Test
    void aGuardianSilentAboutAChildDoesNotBlockTheOther() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID mother = UUID.randomUUID();
        UUID father = UUID.randomUUID();
        fixtures.minor(tenant, child);
        fixtures.guardian(tenant, mother, child);
        fixtures.guardian(tenant, father, child);
        String version = fixtures.enable(tenant, PHOTOS);

        PrivacyCalls.Reply granted = calls.post(MY_CONSENTS, calls.person(tenant, mother, SELF_SERVICE),
                decision(child, PHOTOS, "institution", "granted", version, "en"));
        assertThat(granted.status()).as(granted.body().toString()).isEqualTo(201);

        JsonNode fatherRow = row(calls.get(MY_CONSENTS, calls.person(tenant, father, SELF_SERVICE)), child, PHOTOS);
        assertThat(fatherRow.path("myStatus").asString()).isEqualTo("not_answered");
        assertThat(fatherRow.has("id")).isFalse();
        assertThat(fatherRow.path("effectiveWidestScope").asString()).isEqualTo("institution");
    }
}
