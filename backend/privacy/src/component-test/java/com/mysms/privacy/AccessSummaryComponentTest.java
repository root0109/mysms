package com.mysms.privacy;

import static com.mysms.privacy.ConsentFixtures.MY_CONSENTS;
import static com.mysms.privacy.ConsentFixtures.MY_REQUESTS;
import static com.mysms.privacy.ConsentFixtures.PHOTOS;
import static com.mysms.privacy.ConsentFixtures.REQUESTER;
import static com.mysms.privacy.ConsentFixtures.RIGHTS_REQUESTS;
import static com.mysms.privacy.ConsentFixtures.RIGHTS_STAFF;
import static com.mysms.privacy.ConsentFixtures.SELF_SERVICE;
import static com.mysms.privacy.ConsentFixtures.decision;
import static com.mysms.privacy.ConsentFixtures.rightsRequest;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.events.GuardianLinkSync;
import com.mysms.privacy.events.SubjectSync;
import java.time.OffsetDateTime;
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
 * The access summary against real PostgreSQL through the real edge: the register activities that may concern an adult
 * and a minor (consent-based activities only while switched on; for a minor only activities that process minors' data,
 * never a purpose forbidden for minors), with what the subject's consent allows; the staff's preview while the request
 * is open, and none once it ended without a summary; the fulfilment step with its required etag, impersonation, state
 * checks; the requester's copy, kept as sent and read back whatever a later version added to it, with the visibility of
 * their own request; tenant isolation; and the closing event without the summary.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccessSummaryComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String WHAT_WE_HOLD = "Please tell me what the school holds about me and why.";
    /** A consent purpose forbidden for minors, with the register's lms.proctoring_video. */
    private static final String MONITORING = "biometric_or_video_monitoring";
    private static final String PHOTO_ACTIVITY = "comms.community_photo_publication";
    private static final String PROCTORING_ACTIVITY = "lms.proctoring_video";
    private static final String NOTICE_ACTIVITY = "comms.operational_notice";
    private static final String SUMMARY = "/access-summary";
    private static final String MY_SUMMARY_PATH = MY_REQUESTS + "/";

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

    /** The caller's access request about the subject; answers its id. */
    private String accessRequest(String token, UUID subject) throws Exception {
        PrivacyCalls.Reply submitted = calls.post(MY_REQUESTS, token, rightsRequest(subject, "access", WHAT_WE_HOLD));
        assertThat(submitted.status()).as(submitted.body().toString()).isEqualTo(201);
        return submitted.text("id");
    }

    /** The decision recorded by the giver's own token; answers the consent record. */
    private PrivacyCalls.Reply consent(String token, UUID subject, String purposeCode, String scope, String version)
            throws Exception {
        PrivacyCalls.Reply recorded = calls.post(MY_CONSENTS, token,
                decision(subject, purposeCode, scope, "granted", version, "en"));
        assertThat(recorded.status()).as(recorded.body().toString()).isEqualTo(201);
        return recorded;
    }

    /** Assigns the request to a staff member; answers the assigned request. */
    private PrivacyCalls.Reply assign(String staff, String id) throws Exception {
        PrivacyCalls.Reply assigned = calls.post(RIGHTS_REQUESTS + "/" + id + ":assign", staff,
                Map.of("assigneeUserId", UUID.randomUUID().toString()));
        assertThat(assigned.status()).as(assigned.body().toString()).isEqualTo(200);
        return assigned;
    }

    private static List<String> codes(JsonNode summary) {
        return summary.path("activities").findValuesAsString("activityCode");
    }

    private static JsonNode activity(JsonNode summary, String code) {
        for (JsonNode activity : summary.path("activities")) {
            if (code.equals(activity.path("activityCode").asString())) {
                return activity;
            }
        }
        throw new AssertionError("no activity " + code + " in " + summary);
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asString()));
        return values;
    }

    @Test
    void anAdultsSummaryListsTheRegisterAndWhatTheirConsentAllows() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String version = fixtures.enable(tenant, PHOTOS, MONITORING);
        consent(calls.person(tenant, self, SELF_SERVICE), self, PHOTOS, "institution", version);
        String id = accessRequest(calls.person(tenant, self, REQUESTER), self);

        PrivacyCalls.Reply preview = calls.get(RIGHTS_REQUESTS + "/" + id + SUMMARY,
                calls.administrator(tenant, RIGHTS_STAFF));
        assertThat(preview.status()).as(preview.body().toString()).isEqualTo(200);
        JsonNode summary = preview.body();
        assertThat(summary.path("subjectPersonId").asString()).isEqualTo(self.toString());
        assertThat(summary.path("subjectStatus").asString()).isEqualTo("adult");
        assertThat(summary.has("generatedAt")).isTrue();
        assertThat(codes(summary)).as("every activity not on the consent basis, and the switched-on consent ones")
                .contains("admission.application", NOTICE_ACTIVITY, "finance.payment_gateway",
                        "frontoffice.visitor_log", "sis.health_record", "transport.live_location_share",
                        PHOTO_ACTIVITY, PROCTORING_ACTIVITY)
                .as("a purpose the tenant has not switched on, or one the platform decides")
                .doesNotContain("comms.marketing_broadcast", "lms.learning_analytics", "platform.product_analytics");

        JsonNode photos = activity(summary, PHOTO_ACTIVITY);
        assertThat(photos.path("module").asString()).isEqualTo("comms");
        assertThat(photos.path("purposeCode").asString()).isEqualTo(PHOTOS);
        assertThat(photos.path("legalBasis").asString()).isEqualTo("consent");
        assertThat(strings(photos.path("dataCategories"))).containsExactly("identity", "photo_media");
        assertThat(photos.path("recipients")).isEmpty();
        assertThat(photos.path("crossBorder").asBoolean()).isFalse();
        assertThat(strings(photos.path("consentAllowedScopes"))).as("a grant covers every narrower width")
                .containsExactly("class", "institution");
        JsonNode proctoring = activity(summary, PROCTORING_ACTIVITY);
        assertThat(proctoring.has("consentAllowedScopes")).isTrue();
        assertThat(proctoring.path("consentAllowedScopes")).as("never answered: not allowed").isEmpty();
        JsonNode notices = activity(summary, NOTICE_ACTIVITY);
        assertThat(notices.path("legalBasis").asString()).isEqualTo("legitimate_use");
        assertThat(notices.has("consentAllowedScopes")).as("no consent behind a legitimate use").isFalse();
    }

    @Test
    void aMinorsSummaryLeavesOutWhatDoesNotReachMinors() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID parent = UUID.randomUUID();
        fixtures.minor(tenant, child);
        fixtures.adult(tenant, parent);
        fixtures.guardian(tenant, parent, child);
        String version = fixtures.enable(tenant, PHOTOS, MONITORING);
        consent(calls.person(tenant, parent, SELF_SERVICE), child, PHOTOS, "class", version);
        String id = accessRequest(calls.person(tenant, parent, REQUESTER), child);

        JsonNode summary = calls.get(RIGHTS_REQUESTS + "/" + id + SUMMARY, calls.administrator(tenant, RIGHTS_STAFF))
                .body();
        assertThat(summary.path("subjectPersonId").asString()).isEqualTo(child.toString());
        assertThat(summary.path("subjectStatus").asString()).isEqualTo("minor");
        assertThat(codes(summary)).contains(PHOTO_ACTIVITY, "admission.application", "sis.health_record")
                .as("never a purpose forbidden for minors, even switched on").doesNotContain(PROCTORING_ACTIVITY)
                .as("never an activity that does not process minors' data")
                .doesNotContain(NOTICE_ACTIVITY, "frontoffice.visitor_log", "finance.payment_gateway");
        assertThat(strings(activity(summary, PHOTO_ACTIVITY).path("consentAllowedScopes"))).containsExactly("class");
    }

    @Test
    void theSummaryListsOnlyWhatReachesTheSubjectsRolesAtTheSchool() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        UUID parent = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID visitor = UUID.randomUUID();
        fixtures.adult(tenant, student);
        fixtures.student(tenant, student);
        fixtures.adult(tenant, parent);
        fixtures.minor(tenant, child);
        fixtures.guardian(tenant, parent, child);
        fixtures.adult(tenant, visitor);
        String staff = calls.administrator(tenant, RIGHTS_STAFF);

        JsonNode asStudent = calls.get(RIGHTS_REQUESTS + "/" + accessRequest(calls.person(tenant, student, REQUESTER),
                student) + SUMMARY, staff).body();
        assertThat(strings(asStudent.path("subjectRoles"))).containsExactly("student");
        assertThat(codes(asStudent)).contains("sis.health_record", NOTICE_ACTIVITY)
                .as("an activity of guardians only").doesNotContain("frontoffice.visitor_log");

        JsonNode asGuardian = calls.get(RIGHTS_REQUESTS + "/" + accessRequest(calls.person(tenant, parent, REQUESTER),
                parent) + SUMMARY, staff).body();
        assertThat(strings(asGuardian.path("subjectRoles"))).containsExactly("guardian");
        assertThat(codes(asGuardian)).contains("frontoffice.visitor_log", NOTICE_ACTIVITY)
                .as("activities of students only").doesNotContain("sis.health_record");

        JsonNode noRole = calls.get(RIGHTS_REQUESTS + "/" + accessRequest(calls.person(tenant, visitor, REQUESTER),
                visitor) + SUMMARY, staff).body();
        assertThat(noRole.path("subjectRoles")).as("no role known").isEmpty();
        assertThat(codes(noRole)).as("may concern: nothing left out for its audience")
                .contains("frontoffice.visitor_log", "sis.health_record");
    }

    @Test
    void onlyAnAccessRequestHasASummary() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String token = calls.person(tenant, self, REQUESTER);
        String staff = calls.administrator(tenant, RIGHTS_STAFF);
        String grievance = calls.post(MY_REQUESTS, token, rightsRequest(self, "grievance", "A complaint.")).text("id");

        PrivacyCalls.Reply preview = calls.get(RIGHTS_REQUESTS + "/" + grievance + SUMMARY, staff);
        assertThat(preview.status()).isEqualTo(409);
        assertThat(preview.text("code")).isEqualTo("INVALID_STATE_TRANSITION");
        assertThat(calls.get(RIGHTS_REQUESTS + "/" + UUID.randomUUID() + SUMMARY, staff).status()).isEqualTo(404);
        PrivacyCalls.Reply assigned = assign(staff, grievance);
        assertThat(calls.post(RIGHTS_REQUESTS + "/" + grievance + ":fulfilAccess", staff,
                Map.of("etag", assigned.etag())).status()).as("a grievance is not answered with a summary")
                .isEqualTo(409);
        assertThat(calls.get(MY_SUMMARY_PATH + grievance + SUMMARY, token).status()).isEqualTo(404);
    }

    @Test
    void fulfillingKeepsTheSummaryTheRequesterSeesWhateverChangesLater() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String version = fixtures.enable(tenant, PHOTOS);
        String consents = calls.person(tenant, self, SELF_SERVICE);
        PrivacyCalls.Reply granted = consent(consents, self, PHOTOS, "institution", version);
        String token = calls.person(tenant, self, REQUESTER);
        String id = accessRequest(token, self);
        String staff = calls.administrator(tenant, RIGHTS_STAFF);
        String request = RIGHTS_REQUESTS + "/" + id;
        String mySummary = MY_SUMMARY_PATH + id + SUMMARY;
        Map<String, String> verified = Map.of("etag", calls.get(request, staff).etag());

        assertThat(calls.post(request + ":fulfilAccess", staff, verified).status())
                .as("not before the request is in progress").isEqualTo(409);
        PrivacyCalls.Reply early = calls.get(mySummary, token);
        assertThat(early.status()).as("no summary before the request is fulfilled").isEqualTo(404);
        assertThat(early.text("code")).isEqualTo("NOT_FOUND");
        assertThat(calls.post(request + ":fulfilAccess", calls.schoolAdministrator(tenant, RIGHTS_STAFF), verified)
                .status()).as("rights requests are tenant-level: a school's grant cannot fulfil them").isEqualTo(403);
        assertThat(calls.post(request + ":fulfilAccess", calls.administrator(tenant, "privacy.rights_request.read"),
                verified).status()).isEqualTo(403);

        PrivacyCalls.Reply assigned = assign(staff, id);
        PrivacyCalls.Reply withoutEtag = calls.post(request + ":fulfilAccess", staff, Map.of());
        assertThat(withoutEtag.status()).as("the staff fulfil the version they reviewed").isEqualTo(422);
        assertThat(PrivacyCalls.fieldErrors(withoutEtag)).contains("etag:NotNull");
        assertThat(calls.post(request + ":fulfilAccess", staff, Map.of("etag", "\"1\"")).status()).isEqualTo(412);
        PrivacyCalls.Reply impersonated = calls.post(request + ":fulfilAccess",
                calls.impersonatedAdministrator(tenant, UUID.randomUUID(), RIGHTS_STAFF),
                Map.of("etag", assigned.etag()));
        assertThat(impersonated.status()).as("never decided while acting as another user").isEqualTo(403);
        assertThat(impersonated.text("code")).isEqualTo("PERMISSION_DENIED");
        PrivacyCalls.Reply fulfilled = calls.post(request + ":fulfilAccess", staff, Map.of("etag", assigned.etag()));
        assertThat(fulfilled.status()).as(fulfilled.body().toString()).isEqualTo(200);
        assertThat(fulfilled.text("status")).isEqualTo("closed");
        assertThat(fulfilled.text("outcomeCode")).isEqualTo("fulfilled");
        assertThat(fulfilled.body().has("closedAt")).isTrue();
        assertThat(fulfilled.etag()).isEqualTo("\"3\"");
        assertThat(calls.post(request + ":fulfilAccess", staff, Map.of("etag", fulfilled.etag())).status())
                .as("fulfilled once").isEqualTo(409);

        PrivacyCalls.Reply sent = calls.get(mySummary, token);
        assertThat(sent.status()).as(sent.body().toString()).isEqualTo(200);
        assertThat(OffsetDateTime.parse(sent.text("generatedAt")))
                .isAtSameInstantAs(OffsetDateTime.parse(fulfilled.text("closedAt")));
        assertThat(strings(activity(sent.body(), PHOTO_ACTIVITY).path("consentAllowedScopes")))
                .containsExactly("class", "institution");
        PrivacyCalls.Reply mine = calls.get(MY_REQUESTS + "/" + id, token);
        assertThat(mine.text("status")).isEqualTo("closed");
        assertThat(mine.text("outcomeCode")).isEqualTo("fulfilled");

        PrivacyCalls.Reply withdrawn = calls.post(MY_CONSENTS + "/" + granted.text("id") + ":withdraw", consents,
                Map.of("etag", granted.text("etag")));
        assertThat(withdrawn.status()).as(withdrawn.body().toString()).isEqualTo(200);
        assertThat(calls.get(mySummary, token).body()).as("the summary is kept as it was sent")
                .isEqualTo(sent.body());
        assertThat(calls.get(request + SUMMARY, staff).body()).as("the staff see what was sent, too")
                .isEqualTo(sent.body());

        List<String> events = events(tenant);
        assertThat(events).extracting(event -> event.substring(0, event.indexOf(' '))).containsExactly(
                "privacy.rights_request.submitted", "privacy.rights_request.assigned",
                "privacy.rights_request.closed");
        assertThat(events.get(2)).contains("\"status\":\"closed\"").contains("\"type\":\"access\"");
        assertThat(events).as("the summary never leaves privacy").noneMatch(event -> event.contains("activities")
                || event.contains(PHOTO_ACTIVITY) || event.contains("consentAllowedScopes"));
    }

    @Test
    void onlyTheRequesterWhoStillDecidesSeesTheSummary() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID parent = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        fixtures.minor(tenant, child);
        fixtures.adult(tenant, parent);
        fixtures.adult(tenant, stranger);
        UUID link = fixtures.guardian(tenant, parent, child);
        String token = calls.person(tenant, parent, REQUESTER);
        String id = accessRequest(token, child);
        String staff = calls.administrator(tenant, RIGHTS_STAFF);
        PrivacyCalls.Reply assigned = assign(staff, id);
        assertThat(calls.post(RIGHTS_REQUESTS + "/" + id + ":fulfilAccess", staff, Map.of("etag", assigned.etag()))
                .status()).isEqualTo(200);
        String mySummary = MY_SUMMARY_PATH + id + SUMMARY;

        PrivacyCalls.Reply seen = calls.get(mySummary, token);
        assertThat(seen.status()).as(seen.body().toString()).isEqualTo(200);
        assertThat(seen.text("subjectPersonId")).isEqualTo(child.toString());
        assertThat(calls.get(mySummary, calls.person(tenant, stranger, REQUESTER)).status())
                .as("another person's request").isEqualTo(404);
        assertThat(calls.get(mySummary, calls.person(tenant, child, REQUESTER)).status())
                .as("the child's rights are exercised by their legal guardian").isEqualTo(404);

        fixtures.unlink(tenant, link, parent, child);
        assertThat(calls.get(mySummary, token).status()).as("a guardian whose link ended").isEqualTo(404);
        assertThat(calls.get(RIGHTS_REQUESTS + "/" + id + SUMMARY, staff).status()).as("the staff still see it")
                .isEqualTo(200);
    }

    @Test
    void anotherTenantSeesNoSummary() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String id = accessRequest(calls.person(tenant, self, REQUESTER), self);
        String staff = calls.administrator(tenant, RIGHTS_STAFF);
        PrivacyCalls.Reply assigned = assign(staff, id);

        String outsider = calls.administrator(other, RIGHTS_STAFF);
        String request = RIGHTS_REQUESTS + "/" + id;
        assertThat(calls.get(request + SUMMARY, outsider).status()).isEqualTo(404);
        assertThat(calls.post(request + ":fulfilAccess", outsider, Map.of("etag", assigned.etag())).status())
                .isEqualTo(404);
        assertThat(calls.post(request + ":fulfilAccess", staff, Map.of("etag", assigned.etag())).status())
                .isEqualTo(200);
        assertThat(calls.get(MY_SUMMARY_PATH + id + SUMMARY, calls.person(other, self, REQUESTER)).status())
                .isEqualTo(404);
        assertThat(calls.get(MY_SUMMARY_PATH + id + SUMMARY, calls.person(tenant, self, REQUESTER)).status())
                .isEqualTo(200);
    }

    @Test
    void aRequestThatEndedWithoutASummaryHasNone() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String token = calls.person(tenant, self, REQUESTER);
        String staff = calls.administrator(tenant, RIGHTS_STAFF);
        String rejected = accessRequest(token, self);
        assertThat(calls.post(RIGHTS_REQUESTS + "/" + rejected + ":reject", staff, Map.of("reasonCode", "duplicate"))
                .status()).isEqualTo(200);
        String declined = accessRequest(token, self);
        PrivacyCalls.Reply assigned = assign(staff, declined);
        assertThat(calls.post(RIGHTS_REQUESTS + "/" + declined + ":close", staff,
                Map.of("outcomeCode", "declined", "etag", assigned.etag())).status()).isEqualTo(200);

        PrivacyCalls.Reply afterRejection = calls.get(RIGHTS_REQUESTS + "/" + rejected + SUMMARY, staff);
        assertThat(afterRejection.status()).as("no preview once the request is over").isEqualTo(404);
        assertThat(afterRejection.text("code")).isEqualTo("NOT_FOUND");
        assertThat(calls.get(RIGHTS_REQUESTS + "/" + declined + SUMMARY, staff).status()).isEqualTo(404);
        assertThat(calls.get(MY_SUMMARY_PATH + declined + SUMMARY, token).status()).isEqualTo(404);
    }

    @Test
    void aKeptSummaryIsReadWhateverALaterVersionAddedToIt() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID self = UUID.randomUUID();
        fixtures.adult(tenant, self);
        String token = calls.person(tenant, self, REQUESTER);
        String id = accessRequest(token, self);
        // a summary as a later version keeps it: a higher schemaVersion and properties this version does not know
        String kept = """
                {"schemaVersion": 3, "subjectPersonId": "%s", "subjectStatus": "adult",
                 "generatedAt": "2026-11-20T10:15:30Z", "producedBy": "a later version",
                 "activities": [{"activityCode": "comms.operational_notice", "module": "comms",
                   "purposeCode": "operational_communication", "legalBasis": "legitimate_use",
                   "dataCategories": ["contact"],
                   "recipients": [{"kind": "processor", "name": "SMS gateway", "country": "IN", "contract": "DPA-7"}],
                   "crossBorder": false, "retentionPolicyCode": null, "consentAllowedScopes": null,
                   "riskRating": "low"}]}
                """.formatted(self);
        int written = fixtures.write(tenant, () -> database.jdbc().sql("""
                UPDATE privacy.rights_request
                   SET status = 'closed', outcome_code = 'fulfilled', closed_at = now(), updated_at = now(),
                       access_summary = CAST(:summary AS jsonb), row_version = row_version + 1
                 WHERE tenant_id = :tenantId AND id = :id
                """).param("summary", kept).param("id", UUID.fromString(id)).update());
        assertThat(written).isEqualTo(1);

        for (PrivacyCalls.Reply read : List.of(calls.get(MY_SUMMARY_PATH + id + SUMMARY, token),
                calls.get(RIGHTS_REQUESTS + "/" + id + SUMMARY, calls.administrator(tenant, RIGHTS_STAFF)))) {
            assertThat(read.status()).as(read.body().toString()).isEqualTo(200);
            assertThat(read.text("subjectPersonId")).isEqualTo(self.toString());
            assertThat(read.text("subjectStatus")).isEqualTo("adult");
            assertThat(OffsetDateTime.parse(read.text("generatedAt")))
                    .isAtSameInstantAs(OffsetDateTime.parse("2026-11-20T10:15:30Z"));
            assertThat(codes(read.body())).containsExactly(NOTICE_ACTIVITY);
            JsonNode recipient = activity(read.body(), NOTICE_ACTIVITY).path("recipients").path(0);
            assertThat(recipient.path("name").asString()).isEqualTo("SMS gateway");
            assertThat(recipient.has("contract")).isFalse();
        }
    }
}
