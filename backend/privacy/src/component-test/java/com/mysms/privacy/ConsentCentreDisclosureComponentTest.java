package com.mysms.privacy;

import static com.mysms.privacy.ConsentFixtures.BEHAVIOURAL;
import static com.mysms.privacy.ConsentFixtures.MY_CONSENTS;
import static com.mysms.privacy.ConsentFixtures.NOTICE;
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
import java.time.LocalDate;
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
 * Design §13 "a guardian never learns another guardian's decision", against real PostgreSQL through the real edge:
 * two legal guardians of one child decide differently, the most restrictive decision wins in the effective state,
 * and each guardian's consent centre shows their own decision only — never the other's record, its id or its answer.
 * The child sees the effect read only (design §12 "Students"), with nothing of who decided what.
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

    /** The text values of a JSON array, in order. */
    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (JsonNode value : array) {
            values.add(value.asString());
        }
        return values;
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
        assertThat(motherRow.path("capacity").asString()).isEqualTo("guardian");
        assertThat(motherRow.path("noticeCode").asString()).isEqualTo(NOTICE);
        assertThat(motherRow.path("noticeVersionId").asString()).isEqualTo(version);
        assertThat(strings(motherRow.path("allowedScopes")))
                .containsExactly("class", "institution", "inter_school", "public");
        assertThat(motherRow.has("decidesFrom")).isFalse();
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

    @Test
    void aMinorSeesTheEffectReadOnlyAndNothingOfWhoDecided() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID mother = UUID.randomUUID();
        fixtures.minor(tenant, child);
        fixtures.adult(tenant, mother);
        fixtures.guardian(tenant, mother, child);
        String version = fixtures.enable(tenant, PHOTOS, BEHAVIOURAL);
        ceiling(tenant, PHOTOS, "institution");
        PrivacyCalls.Reply granted = calls.post(MY_CONSENTS, calls.person(tenant, mother, SELF_SERVICE),
                decision(child, PHOTOS, "institution", "granted", version, "en"));
        assertThat(granted.status()).as(granted.body().toString()).isEqualTo(201);
        assertThat(strings(granted.body().path("allowedScopes")))
                .as("the school's ceiling narrows the widths").containsExactly("class", "institution");

        String childToken = calls.person(tenant, child, SELF_SERVICE);
        PrivacyCalls.Reply centre = calls.get(MY_CONSENTS, childToken);
        assertThat(centre.status()).isEqualTo(200);
        assertThat(centre.body().path("data").size()).as("a purpose forbidden for minors has no row").isEqualTo(1);
        JsonNode own = row(centre, child, PHOTOS);
        assertThat(own.path("capacity").asString()).isEqualTo("minor_self");
        assertThat(own.path("effectiveWidestScope").asString()).isEqualTo("institution");
        assertThat(own.path("decidesFrom").asString()).isEqualTo(majorityDate(tenant, child).toString());
        assertThat(own.path("noticeCode").asString()).isEqualTo(NOTICE);
        assertThat(own.has("myStatus")).isFalse();
        assertThat(own.has("id")).isFalse();
        assertThat(own.has("scope")).isFalse();
        assertThat(own.has("etag")).isFalse();
        assertThat(centre.body().toString()).doesNotContain(granted.text("id")).doesNotContain(mother.toString())
                .doesNotContain("granted");

        assertThat(calls.post(MY_CONSENTS, childToken, decision(child, PHOTOS, "class", "refused", version, "en"))
                .status()).as("a minor does not decide for themself").isEqualTo(404);
    }

    @Test
    void aGuardianWhoseLinkEndedWithdrawsWithoutLearningTheChildsState() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID mother = UUID.randomUUID();
        UUID father = UUID.randomUUID();
        fixtures.minor(tenant, child);
        UUID motherLink = fixtures.guardian(tenant, mother, child);
        fixtures.guardian(tenant, father, child);
        String version = fixtures.enable(tenant, PHOTOS);
        String motherToken = calls.person(tenant, mother, SELF_SERVICE);
        PrivacyCalls.Reply granted = calls.post(MY_CONSENTS, motherToken,
                decision(child, PHOTOS, "institution", "granted", version, "en"));
        assertThat(granted.status()).as(granted.body().toString()).isEqualTo(201);
        assertThat(calls.post(MY_CONSENTS, calls.person(tenant, father, SELF_SERVICE),
                decision(child, PHOTOS, "institution", "granted", version, "en")).status()).isEqualTo(201);

        fixtures.unlink(tenant, motherLink, mother, child);
        assertThat(calls.get(MY_CONSENTS, motherToken).body().path("data").size())
                .as("the child is no longer in her centre").isZero();
        PrivacyCalls.Reply withdrawn = calls.post(MY_CONSENTS + "/" + granted.text("id") + ":withdraw", motherToken,
                Map.of("etag", granted.text("etag")));
        assertThat(withdrawn.status()).as(withdrawn.body().toString()).isEqualTo(200);
        assertThat(withdrawn.body().has("effectiveWidestScope"))
                .as("the father's grant still shares the photos; she is not told").isFalse();
    }

    @Test
    void aPersonOfUnknownAgeSeesNoRowOfTheirOwn() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID person = UUID.randomUUID();
        fixtures.ageUnknown(tenant, person);
        fixtures.enable(tenant, PHOTOS);
        PrivacyCalls.Reply centre = calls.get(MY_CONSENTS, calls.person(tenant, person, SELF_SERVICE));
        assertThat(centre.status()).isEqualTo(200);
        assertThat(centre.body().path("data").size()).isZero();
    }

    /** Sets the tenant's width ceiling of the purpose, as a tenant administrator. */
    private void ceiling(UUID tenant, String purposeCode, String maxScope) throws Exception {
        String admin = calls.administrator(tenant, "privacy.tenant_purpose.read", "privacy.tenant_purpose.update");
        for (JsonNode purpose : calls.get("/v1/privacy/purposes?size=500", admin).body().path("data")) {
            if (purposeCode.equals(purpose.path("code").asString())) {
                PrivacyCalls.Reply set = calls.patch("/v1/privacy/purposes/" + purpose.path("id").asString(), admin,
                        purpose.path("etag").asString(), Map.of("maxScope", maxScope));
                assertThat(set.status()).as(set.body().toString()).isEqualTo(200);
            }
        }
    }

    private LocalDate majorityDate(UUID tenant, UUID person) {
        return fixtures.read(tenant, () -> database.jdbc().sql("""
                SELECT s.majority_date FROM privacy.subject s WHERE s.tenant_id = :tenantId AND s.person_id = :person
                """).param("person", person).query(LocalDate.class).single());
    }
}
