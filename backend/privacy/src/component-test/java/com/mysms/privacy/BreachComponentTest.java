package com.mysms.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.events.GuardianLinkSync;
import com.mysms.privacy.events.SubjectSync;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
import tools.jackson.databind.JsonNode;

/**
 * The breach register against real PostgreSQL through the real edge: a breach runs from its report to its close
 * against the Board's 72-hour clock, each notice recorded and moving the status where it is a step; a breach found
 * not to be one ends there; what may change is changed under If-Match; nothing is reported or changed without a
 * tenant-wide grant or while impersonating; no event carries what the staff wrote.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BreachComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String BREACHES = "/v1/privacy/breaches";
    private static final String[] OFFICER = {"privacy.breach.read", "privacy.breach.report", "privacy.breach.update",
        "privacy.breach.triage", "privacy.breach.contain", "privacy.breach.record_notification",
        "privacy.breach.close", "privacy.breach.mark_not_a_breach"};
    private static final String WHAT_HAPPENED = "Export job 7f3c sent 412 rows of sis.student to a wrong bucket.";

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

    private static OffsetDateTime hoursAgo(long hours) {
        return OffsetDateTime.now(ZoneOffset.UTC).minusHours(hours).truncatedTo(ChronoUnit.SECONDS);
    }

    /** ReportBreach's body: detected an hour before the school became aware. */
    private static Map<String, Object> report(OffsetDateTime awareAt) {
        Map<String, Object> body = new HashMap<>();
        body.put("detectedAt", awareAt.minusHours(1).toString());
        body.put("becameAwareAt", awareAt.toString());
        body.put("description", WHAT_HAPPENED);
        body.put("categories", List.of("contact"));
        body.put("modules", List.of("sis"));
        return body;
    }

    private String reported(String officer, OffsetDateTime awareAt) throws Exception {
        PrivacyCalls.Reply reported = calls.post(BREACHES, officer, report(awareAt));
        assertThat(reported.status()).as(reported.body().toString()).isEqualTo(201);
        return reported.text("id");
    }

    private JsonNode step(String officer, String id, String step, Map<String, Object> body) throws Exception {
        PrivacyCalls.Reply stepped = calls.post(BREACHES + "/" + id + ":" + step, officer, body);
        assertThat(stepped.status()).as(stepped.body().toString()).isEqualTo(200);
        return stepped.body();
    }

    private static Map<String, Object> notice(String audience, String kind, OffsetDateTime sentAt) {
        return Map.of("audience", audience, "kind", kind, "sentAt", sentAt.toString());
    }

    /** Every event type and payload the tenant's outbox holds, audit records included. */
    private List<String> outbox(UUID tenant) {
        return fixtures.read(tenant, () -> database.jdbc().sql(
                "SELECT o.event_type || ' ' || o.payload FROM privacy.outbox_event o WHERE o.tenant_id = :tenantId"
                        + " ORDER BY o.id").query(String.class).list());
    }

    @Test
    void aBreachRunsFromReportToCloseAgainstTheBoardsClock() throws Exception {
        UUID tenant = UUID.randomUUID();
        String officer = calls.administrator(tenant, OFFICER);
        OffsetDateTime awareAt = hoursAgo(2);

        PrivacyCalls.Reply reported = calls.post(BREACHES, officer, report(awareAt));
        assertThat(reported.status()).as(reported.body().toString()).isEqualTo(201);
        JsonNode breach = reported.body();
        String id = breach.path("id").asString();
        assertThat(breach.path("number").asString()).matches("PRB-\\d{4}-\\d{6}");
        assertThat(breach.path("status").asString()).isEqualTo("reported");
        assertThat(OffsetDateTime.parse(breach.path("boardReportDueAt").asString()))
                .isAtSameInstantAs(awareAt.plusHours(72));
        assertThat(breach.path("overdue").asBoolean()).isFalse();
        assertThat(breach.path("hoursRemaining").asInt()).isBetween(69, 70);

        PrivacyCalls.Reply tooEarly = calls.post(BREACHES + "/" + id + ":recordNotification", officer,
                notice("board", "initial", awareAt.plusMinutes(30)));
        assertThat(tooEarly.status()).as("the Board's intimation follows containment").isEqualTo(409);
        assertThat(step(officer, id, "triage", Map.of("etag", breach.path("etag").asString())).path("status")
                .asString()).isEqualTo("triaged");
        assertThat(step(officer, id, "contain", Map.of()).path("status").asString()).isEqualTo("contained");
        assertThat(step(officer, id, "recordNotification", notice("board", "initial", awareAt.plusMinutes(30)))
                .path("status").asString()).isEqualTo("board_initial_sent");
        PrivacyCalls.Reply notYet = calls.post(BREACHES + "/" + id + ":close", officer, Map.of());
        assertThat(notYet.status()).as("closed only once the principals are told").isEqualTo(409);
        JsonNode boardReport = step(officer, id, "recordNotification",
                notice("board", "detailed", awareAt.plusHours(1)));
        assertThat(boardReport.path("status").asString()).isEqualTo("board_report_sent");
        assertThat(boardReport.path("boardReportSentAt").isMissingNode()).isFalse();
        assertThat(boardReport.path("hoursRemaining").isMissingNode()).as("the clock stops").isTrue();
        assertThat(calls.post(BREACHES + "/" + id + ":recordNotification", officer,
                notice("board", "detailed", awareAt.plusHours(1))).status()).as("one detailed report").isEqualTo(409);
        assertThat(step(officer, id, "recordNotification", notice("principals", "detailed", awareAt.plusHours(1)))
                .path("status").asString()).isEqualTo("principals_notified");
        JsonNode closed = step(officer, id, "close", Map.of());
        assertThat(closed.path("status").asString()).isEqualTo("closed");
        assertThat(closed.path("notifications").findValuesAsString("audience"))
                .containsExactly("board", "board", "principals");

        PrivacyCalls.Reply afterClose = calls.patch(BREACHES + "/" + id, officer, closed.path("etag").asString(),
                Map.of("estimatedPrincipals", 412));
        assertThat(afterClose.status()).as("a closed breach no longer changes").isEqualTo(409);
        List<String> events = outbox(tenant);
        assertThat(events).anyMatch(e -> e.startsWith("privacy.breach.reported "))
                .anyMatch(e -> e.startsWith("privacy.breach.notification_recorded "))
                .anyMatch(e -> e.startsWith("privacy.breach.closed "))
                .noneMatch(e -> e.contains("wrong bucket"));
    }

    @Test
    void aBreachPastItsDeadlineIsOverdueAndOneFoundNotToBeABreachEndsThere() throws Exception {
        UUID tenant = UUID.randomUUID();
        String officer = calls.administrator(tenant, OFFICER);
        String late = reported(officer, hoursAgo(80));

        JsonNode overdue = calls.get(BREACHES + "/" + late, officer).body();
        assertThat(overdue.path("overdue").asBoolean()).isTrue();
        assertThat(overdue.path("hoursRemaining").asInt()).isBetween(-9, -8);
        assertThat(calls.get(BREACHES + "?filter=status%20%3D%20%22reported%22", officer).body().path("data"))
                .hasSize(1);

        PrivacyCalls.Reply untriaged = calls.post(BREACHES + "/" + late + ":markNotABreach", officer,
                Map.of("reason", "A test export."));
        assertThat(untriaged.status()).as("triage decides").isEqualTo(409);
        step(officer, late, "triage", Map.of());
        JsonNode notABreach = step(officer, late, "markNotABreach", Map.of("reason", "A test export."));
        assertThat(notABreach.path("status").asString()).isEqualTo("not_a_breach");
        assertThat(notABreach.path("notABreachReason").asString()).isEqualTo("A test export.");
        assertThat(notABreach.path("overdue").asBoolean()).isFalse();
        assertThat(notABreach.path("closedAt").isMissingNode()).isFalse();
    }

    @Test
    void whatIsKnownChangesUnderIfMatchAndAReportIsChecked() throws Exception {
        UUID tenant = UUID.randomUUID();
        String officer = calls.administrator(tenant, OFFICER);
        String id = reported(officer, hoursAgo(1));

        PrivacyCalls.Reply updated = calls.patch(BREACHES + "/" + id, officer, "\"1\"",
                Map.of("estimatedPrincipals", 412, "mitigation", "Bucket 7f3c made private; link revoked."));
        assertThat(updated.status()).as(updated.body().toString()).isEqualTo(200);
        assertThat(updated.body().path("estimatedPrincipals").asInt()).isEqualTo(412);
        assertThat(updated.etag()).isEqualTo("\"2\"");
        assertThat(calls.patch(BREACHES + "/" + id, officer, "\"1\"", Map.of("estimatedPrincipals", 1)).status())
                .isEqualTo(412);
        assertThat(calls.post(BREACHES + "/" + id + ":triage", officer, Map.of("etag", "\"1\"")).status())
                .isEqualTo(412);

        Map<String, Object> future = report(hoursAgo(-2));
        assertThat(PrivacyCalls.fieldErrors(calls.post(BREACHES, officer, future)))
                .containsExactlyInAnyOrder("detectedAt:BREACH_TIME_IN_FUTURE", "becameAwareAt:BREACH_TIME_IN_FUTURE");
        Map<String, Object> awareFirst = report(hoursAgo(3));
        awareFirst.put("detectedAt", hoursAgo(1).toString());
        assertThat(PrivacyCalls.fieldErrors(calls.post(BREACHES, officer, awareFirst)))
                .containsExactly("becameAwareAt:BREACH_AWARE_BEFORE_DETECTED");
        Map<String, Object> unknown = report(hoursAgo(1));
        unknown.put("categories", List.of("no_such_category"));
        unknown.put("modules", List.of("Not A Module"));
        assertThat(PrivacyCalls.fieldErrors(calls.post(BREACHES, officer, unknown)))
                .containsExactlyInAnyOrder("categories:BREACH_CATEGORY_UNKNOWN", "modules:BREACH_MODULE_INVALID");
        PrivacyCalls.Reply early = calls.post(BREACHES + "/" + id + ":recordNotification", officer,
                notice("tenant", "initial", hoursAgo(5)));
        assertThat(PrivacyCalls.fieldErrors(early)).containsExactly("sentAt:BREACH_NOTICE_BEFORE_AWARE");
        assertThat(step(officer, id, "recordNotification", notice("tenant", "initial", hoursAgo(0)))
                .path("status").asString()).as("the tenant's notice is no step").isEqualTo("reported");
    }

    @Test
    void onlyATenantWideOfficerReportsOrChangesABreachAndNeverWhileImpersonating() throws Exception {
        UUID tenant = UUID.randomUUID();
        String officer = calls.administrator(tenant, OFFICER);
        String id = reported(officer, hoursAgo(1));

        assertThat(calls.post(BREACHES, calls.schoolAdministrator(tenant, OFFICER), report(hoursAgo(1))).status())
                .isEqualTo(403);
        assertThat(calls.post(BREACHES + "/" + id + ":triage", calls.schoolAdministrator(tenant, OFFICER), Map.of())
                .status()).isEqualTo(403);
        String acting = calls.impersonatedAdministrator(tenant, UUID.randomUUID(), OFFICER);
        assertThat(calls.post(BREACHES, acting, report(hoursAgo(1))).status()).isEqualTo(403);
        assertThat(calls.post(BREACHES + "/" + id + ":triage", acting, Map.of()).status()).isEqualTo(403);
        assertThat(calls.get(BREACHES + "/" + id, acting).status()).as("support staff may look").isEqualTo(200);
        assertThat(calls.get(BREACHES + "/" + id, calls.administrator(tenant, "privacy.rights_request.read"))
                .status()).isEqualTo(403);
        assertThat(calls.get(BREACHES + "/" + id, calls.administrator(UUID.randomUUID(), OFFICER)).status())
                .as("another tenant's breach does not exist").isEqualTo(404);
    }
}
