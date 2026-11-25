package com.mysms.privacy;

import static com.mysms.privacy.ConsentFixtures.MY_REQUESTS;
import static com.mysms.privacy.ConsentFixtures.REQUESTER;
import static com.mysms.privacy.ConsentFixtures.RIGHTS_REQUESTS;
import static com.mysms.privacy.ConsentFixtures.RIGHTS_STAFF;
import static com.mysms.privacy.ConsentFixtures.rightsRequest;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.events.GuardianLinkSync;
import com.mysms.privacy.events.StaffSync;
import com.mysms.privacy.events.SubjectSync;
import java.util.HashMap;
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
 * The fulfilment of rights requests against real PostgreSQL through the real edge: no staff member acts on a request
 * they are the subject or the requester of, nor is assigned one; corrections and grievances carry their typed
 * details and are answered; and the nominee of an adult (DPDP Act s.14) — named, masked, replaced and revoked by the
 * principal — acts for them only once their nomination request is fulfilled. No name, contact, text or answer reaches
 * an event.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RightsRequestFulfilmentComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String MY_NOMINEE = "/v1/privacy/my/nominee";
    private static final String[] NOMINATOR = {"privacy.my_nominee.read", "privacy.my_nominee.set",
        "privacy.my_nominee.revoke"};
    private static final String NOMINEE_NAME = "Meera Krishnan";
    private static final String NOMINEE_PHONE = "+91 98450 12345";

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
    private StaffSync staffSync;

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

    /** Every payload the tenant's outbox holds, audit records included. */
    private List<String> outbox(UUID tenant) {
        return fixtures.read(tenant, () -> database.jdbc().sql(
                "SELECT o.event_type || ' ' || o.payload FROM privacy.outbox_event o WHERE o.tenant_id = :tenantId"
                        + " ORDER BY o.id").query(String.class).list());
    }

    private String submit(String token, Map<String, Object> body) throws Exception {
        PrivacyCalls.Reply submitted = calls.post(MY_REQUESTS, token, body);
        assertThat(submitted.status()).as(submitted.body().toString()).isEqualTo(201);
        return submitted.text("id");
    }

    private PrivacyCalls.Reply assign(String staff, String id) throws Exception {
        PrivacyCalls.Reply assigned = calls.post(RIGHTS_REQUESTS + "/" + id + ":assign", staff,
                Map.of("assigneeUserId", UUID.randomUUID().toString()));
        assertThat(assigned.status()).as(assigned.body().toString()).isEqualTo(200);
        return assigned;
    }

    @Test
    void noStaffMemberActsOnARequestTheyAreTheSubjectOrRequesterOf() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID officer = UUID.randomUUID();
        UUID officerAccount = UUID.randomUUID();
        fixtures.adult(tenant, officer);
        fixtures.staff(tenant, staffSync, officer, officerAccount);
        String own = submit(calls.person(tenant, officer, REQUESTER), rightsRequest(officer, "access", "Mine."));
        String asStaff = calls.administratorAs(tenant, officer, RIGHTS_STAFF);

        PrivacyCalls.Reply self = calls.post(RIGHTS_REQUESTS + "/" + own + ":assign", asStaff,
                Map.of("assigneeUserId", UUID.randomUUID().toString()));
        assertThat(self.status()).isEqualTo(409);
        assertThat(self.text("code")).isEqualTo("RIGHTS_REQUEST_CONFLICT_OF_INTEREST");
        assertThat(calls.post(RIGHTS_REQUESTS + "/" + own + ":reject", asStaff, Map.of("reasonCode", "other"))
                .text("code")).isEqualTo("RIGHTS_REQUEST_CONFLICT_OF_INTEREST");

        String colleague = calls.administrator(tenant, RIGHTS_STAFF);
        PrivacyCalls.Reply toThemself = calls.post(RIGHTS_REQUESTS + "/" + own + ":assign", colleague,
                Map.of("assigneeUserId", officerAccount.toString()));
        assertThat(toThemself.status()).as("never assigned through their own staff account").isEqualTo(409);
        assertThat(toThemself.text("code")).isEqualTo("RIGHTS_REQUEST_CONFLICT_OF_INTEREST");
        assertThat(assign(colleague, own).text("status")).isEqualTo("in_progress");

        String throughTheAccount = calls.administratorAccount(tenant, officerAccount, RIGHTS_STAFF);
        PrivacyCalls.Reply noPid = calls.post(RIGHTS_REQUESTS + "/" + own + ":reject", throughTheAccount,
                Map.of("reasonCode", "other"));
        assertThat(noPid.status()).as("a token without a person is recognised by its staff account").isEqualTo(409);
        assertThat(noPid.text("code")).isEqualTo("RIGHTS_REQUEST_CONFLICT_OF_INTEREST");

        UUID someoneElse = UUID.randomUUID();
        fixtures.adult(tenant, someoneElse);
        String theirs = submit(calls.person(tenant, someoneElse, REQUESTER),
                rightsRequest(someoneElse, "access", "Theirs."));
        assertThat(assign(asStaff, theirs).text("status")).as("anyone else's request is theirs to handle")
                .isEqualTo("in_progress");
    }

    @Test
    void aCorrectionNamesItsRecordAndADeclineIsExplained() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String token = calls.person(tenant, self, REQUESTER);

        Map<String, Object> bare = new HashMap<>(rightsRequest(self, "correction", "Fix my surname."));
        bare.remove("correction");
        assertThat(PrivacyCalls.fieldErrors(calls.post(MY_REQUESTS, token, bare)))
                .containsExactly("correction:RIGHTS_REQUEST_DETAILS_REQUIRED");
        Map<String, Object> misplaced = new HashMap<>(rightsRequest(self, "access", "What you hold."));
        misplaced.put("grievanceCategory", "consent");
        assertThat(PrivacyCalls.fieldErrors(calls.post(MY_REQUESTS, token, misplaced)))
                .containsExactly("grievanceCategory:RIGHTS_REQUEST_DETAILS_NOT_ALLOWED");

        String id = submit(token, rightsRequest(self, "correction", "Fix my surname."));
        String staff = calls.administrator(tenant, RIGHTS_STAFF);
        PrivacyCalls.Reply read = calls.get(RIGHTS_REQUESTS + "/" + id, staff);
        assertThat(read.body().path("correction").path("module").asString()).isEqualTo("people");
        assertThat(read.body().path("correction").path("fields").path(0).path("requestedValue").asString())
                .isEqualTo("Sharma");
        PrivacyCalls.Reply assigned = assign(staff, id);

        PrivacyCalls.Reply unexplained = calls.post(RIGHTS_REQUESTS + "/" + id + ":close", staff,
                Map.of("outcomeCode", "declined", "etag", assigned.etag()));
        assertThat(PrivacyCalls.fieldErrors(unexplained)).containsExactly("response:RIGHTS_REQUEST_RESPONSE_REQUIRED");
        PrivacyCalls.Reply declined = calls.post(RIGHTS_REQUESTS + "/" + id + ":close", staff, Map.of(
                "outcomeCode", "declined", "response", "The admission register shows Sarma.", "etag", assigned.etag()));
        assertThat(declined.status()).as(declined.body().toString()).isEqualTo(200);

        PrivacyCalls.Reply mine = calls.get(MY_REQUESTS + "/" + id, token);
        assertThat(mine.text("response")).isEqualTo("The admission register shows Sarma.");
        assertThat(mine.body().path("correction").path("recordType").asString()).isEqualTo("person");
        assertThat(outbox(tenant)).noneMatch(event -> event.contains("Sharma") || event.contains("Sarma")
                || event.contains("surname"));
    }

    @Test
    void aNomineeActsForThePrincipalOnlyOnceTheirNominationIsFulfilled() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID principal = UUID.randomUUID();
        UUID nominee = UUID.randomUUID();
        fixtures.adult(tenant, principal);
        fixtures.adult(tenant, nominee);
        String principalToken = calls.person(tenant, principal, NOMINATOR);
        assertThat(calls.get(MY_NOMINEE, principalToken).status()).as("none named yet").isEqualTo(404);

        PrivacyCalls.Reply named = calls.post(MY_NOMINEE + ":set", principalToken, Map.of("nomineeName",
                NOMINEE_NAME, "nomineeContact", NOMINEE_PHONE, "nomineePersonId", nominee.toString()));
        assertThat(named.status()).as(named.body().toString()).isEqualTo(200);
        assertThat(named.text("nomineeNameMasked")).isEqualTo("••••hnan");
        assertThat(named.text("nomineeContactMasked")).isEqualTo("••••2345");
        assertThat(named.body().has("activatedAt")).isFalse();
        assertThat(named.body().toString()).doesNotContain(NOMINEE_NAME).doesNotContain("98450");

        String nomineeToken = calls.person(tenant, nominee, REQUESTER);
        PrivacyCalls.Reply early = calls.post(MY_REQUESTS, nomineeToken, rightsRequest(principal, "access", "Hers."));
        assertThat(PrivacyCalls.fieldErrors(early)).containsExactly("type:RIGHTS_REQUEST_NOMINEE_NOT_ACTIVE");
        assertThat(PrivacyCalls.fieldErrors(calls.post(MY_REQUESTS, calls.person(tenant, principal, REQUESTER),
                rightsRequest(principal, "nomination", "Me.")))).containsExactly("type:RIGHTS_REQUEST_TYPE_NOT_ALLOWED");
        UUID stranger = UUID.randomUUID();
        fixtures.adult(tenant, stranger);
        assertThat(calls.post(MY_REQUESTS, calls.person(tenant, stranger, REQUESTER),
                rightsRequest(principal, "nomination", "Her.")).status()).isEqualTo(404);

        PrivacyCalls.Reply nomination = calls.post(MY_REQUESTS, nomineeToken,
                rightsRequest(principal, "nomination", "She is in hospital and cannot act."));
        assertThat(nomination.status()).as(nomination.body().toString()).isEqualTo(201);
        assertThat(nomination.text("requesterCapacity")).isEqualTo("nominee");
        assertThat(nomination.text("status")).as("staff verify a nominee in person or by document")
                .isEqualTo("submitted");

        String staff = calls.administrator(tenant, RIGHTS_STAFF);
        String request = RIGHTS_REQUESTS + "/" + nomination.text("id");
        PrivacyCalls.Reply verified = calls.post(request + ":verifyIdentity", staff,
                Map.of("identityCheck", "document"));
        assertThat(verified.status()).as(verified.body().toString()).isEqualTo(200);
        PrivacyCalls.Reply assigned = assign(staff, nomination.text("id"));
        assertThat(calls.post(request + ":close", staff, Map.of("outcomeCode", "partially_fulfilled",
                "etag", assigned.etag())).status()).as("fulfilled or declined, never partly").isEqualTo(409);
        PrivacyCalls.Reply fulfilled = calls.post(request + ":close", staff,
                Map.of("outcomeCode", "fulfilled", "etag", assigned.etag()));
        assertThat(fulfilled.status()).as(fulfilled.body().toString()).isEqualTo(200);
        assertThat(calls.get(MY_NOMINEE, principalToken).body().has("activatedAt")).isTrue();

        PrivacyCalls.Reply acting = calls.post(MY_REQUESTS, nomineeToken,
                rightsRequest(principal, "access", "What the school holds about her."));
        assertThat(acting.status()).as(acting.body().toString()).isEqualTo(201);
        assertThat(acting.text("status")).isEqualTo("submitted");
        assertThat(calls.get(MY_REQUESTS, nomineeToken).body().path("data")).hasSize(2);

        PrivacyCalls.Reply current = calls.get(MY_NOMINEE, principalToken);
        assertThat(calls.post(MY_NOMINEE + ":revoke", principalToken, Map.of("etag", "\"99\"")).status())
                .isEqualTo(412);
        assertThat(calls.post(MY_NOMINEE + ":revoke", principalToken, Map.of("etag", current.text("etag"))).status())
                .isEqualTo(204);
        assertThat(calls.get(MY_NOMINEE, principalToken).status()).isEqualTo(404);
        assertThat(calls.get(MY_REQUESTS, nomineeToken).body().path("data"))
                .as("a revoked nominee no longer sees the principal's requests").isEmpty();
        assertThat(outbox(tenant)).noneMatch(event -> event.contains("Krishnan") || event.contains("98450")
                || event.contains("hospital"));
    }

    @Test
    void onlyAnAdultNamesANomineeAndNeverWhileImpersonated() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID adult = UUID.randomUUID();
        fixtures.minor(tenant, child);
        fixtures.adult(tenant, adult);
        Map<String, Object> body = Map.of("nomineeName", NOMINEE_NAME, "nomineeContact", NOMINEE_PHONE);

        assertThat(calls.post(MY_NOMINEE + ":set", calls.person(tenant, child, NOMINATOR), body).status())
                .isEqualTo(403);
        assertThat(calls.post(MY_NOMINEE + ":set", calls.impersonated(tenant, adult, UUID.randomUUID(), NOMINATOR),
                body).status()).isEqualTo(403);
        assertThat(calls.post(MY_NOMINEE + ":set", calls.person(tenant, adult, NOMINATOR),
                Map.of("nomineeName", NOMINEE_NAME, "nomineeContact", NOMINEE_PHONE,
                        "nomineePersonId", adult.toString())).status()).as("never oneself").isEqualTo(403);
        assertThat(calls.post(MY_NOMINEE + ":set", calls.person(tenant, adult, NOMINATOR),
                Map.of("nomineeName", NOMINEE_NAME, "nomineeContact", NOMINEE_PHONE,
                        "nomineePersonId", child.toString())).status()).as("never a minor").isEqualTo(403);
        assertThat(calls.post(MY_NOMINEE + ":set", calls.person(tenant, adult, NOMINATOR),
                Map.of("nomineeName", NOMINEE_NAME, "nomineeContact", NOMINEE_PHONE,
                        "nomineePersonId", UUID.randomUUID().toString())).status()).as("an unknown person")
                .isEqualTo(404);

        PrivacyCalls.Reply first = calls.post(MY_NOMINEE + ":set", calls.person(tenant, adult, NOMINATOR), body);
        PrivacyCalls.Reply replaced = calls.post(MY_NOMINEE + ":set", calls.person(tenant, adult, NOMINATOR),
                Map.of("nomineeName", "Arjun Rao", "nomineeContact", "arjun@example.org", "etag", first.text("etag")));
        assertThat(replaced.status()).as(replaced.body().toString()).isEqualTo(200);
        assertThat(replaced.text("id")).isNotEqualTo(first.text("id"));
        assertThat(replaced.text("nomineeContactMasked")).isEqualTo("••••.org");
        assertThat(fixtures.read(tenant, () -> database.jdbc().sql(
                "SELECT count(*) FROM privacy.nominee WHERE tenant_id = :tenantId AND status = 'revoked'")
                .query(Integer.class).single())).as("the replaced nomination stays as evidence").isEqualTo(1);
    }
}
