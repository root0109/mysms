package com.mysms.privacy;

import static com.mysms.privacy.ConsentFixtures.MY_REQUESTS;
import static com.mysms.privacy.ConsentFixtures.REQUESTER;
import static com.mysms.privacy.ConsentFixtures.RIGHTS_REQUESTS;
import static com.mysms.privacy.ConsentFixtures.RIGHTS_STAFF;
import static com.mysms.privacy.ConsentFixtures.rightsRequest;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.events.GuardianLinkSync;
import com.mysms.privacy.events.SubjectSync;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
 * Rights requests against real PostgreSQL through the real edge: who may submit for whom (404 for anyone else, 403
 * while impersonating), the number, verification and deadline a submission gets, the requester's own view, the staff's
 * steps with their etag, state and tenant-wide checks (an access request closed only as declined), tenant isolation,
 * and the events written without what the requester wrote.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RightsRequestComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String WHAT_WE_HOLD = "Please tell me what the school holds about me and why.";
    private static final String NUMBER = "PRV-\\d{4}-\\d{6}";

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

    /** The type and payload of each rights-request event of the tenant, in the order written. */
    private List<String> events(UUID tenant) {
        return fixtures.read(tenant, () -> database.jdbc().sql("""
                SELECT o.event_type || ' ' || o.payload
                  FROM privacy.outbox_event o
                 WHERE o.tenant_id = :tenantId AND o.event_type LIKE 'privacy.rights_request.%'
                 ORDER BY o.id
                """).query(String.class).list());
    }

    /** Every payload the tenant's outbox holds, audit records included. */
    private List<String> outbox(UUID tenant) {
        return fixtures.read(tenant, () -> database.jdbc().sql(
                "SELECT o.payload FROM privacy.outbox_event o WHERE o.tenant_id = :tenantId ORDER BY o.id")
                .query(String.class).list());
    }

    /** Runs a statement in a write transaction of the tenant (row-level security applies). */
    private void write(UUID tenant, String sql, UUID id) {
        int changed = CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).write(() -> database.jdbc().sql(sql).param("id", id).update()));
        assertThat(changed).isEqualTo(1);
    }

    private static OffsetDateTime time(JsonNode body, String field) {
        return OffsetDateTime.parse(body.path(field).asString());
    }

    @Test
    void anAdultSubmitsForThemselfVerifiedNumberedAndDueAfterThePeriod() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String token = calls.person(tenant, self, REQUESTER);

        PrivacyCalls.Reply submitted = calls.post(MY_REQUESTS, token, rightsRequest(self, "access", WHAT_WE_HOLD));
        assertThat(submitted.status()).as(submitted.body().toString()).isEqualTo(201);
        JsonNode mine = submitted.body();
        int year = OffsetDateTime.now(ZoneOffset.UTC).getYear();
        assertThat(mine.path("number").asString()).matches(NUMBER).startsWith("PRV-" + year + "-");
        assertThat(mine.path("type").asString()).isEqualTo("access");
        assertThat(mine.path("requesterCapacity").asString()).isEqualTo("self");
        assertThat(mine.path("status").asString()).as("a signed-in session verifies the requester")
                .isEqualTo("verified");
        assertThat(mine.path("description").asString()).isEqualTo(WHAT_WE_HOLD);
        assertThat(mine.path("overdue").asBoolean()).isFalse();
        assertThat(time(mine, "dueAt")).isAtSameInstantAs(time(mine, "submittedAt").plusDays(30));
        assertThat(mine.has("assigneeUserId")).isFalse();
        assertThat(mine.has("identityCheck")).isFalse();
        assertThat(mine.has("requesterPersonId")).isFalse();

        PrivacyCalls.Reply next = calls.post(MY_REQUESTS, token, rightsRequest(self, "grievance", "A complaint."));
        assertThat(next.status()).as(next.body().toString()).isEqualTo(201);
        long first = Long.parseLong(mine.path("number").asString().substring(9));
        assertThat(Long.parseLong(next.text("number").substring(9))).as("the tenant's series of the year")
                .isEqualTo(first + 1);

        PrivacyCalls.Reply staff = calls.get(RIGHTS_REQUESTS + "/" + mine.path("id").asString(),
                calls.administrator(tenant, RIGHTS_STAFF));
        assertThat(staff.status()).isEqualTo(200);
        assertThat(staff.text("identityCheck")).isEqualTo("signed_in_session");
        assertThat(staff.text("requesterPersonId")).isEqualTo(self.toString());
        assertThat(staff.etag()).isEqualTo("\"1\"");

        assertThat(events(tenant)).hasSize(2).allSatisfy(event -> assertThat(event)
                .startsWith("privacy.rights_request.submitted ").contains("\"status\":\"verified\""));
        assertThat(outbox(tenant)).as("what the requester wrote never leaves privacy")
                .noneMatch(payload -> payload.contains(WHAT_WE_HOLD));
    }

    @Test
    void onlyTheAdultOrAnActiveLegalGuardianMaySubmit() throws Exception {
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

        PrivacyCalls.Reply asGuardian = calls.post(MY_REQUESTS, calls.person(tenant, parent, REQUESTER),
                rightsRequest(child, "correction", "My son's surname is misspelt."));
        assertThat(asGuardian.status()).as(asGuardian.body().toString()).isEqualTo(201);
        assertThat(asGuardian.text("requesterCapacity")).isEqualTo("guardian");
        assertThat(asGuardian.text("subjectPersonId")).isEqualTo(child.toString());

        PrivacyCalls.Reply notGuardian = calls.post(MY_REQUESTS, calls.person(tenant, stranger, REQUESTER),
                rightsRequest(child, "access", WHAT_WE_HOLD));
        assertThat(notGuardian.status()).isEqualTo(404);
        assertThat(notGuardian.text("code")).isEqualTo("NOT_FOUND");
        assertThat(calls.post(MY_REQUESTS, calls.person(tenant, child, REQUESTER),
                rightsRequest(child, "access", WHAT_WE_HOLD)).status())
                .as("a minor's rights are exercised by their legal guardian, as for consent").isEqualTo(404);
        assertThat(calls.post(MY_REQUESTS, calls.person(tenant, parent, REQUESTER),
                rightsRequest(grownUp, "access", WHAT_WE_HOLD)).status())
                .as("a guardian does not ask for an adult").isEqualTo(404);
        assertThat(calls.post(MY_REQUESTS, calls.person(tenant, parent, REQUESTER),
                rightsRequest(UUID.randomUUID(), "access", WHAT_WE_HOLD)).status())
                .as("an unknown person looks like any other").isEqualTo(404);

        PrivacyCalls.Reply impersonated = calls.post(MY_REQUESTS,
                calls.impersonated(tenant, parent, UUID.randomUUID(), REQUESTER),
                rightsRequest(child, "access", WHAT_WE_HOLD));
        assertThat(impersonated.status()).isEqualTo(403);
        assertThat(impersonated.text("code")).isEqualTo("PERMISSION_DENIED");
        assertThat(calls.post(MY_REQUESTS, calls.administrator(tenant, REQUESTER),
                rightsRequest(child, "access", WHAT_WE_HOLD)).status())
                .as("an account linked to no person asks for nobody").isEqualTo(403);

        String token = calls.person(tenant, stranger, REQUESTER);
        assertThat(calls.post(MY_REQUESTS, token, rightsRequest(stranger, "deletion", WHAT_WE_HOLD)).status())
                .as("a type the contract does not know").isEqualTo(400);
        assertThat(PrivacyCalls.fieldErrors(calls.post(MY_REQUESTS, token, rightsRequest(stranger, "access", " "))))
                .anySatisfy(error -> assertThat(error).startsWith("description:"));
        assertThat(events(tenant)).hasSize(1);
    }

    @Test
    void aRequesterSeesOnlyTheirOwnRequestsNewestFirst() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        fixtures.adult(tenant, self);
        fixtures.adult(tenant, other);
        String token = calls.person(tenant, self, REQUESTER);
        String older = calls.post(MY_REQUESTS, token, rightsRequest(self, "access", WHAT_WE_HOLD)).text("id");
        String newer = calls.post(MY_REQUESTS, token, rightsRequest(self, "grievance", "A complaint.")).text("id");
        String theirs = calls.post(MY_REQUESTS, calls.person(tenant, other, REQUESTER),
                rightsRequest(other, "access", WHAT_WE_HOLD)).text("id");

        PrivacyCalls.Reply list = calls.get(MY_REQUESTS, token);
        assertThat(list.status()).as(list.body().toString()).isEqualTo(200);
        assertThat(list.body().path("data").findValuesAsString("id")).containsExactly(newer, older);
        assertThat(calls.get(MY_REQUESTS + "?size=1", token).body().path("data").findValuesAsString("id"))
                .containsExactly(newer);
        assertThat(calls.get(MY_REQUESTS + "/" + older, token).text("type")).isEqualTo("access");
        assertThat(calls.get(MY_REQUESTS + "/" + theirs, token).status()).as("another person's request")
                .isEqualTo(404);
        assertThat(calls.get(MY_REQUESTS + "/" + UUID.randomUUID(), token).status()).isEqualTo(404);
    }

    @Test
    void aGuardianWhoseLinkEndedNoLongerSeesTheWardsRequests() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID parent = UUID.randomUUID();
        fixtures.minor(tenant, child);
        fixtures.adult(tenant, parent);
        UUID link = fixtures.guardian(tenant, parent, child);
        String token = calls.person(tenant, parent, REQUESTER);
        String forChild = calls.post(MY_REQUESTS, token, rightsRequest(child, "access", WHAT_WE_HOLD)).text("id");
        String forSelf = calls.post(MY_REQUESTS, token, rightsRequest(parent, "access", WHAT_WE_HOLD)).text("id");

        fixtures.unlink(tenant, link, parent, child);
        assertThat(calls.get(MY_REQUESTS, token).body().path("data").findValuesAsString("id"))
                .as("only what they still decide for").containsExactly(forSelf);
        assertThat(calls.get(MY_REQUESTS + "/" + forChild, token).status()).isEqualTo(404);
        assertThat(calls.get(RIGHTS_REQUESTS + "/" + forChild, calls.administrator(tenant, RIGHTS_STAFF)).status())
                .as("the staff still handle it").isEqualTo(200);
    }

    @Test
    void theStaffAssignExtendAndCloseARequest() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String token = calls.person(tenant, self, REQUESTER);
        calls.post(MY_REQUESTS, token, rightsRequest(self, "access", WHAT_WE_HOLD));
        String id = calls.post(MY_REQUESTS, token, rightsRequest(self, "grievance", "A complaint.")).text("id");
        String staff = calls.administrator(tenant, RIGHTS_STAFF);
        String request = RIGHTS_REQUESTS + "/" + id;
        UUID officer = UUID.randomUUID();

        PrivacyCalls.Reply grievances = calls.get(RIGHTS_REQUESTS + "?filter=type%20%3D%20%22grievance%22", staff);
        assertThat(grievances.status()).as(grievances.body().toString()).isEqualTo(200);
        assertThat(grievances.body().path("data").findValuesAsString("type")).containsExactly("grievance");
        assertThat(calls.get(RIGHTS_REQUESTS + "?sort=number,desc", staff).body().path("data")).hasSize(2);

        assertThat(calls.post(request + ":assign", calls.schoolAdministrator(tenant, RIGHTS_STAFF),
                Map.of("assigneeUserId", officer.toString())).status())
                .as("rights requests are tenant-level: a school's grant cannot step them").isEqualTo(403);
        PrivacyCalls.Reply assigned = calls.post(request + ":assign", staff,
                Map.of("assigneeUserId", officer.toString(), "etag", "\"1\""));
        assertThat(assigned.status()).as(assigned.body().toString()).isEqualTo(200);
        assertThat(assigned.text("status")).isEqualTo("in_progress");
        assertThat(assigned.text("assigneeUserId")).isEqualTo(officer.toString());
        assertThat(assigned.etag()).isEqualTo("\"2\"");

        OffsetDateTime dueAt = time(assigned.body(), "dueAt");
        PrivacyCalls.Reply stale = calls.post(request + ":extend", staff, Map.of("dueAt",
                dueAt.plusDays(30).toString(), "reason", "Records are archived off site.", "etag", "\"1\""));
        assertThat(stale.status()).isEqualTo(412);
        PrivacyCalls.Reply earlier = calls.post(request + ":extend", staff, Map.of("dueAt",
                dueAt.minusDays(1).toString(), "reason", "Records are archived off site."));
        assertThat(PrivacyCalls.fieldErrors(earlier)).containsExactly("dueAt:RIGHTS_REQUEST_DUE_AT_NOT_LATER");
        PrivacyCalls.Reply extended = calls.post(request + ":extend", staff, Map.of("dueAt",
                dueAt.plusDays(30).toString(), "reason", "Records are archived off site.", "etag", assigned.etag()));
        assertThat(extended.status()).as(extended.body().toString()).isEqualTo(200);
        assertThat(extended.text("status")).as("an extension is not a state").isEqualTo("in_progress");
        assertThat(time(extended.body(), "dueAt")).isAtSameInstantAs(dueAt.plusDays(30));
        assertThat(extended.text("extendedReason")).isEqualTo("Records are archived off site.");

        PrivacyCalls.Reply notRejectable = calls.post(request + ":reject", staff, Map.of("reasonCode", "other"));
        assertThat(notRejectable.status()).isEqualTo(409);
        assertThat(notRejectable.text("code")).isEqualTo("INVALID_STATE_TRANSITION");
        PrivacyCalls.Reply unanswered = calls.post(request + ":close", staff, Map.of("outcomeCode", "fulfilled",
                "note", "Summary sent by letter.", "etag", extended.etag()));
        assertThat(PrivacyCalls.fieldErrors(unanswered)).as("a grievance is always answered")
                .containsExactly("response:RIGHTS_REQUEST_RESPONSE_REQUIRED");
        PrivacyCalls.Reply closed = calls.post(request + ":close", staff, Map.of("outcomeCode", "fulfilled",
                "note", "Summary sent by letter.", "response", "We have corrected our process.",
                "etag", extended.etag()));
        assertThat(closed.status()).as(closed.body().toString()).isEqualTo(200);
        assertThat(closed.text("status")).isEqualTo("closed");
        assertThat(closed.text("outcomeCode")).isEqualTo("fulfilled");
        assertThat(closed.text("outcomeNote")).isEqualTo("Summary sent by letter.");
        assertThat(closed.text("type")).isEqualTo("grievance");
        assertThat(closed.body().has("closedAt")).isTrue();
        assertThat(calls.post(request + ":close", staff, Map.of("outcomeCode", "declined")).status())
                .as("closed once").isEqualTo(409);
        assertThat(calls.post(request + ":extend", staff, Map.of("dueAt", dueAt.plusDays(90).toString(),
                "reason", "Late.")).status()).as("a closed request has no deadline left").isEqualTo(409);

        PrivacyCalls.Reply mine = calls.get(MY_REQUESTS + "/" + id, token);
        assertThat(mine.text("status")).isEqualTo("closed");
        assertThat(mine.text("outcomeCode")).isEqualTo("fulfilled");
        assertThat(mine.body().has("outcomeNote")).as("the staff's note stays with the staff").isFalse();
        assertThat(mine.text("response")).as("the answer is the requester's").isEqualTo("We have corrected our process.");
        assertThat(mine.text("grievanceCategory")).isEqualTo("other");
        assertThat(mine.body().has("extendedReason")).isFalse();

        List<String> events = events(tenant);
        assertThat(events).extracting(event -> event.substring(0, event.indexOf(' '))).containsExactly(
                "privacy.rights_request.submitted", "privacy.rights_request.submitted",
                "privacy.rights_request.assigned", "privacy.rights_request.extended", "privacy.rights_request.closed");
        assertThat(events).noneMatch(event -> event.contains("archived") || event.contains("letter")
                || event.contains(WHAT_WE_HOLD) || event.contains("corrected our process"));
    }

    @Test
    void anAccessRequestIsNeverClosedAsFulfilledWithoutItsSummary() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String id = calls.post(MY_REQUESTS, calls.person(tenant, self, REQUESTER),
                rightsRequest(self, "access", WHAT_WE_HOLD)).text("id");
        String staff = calls.administrator(tenant, RIGHTS_STAFF);
        String request = RIGHTS_REQUESTS + "/" + id;
        PrivacyCalls.Reply assigned = calls.post(request + ":assign", staff,
                Map.of("assigneeUserId", UUID.randomUUID().toString()));
        assertThat(assigned.status()).as(assigned.body().toString()).isEqualTo(200);

        for (String outcome : List.of("fulfilled", "partially_fulfilled")) {
            PrivacyCalls.Reply refused = calls.post(request + ":close", staff,
                    Map.of("outcomeCode", outcome, "etag", assigned.etag()));
            assertThat(refused.status()).as(outcome + ": fulfilled with its summary only").isEqualTo(409);
            assertThat(refused.text("code")).isEqualTo("INVALID_STATE_TRANSITION");
        }
        PrivacyCalls.Reply unchanged = calls.get(request, staff);
        assertThat(unchanged.text("status")).isEqualTo("in_progress");
        assertThat(unchanged.body().has("outcomeCode")).isFalse();
        assertThat(unchanged.body().has("closedAt")).isFalse();
        assertThat(unchanged.etag()).isEqualTo(assigned.etag());

        PrivacyCalls.Reply declined = calls.post(request + ":close", staff,
                Map.of("outcomeCode", "declined", "etag", assigned.etag()));
        assertThat(declined.status()).as(declined.body().toString()).isEqualTo(200);
        assertThat(declined.text("status")).isEqualTo("closed");
        assertThat(declined.text("outcomeCode")).isEqualTo("declined");
        assertThat(events(tenant)).extracting(event -> event.substring(0, event.indexOf(' '))).containsExactly(
                "privacy.rights_request.submitted", "privacy.rights_request.assigned",
                "privacy.rights_request.closed");
    }

    @Test
    void theStaffVerifyRejectAndSeeWhatIsOverdue() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String token = calls.person(tenant, self, REQUESTER);
        String staff = calls.administrator(tenant, RIGHTS_STAFF);
        UUID id = UUID.fromString(calls.post(MY_REQUESTS, token, rightsRequest(self, "access", WHAT_WE_HOLD))
                .text("id"));
        String request = RIGHTS_REQUESTS + "/" + id;

        assertThat(calls.post(request + ":verifyIdentity", staff, Map.of("identityCheck", "in_person")).status())
                .as("a signed-in request is verified already").isEqualTo(409);
        // a request awaiting verification in person, as a nominee's will (no operation submits one yet)
        write(tenant, "UPDATE privacy.rights_request SET status = 'submitted', identity_check = NULL"
                + " WHERE tenant_id = :tenantId AND id = :id", id);
        assertThat(calls.post(request + ":assign", staff, Map.of("assigneeUserId", UUID.randomUUID().toString()))
                .status()).as("not before the requester is verified").isEqualTo(409);
        PrivacyCalls.Reply verified = calls.post(request + ":verifyIdentity", staff,
                Map.of("identityCheck", "in_person", "etag", "\"1\""));
        assertThat(verified.status()).as(verified.body().toString()).isEqualTo(200);
        assertThat(verified.text("status")).isEqualTo("verified");
        assertThat(verified.text("identityCheck")).isEqualTo("in_person");

        write(tenant, "UPDATE privacy.rights_request SET due_at = submitted_at + INTERVAL '1 microsecond'"
                + " WHERE tenant_id = :tenantId AND id = :id", id);
        assertThat(calls.get(request, staff).body().path("overdue").asBoolean()).isTrue();
        assertThat(calls.get(MY_REQUESTS + "/" + id, token).body().path("overdue").asBoolean()).isTrue();

        PrivacyCalls.Reply rejected = calls.post(request + ":reject", staff, Map.of("reasonCode", "duplicate"));
        assertThat(rejected.status()).as(rejected.body().toString()).isEqualTo(200);
        assertThat(rejected.text("status")).isEqualTo("rejected");
        assertThat(rejected.text("outcomeCode")).isEqualTo("duplicate");
        assertThat(rejected.body().path("overdue").asBoolean()).as("a rejected request is not overdue").isFalse();
        PrivacyCalls.Reply seen = calls.get(MY_REQUESTS + "/" + id, token);
        assertThat(seen.text("status")).isEqualTo("rejected");
        assertThat(seen.text("outcomeCode")).as("the requester learns why, to appeal").isEqualTo("duplicate");
        assertThat(calls.post(request + ":reject", staff, Map.of("reasonCode", "other")).status()).isEqualTo(409);
        assertThat(events(tenant)).extracting(event -> event.substring(0, event.indexOf(' '))).containsExactly(
                "privacy.rights_request.submitted", "privacy.rights_request.verified",
                "privacy.rights_request.rejected");
    }

    @Test
    void aBlankCloseNoteIsNoNote() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String id = calls.post(MY_REQUESTS, calls.person(tenant, self, REQUESTER),
                rightsRequest(self, "grievance", "A complaint.")).text("id");
        String staff = calls.administrator(tenant, RIGHTS_STAFF);
        String request = RIGHTS_REQUESTS + "/" + id;
        PrivacyCalls.Reply assigned = calls.post(request + ":assign", staff,
                Map.of("assigneeUserId", UUID.randomUUID().toString()));
        PrivacyCalls.Reply closed = calls.post(request + ":close", staff, Map.of("outcomeCode", "declined",
                "note", " ", "response", "We found no fault.", "etag", assigned.etag()));
        assertThat(closed.status()).as(closed.body().toString()).isEqualTo(200);
        assertThat(closed.body().has("outcomeNote")).isFalse();
    }

    @Test
    void anotherTenantSeesNothing() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String id = calls.post(MY_REQUESTS, calls.person(tenant, self, REQUESTER),
                rightsRequest(self, "access", WHAT_WE_HOLD)).text("id");

        String outsider = calls.administrator(other, RIGHTS_STAFF);
        String request = RIGHTS_REQUESTS + "/" + id;
        assertThat(calls.get(request, outsider).status()).isEqualTo(404);
        assertThat(calls.post(request + ":assign", outsider, Map.of("assigneeUserId", UUID.randomUUID().toString()))
                .status()).isEqualTo(404);
        assertThat(calls.get(RIGHTS_REQUESTS, outsider).body().path("data")).isEmpty();
        assertThat(calls.get(MY_REQUESTS + "/" + id, calls.person(other, self, REQUESTER)).status()).isEqualTo(404);
        assertThat(calls.get(MY_REQUESTS, calls.person(other, self, REQUESTER)).body().path("data")).isEmpty();
        assertThat(calls.get(RIGHTS_REQUESTS, calls.administrator(tenant, RIGHTS_STAFF)).body().path("data"))
                .hasSize(1);
    }
}
