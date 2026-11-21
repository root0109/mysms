package com.mysms.privacy;

import static com.mysms.privacy.ConsentFixtures.BEHAVIOURAL;
import static com.mysms.privacy.ConsentFixtures.CONSENT_RECORDS;
import static com.mysms.privacy.ConsentFixtures.ENGLISH;
import static com.mysms.privacy.ConsentFixtures.HINDI;
import static com.mysms.privacy.ConsentFixtures.MARKETING;
import static com.mysms.privacy.ConsentFixtures.MY_CONSENTS;
import static com.mysms.privacy.ConsentFixtures.NOTICE;
import static com.mysms.privacy.ConsentFixtures.PHOTOS;
import static com.mysms.privacy.ConsentFixtures.SELF_SERVICE;
import static com.mysms.privacy.ConsentFixtures.decision;
import static com.mysms.privacy.ConsentFixtures.paperForm;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.events.GuardianLinkSync;
import com.mysms.privacy.events.SubjectSync;
import com.mysms.privacy.model.EvidenceEntry;
import java.time.OffsetDateTime;
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

/**
 * Consent records against real PostgreSQL through the real edge: an adult's own decisions (supersede, withdraw) and
 * the hash-chained evidence they leave, who may decide for whom (404 for anyone else, 403 while impersonating or
 * without a person), the 422 checks of a decision, the four-eyes rule on paper forms, tenant isolation, and the notice
 * the consent centre shows.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsentRecordComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String[] STAFF = {"privacy.consent_record.read", "privacy.consent_record.record_on_behalf",
        "privacy.consent_record.verify"};
    private static final String EVIDENCE = """
            SELECT e.id, e.tenant_id, e.consent_record_id, e.event, e.actor_user_id, e.actor_person_id,
                   e.on_behalf_of_capacity, e.channel, e.notice_content_sha256, host(e.ip_address) AS ip_address,
                   e.user_agent_hash, e.impersonator_user_id, e.occurred_at, e.previous_hash, e.entry_hash
              FROM privacy.consent_evidence e
             WHERE e.tenant_id = :tenantId
             ORDER BY e.occurred_at
            """;

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

    /** One evidence entry as stored, with the hash the database holds for it. */
    private record Stored(EvidenceEntry entry, String entryHash) {
    }

    private List<Stored> evidence(UUID tenant) {
        return fixtures.read(tenant, () -> database.jdbc().sql(EVIDENCE).query((rs, row) -> new Stored(
                new EvidenceEntry(rs.getObject("id", UUID.class), rs.getObject("tenant_id", UUID.class),
                        rs.getObject("consent_record_id", UUID.class), rs.getString("event"),
                        rs.getObject("actor_user_id", UUID.class), rs.getObject("actor_person_id", UUID.class),
                        rs.getString("on_behalf_of_capacity"), rs.getString("channel"),
                        rs.getString("notice_content_sha256"), rs.getString("ip_address"),
                        rs.getString("user_agent_hash"), rs.getObject("impersonator_user_id", UUID.class),
                        rs.getObject("occurred_at", OffsetDateTime.class), rs.getString("previous_hash")),
                rs.getString("entry_hash"))).list());
    }

    private String chainHead(UUID tenant) {
        return fixtures.read(tenant, () -> database.jdbc().sql(
                "SELECT c.last_entry_hash FROM privacy.evidence_chain c WHERE c.tenant_id = :tenantId")
                .query(String.class).single());
    }

    private List<String> state(UUID tenant, UUID subject, String purposeCode) {
        return fixtures.read(tenant, () -> database.jdbc().sql("""
                SELECT s.scope || ':' || s.reason
                  FROM privacy.consent_state s
                 WHERE s.tenant_id = :tenantId AND s.subject_person_id = :subject AND s.purpose_code = :purpose
                 ORDER BY s.scope
                """).param("subject", subject).param("purpose", purposeCode).query(String.class).list());
    }

    @Test
    void anAdultDecidesSupersedesAndWithdrawsLeavingAnUnbrokenEvidenceChain() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String version = fixtures.enable(tenant, PHOTOS);
        String token = calls.person(tenant, self, SELF_SERVICE);
        String staff = calls.administrator(tenant, STAFF);

        PrivacyCalls.Reply granted = calls.post(MY_CONSENTS, token,
                decision(self, PHOTOS, "institution", "granted", version, "en"));
        assertThat(granted.status()).as(granted.body().toString()).isEqualTo(201);
        assertThat(granted.text("myStatus")).isEqualTo("granted");
        assertThat(granted.text("effectiveWidestScope")).isEqualTo("institution");
        PrivacyCalls.Reply first = calls.get(CONSENT_RECORDS + "/" + granted.text("id"), staff);
        assertThat(first.text("giverCapacity")).isEqualTo("self");
        assertThat(first.text("channel")).isEqualTo("web");
        assertThat(first.text("verificationStatus")).isEqualTo("verified");
        assertThat(first.text("verificationMethod")).isEqualTo("existing_record");
        assertThat(state(tenant, self, PHOTOS)).containsExactly("class:CONSENT", "institution:CONSENT",
                "inter_school:NOT_ANSWERED", "public:NOT_ANSWERED");

        PrivacyCalls.Reply refused = calls.post(MY_CONSENTS, token,
                decision(self, PHOTOS, "class", "refused", version, "hi"));
        assertThat(refused.status()).as(refused.body().toString()).isEqualTo(201);
        assertThat(refused.text("myStatus")).isEqualTo("refused");
        assertThat(calls.get(CONSENT_RECORDS + "/" + granted.text("id"), staff).text("supersededById"))
                .as("the newer decision supersedes the live one").isEqualTo(refused.text("id"));
        assertThat(state(tenant, self, PHOTOS)).containsExactly("class:REFUSED", "institution:REFUSED",
                "inter_school:REFUSED", "public:REFUSED");

        String withdraw = MY_CONSENTS + "/" + refused.text("id") + ":withdraw";
        PrivacyCalls.Reply withdrawn = calls.post(withdraw, token, Map.of("etag", refused.text("etag")));
        assertThat(withdrawn.status()).as(withdrawn.body().toString()).isEqualTo(200);
        assertThat(withdrawn.text("myStatus")).isEqualTo("not_answered");
        assertThat(withdrawn.body().has("id")).isFalse();
        assertThat(calls.post(withdraw, token, Map.of()).status()).as("no longer live").isEqualTo(404);
        assertThat(state(tenant, self, PHOTOS)).containsOnly("class:NOT_ANSWERED", "institution:NOT_ANSWERED",
                "inter_school:NOT_ANSWERED", "public:NOT_ANSWERED");

        List<Stored> chain = evidence(tenant);
        assertThat(chain).extracting(s -> s.entry().event())
                .containsExactly("recorded", "superseded", "recorded", "withdrawn");
        assertThat(chain).extracting(s -> s.entry().consentRecordId().toString()).containsExactly(
                granted.text("id"), granted.text("id"), refused.text("id"), refused.text("id"));
        String previous = null;
        for (Stored stored : chain) {
            assertThat(stored.entry().previousHash()).as("names its predecessor").isEqualTo(previous);
            assertThat(stored.entry().entryHash()).as("recomputed hash").isEqualTo(stored.entryHash());
            assertThat(stored.entry().actorPersonId()).isEqualTo(self);
            assertThat(stored.entry().ipAddress()).isNotNull();
            assertThat(stored.entry().userAgentHash()).matches("[0-9a-f]{64}");
            previous = stored.entryHash();
        }
        assertThat(chainHead(tenant)).isEqualTo(previous);
        assertThat(chain.get(0).entry().noticeContentSha256()).matches("[0-9a-f]{64}");
        assertThat(chain.get(2).entry().noticeContentSha256()).as("the Hindi wording was shown")
                .isNotEqualTo(chain.get(0).entry().noticeContentSha256());
    }

    @Test
    void onlyTheAdultOrAnActiveLegalGuardianMayDecide() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID grownUp = UUID.randomUUID();
        UUID parent = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        fixtures.minor(tenant, child);
        fixtures.adult(tenant, grownUp);
        fixtures.adult(tenant, stranger);
        fixtures.guardian(tenant, parent, child);
        fixtures.guardian(tenant, parent, grownUp);
        String version = fixtures.enable(tenant, PHOTOS);

        PrivacyCalls.Reply notGuardian = calls.post(MY_CONSENTS, calls.person(tenant, stranger, SELF_SERVICE),
                decision(child, PHOTOS, "class", "granted", version, "en"));
        assertThat(notGuardian.status()).isEqualTo(404);
        assertThat(notGuardian.text("code")).isEqualTo("NOT_FOUND");
        assertThat(calls.post(MY_CONSENTS, calls.person(tenant, child, SELF_SERVICE),
                decision(child, PHOTOS, "class", "granted", version, "en")).status())
                .as("a minor does not decide for themself").isEqualTo(404);
        assertThat(calls.post(MY_CONSENTS, calls.person(tenant, parent, SELF_SERVICE),
                decision(grownUp, PHOTOS, "class", "granted", version, "en")).status())
                .as("a guardian does not decide for an adult").isEqualTo(404);
        assertThat(calls.post(MY_CONSENTS, calls.person(tenant, parent, SELF_SERVICE),
                decision(UUID.randomUUID(), PHOTOS, "class", "granted", version, "en")).status())
                .as("an unknown person looks like any other").isEqualTo(404);

        PrivacyCalls.Reply impersonated = calls.post(MY_CONSENTS,
                calls.impersonated(tenant, parent, UUID.randomUUID(), SELF_SERVICE),
                decision(child, PHOTOS, "class", "granted", version, "en"));
        assertThat(impersonated.status()).isEqualTo(403);
        assertThat(impersonated.text("code")).isEqualTo("PERMISSION_DENIED");
        assertThat(calls.post(MY_CONSENTS, calls.administrator(tenant, SELF_SERVICE),
                decision(child, PHOTOS, "class", "granted", version, "en")).status())
                .as("an account linked to no person decides for nobody").isEqualTo(403);

        PrivacyCalls.Reply asGuardian = calls.post(MY_CONSENTS, calls.person(tenant, parent, SELF_SERVICE),
                decision(child, PHOTOS, "class", "granted", version, "en"));
        assertThat(asGuardian.status()).as(asGuardian.body().toString()).isEqualTo(201);
        assertThat(calls.get(CONSENT_RECORDS + "/" + asGuardian.text("id"), calls.administrator(tenant, STAFF))
                .text("giverCapacity")).isEqualTo("guardian");
        assertThat(calls.post(MY_CONSENTS + "/" + asGuardian.text("id") + ":withdraw",
                calls.impersonated(tenant, parent, UUID.randomUUID(), SELF_SERVICE), Map.of()).status())
                .as("no withdrawal while impersonating either").isEqualTo(403);
        assertThat(evidence(tenant)).hasSize(1);
    }

    @Test
    void aDecisionIsCheckedAgainstThePurposeTheScopeAndTheNoticeShown() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID parent = UUID.randomUUID();
        fixtures.minor(tenant, child);
        fixtures.guardian(tenant, parent, child);
        String version = fixtures.enable(tenant, PHOTOS, BEHAVIOURAL);
        String token = calls.person(tenant, parent, SELF_SERVICE);

        assertThat(PrivacyCalls.fieldErrors(calls.post(MY_CONSENTS, token,
                decision(child, MARKETING, "tenant", "granted", version, "en"))))
                .contains("purposeCode:CONSENT_PURPOSE_DISABLED");
        assertThat(PrivacyCalls.fieldErrors(calls.post(MY_CONSENTS, token,
                decision(child, BEHAVIOURAL, "tenant", "granted", version, "en"))))
                .containsExactly("purposeCode:CONSENT_FORBIDDEN_FOR_MINOR");
        assertThat(PrivacyCalls.fieldErrors(calls.post(MY_CONSENTS, token,
                decision(child, PHOTOS, "tenant", "granted", version, "en"))))
                .containsExactly("scope:CONSENT_SCOPE_ABOVE_TENANT_MAX");
        assertThat(PrivacyCalls.fieldErrors(calls.post(MY_CONSENTS, token,
                decision(child, PHOTOS, "class", "granted", version, "fr"))))
                .containsExactly("noticeLanguage:CONSENT_NOTICE_NOT_ACTIVE");
        assertThat(PrivacyCalls.fieldErrors(calls.post(MY_CONSENTS, token,
                decision(child, PHOTOS, "class", "granted", UUID.randomUUID().toString(), "en"))))
                .containsExactly("noticeVersionId:CONSENT_NOTICE_NOT_ACTIVE");
        assertThat(evidence(tenant)).isEmpty();
    }

    @Test
    void aPaperFormCountsOnlyOnceAnotherStaffMemberVerifiesIt() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID parent = UUID.randomUUID();
        fixtures.minor(tenant, child);
        fixtures.guardian(tenant, parent, child);
        String version = fixtures.enable(tenant, PHOTOS);
        String recorder = calls.administrator(tenant, STAFF);
        String verifier = calls.administrator(tenant, STAFF);

        assertThat(calls.post(CONSENT_RECORDS, calls.schoolAdministrator(tenant, STAFF),
                paperForm(child, parent, "guardian", PHOTOS, "public", "granted", version)).status())
                .as("consent data is tenant-level: a school's grant cannot write it").isEqualTo(403);
        assertThat(calls.post(CONSENT_RECORDS, recorder,
                paperForm(child, UUID.randomUUID(), "guardian", PHOTOS, "public", "granted", version)).status())
                .as("the form's giver must be an active legal guardian").isEqualTo(404);

        PrivacyCalls.Reply recorded = calls.post(CONSENT_RECORDS, recorder,
                paperForm(child, parent, "guardian", PHOTOS, "public", "granted", version));
        assertThat(recorded.status()).as(recorded.body().toString()).isEqualTo(201);
        assertThat(recorded.text("channel")).isEqualTo("paper_form");
        assertThat(recorded.text("verificationStatus")).isEqualTo("unverified");
        assertThat(recorded.text("recordedByUserId")).isNotEmpty();
        assertThat(state(tenant, child, PHOTOS)).containsOnly("class:NOT_ANSWERED", "institution:NOT_ANSWERED",
                "inter_school:NOT_ANSWERED", "public:NOT_ANSWERED");

        String verify = CONSENT_RECORDS + "/" + recorded.text("id") + ":verify";
        PrivacyCalls.Reply sameStaff = calls.post(verify, recorder, Map.of());
        assertThat(sameStaff.status()).isEqualTo(409);
        assertThat(sameStaff.text("code")).isEqualTo("CONSENT_FOUR_EYES");

        PrivacyCalls.Reply verified = calls.post(verify, verifier, Map.of("etag", recorded.text("etag")));
        assertThat(verified.status()).as(verified.body().toString()).isEqualTo(200);
        assertThat(verified.text("verificationStatus")).isEqualTo("verified");
        assertThat(verified.text("verificationMethod")).isEqualTo("paper_form_countersigned");
        assertThat(verified.text("verifiedByUserId")).isNotEqualTo(recorded.text("recordedByUserId"));
        assertThat(state(tenant, child, PHOTOS)).containsOnly("class:CONSENT", "institution:CONSENT",
                "inter_school:CONSENT", "public:CONSENT");
        assertThat(calls.post(verify, verifier, Map.of()).status()).as("verified once").isEqualTo(409);

        PrivacyCalls.Reply centre = calls.get(MY_CONSENTS, calls.person(tenant, parent, SELF_SERVICE));
        assertThat(centre.body().path("data")).anySatisfy(row -> {
            assertThat(row.path("id").asString()).isEqualTo(recorded.text("id"));
            assertThat(row.path("channel").asString()).isEqualTo("paper_form");
            assertThat(row.path("effectiveWidestScope").asString()).isEqualTo("public");
        });
        assertThat(evidence(tenant)).extracting(s -> s.entry().event()).containsExactly("recorded", "verified");
    }

    @Test
    void anotherTenantSeesNothing() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String version = fixtures.enable(tenant, PHOTOS);
        PrivacyCalls.Reply granted = calls.post(MY_CONSENTS, calls.person(tenant, self, SELF_SERVICE),
                decision(self, PHOTOS, "class", "granted", version, "en"));
        assertThat(granted.status()).as(granted.body().toString()).isEqualTo(201);

        String outsider = calls.administrator(other, STAFF);
        String record = CONSENT_RECORDS + "/" + granted.text("id");
        assertThat(calls.get(record, outsider).status()).isEqualTo(404);
        assertThat(calls.post(record + ":verify", outsider, Map.of()).status()).isEqualTo(404);
        assertThat(calls.get(CONSENT_RECORDS, outsider).body().path("data")).isEmpty();
        assertThat(calls.post(MY_CONSENTS + "/" + granted.text("id") + ":withdraw",
                calls.person(other, self, SELF_SERVICE), Map.of()).status()).isEqualTo(404);
        assertThat(calls.get(CONSENT_RECORDS, calls.administrator(tenant, STAFF)).body().path("data")).hasSize(1);
    }

    @Test
    void theCentreShowsTheActiveNoticeInTheLanguageAskedForElseInEnglish() throws Exception {
        UUID tenant = UUID.randomUUID();
        String version = fixtures.enable(tenant, PHOTOS);
        String token = calls.person(tenant, UUID.randomUUID(), SELF_SERVICE);
        String notice = "/v1/privacy/my/notices/" + NOTICE + "?language=";

        PrivacyCalls.Reply hindi = calls.get(notice + "hi", token);
        assertThat(hindi.status()).as(hindi.body().toString()).isEqualTo(200);
        assertThat(hindi.text("language")).isEqualTo("hi");
        assertThat(hindi.text("bodyMarkdown")).isEqualTo(HINDI);
        assertThat(hindi.text("noticeVersionId")).isEqualTo(version);
        assertThat(hindi.body().path("purposeCodes").get(0).asString()).isEqualTo(PHOTOS);

        PrivacyCalls.Reply fallback = calls.get(notice + "fr", token);
        assertThat(fallback.status()).isEqualTo(200);
        assertThat(fallback.text("language")).isEqualTo("en");
        assertThat(fallback.text("bodyMarkdown")).isEqualTo(ENGLISH);
        assertThat(fallback.text("contentSha256")).matches("[0-9a-f]{64}");

        assertThat(calls.get("/v1/privacy/my/notices/no_such_notice?language=en", token).status()).isEqualTo(404);
        assertThat(calls.get(notice + "en", calls.person(UUID.randomUUID(), UUID.randomUUID(), SELF_SERVICE))
                .status()).as("another tenant's notice").isEqualTo(404);
    }
}
