package com.mysms.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.events.GuardianLinkSync;
import com.mysms.privacy.events.SubjectSync;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
 * Legal holds against real PostgreSQL through the real edge: a hold on a subject, a data category or named records is
 * placed with its review date, moved under If-Match and released, and is never changed once released; what a hold
 * names must fit its scope; nothing is placed without a tenant-wide grant or while impersonating; and a module asking
 * which holds cover a subject (the internal ListActiveHolds) gets the subject's own holds and everyone's, record sets
 * narrowed to its module, released holds left out.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LegalHoldComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String HOLDS = "/v1/privacy/legal-holds";
    private static final String[] OFFICER = {"privacy.legal_hold.read", "privacy.legal_hold.place",
        "privacy.legal_hold.update", "privacy.legal_hold.release"};
    private static final String MODULE = TOKENS.service("sis", "privacy.legal_hold.check");
    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);

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

    private static Map<String, Object> hold(String scope, UUID subject) {
        Map<String, Object> body = new HashMap<>(Map.of("reasonCode", "litigation", "scope", scope,
                "reviewBy", TODAY.plusMonths(6).toString()));
        if (subject != null) {
            body.put("subjectPersonId", subject.toString());
        }
        return body;
    }

    private String placed(String officer, Map<String, Object> body) throws Exception {
        PrivacyCalls.Reply placed = calls.post(HOLDS, officer, body);
        assertThat(placed.status()).as(placed.body().toString()).isEqualTo(201);
        return placed.text("id");
    }

    private List<String> activeFor(UUID tenant, UUID subject, String module) throws Exception {
        PrivacyCalls.Reply active = calls.asService("GET", HOLDS + ":active?subjectPersonId=" + subject
                + (module == null ? "" : "&module=" + module), MODULE, tenant, null);
        assertThat(active.status()).as(active.body().toString()).isEqualTo(200);
        List<String> ids = new ArrayList<>();
        for (JsonNode hold : active.body().path("holds")) {
            ids.add(hold.path("id").asString());
        }
        return ids;
    }

    private List<String> outbox(UUID tenant) {
        return fixtures.read(tenant, () -> database.jdbc().sql(
                "SELECT o.event_type || ' ' || o.payload FROM privacy.outbox_event o WHERE o.tenant_id = :tenantId"
                        + " ORDER BY o.id").query(String.class).list());
    }

    @Test
    void aHoldIsPlacedMovedAndReleasedAndThenNoLongerChanges() throws Exception {
        UUID tenant = UUID.randomUUID();
        String officer = calls.administrator(tenant, OFFICER);
        UUID subject = UUID.randomUUID();

        PrivacyCalls.Reply placed = calls.post(HOLDS, officer, hold("subject", subject));
        assertThat(placed.status()).as(placed.body().toString()).isEqualTo(201);
        JsonNode hold = placed.body();
        String id = hold.path("id").asString();
        assertThat(hold.path("status").asString()).isEqualTo("active");
        assertThat(hold.path("reviewDue").asBoolean()).isFalse();
        assertThat(hold.path("placedByUserId").asString()).isNotEmpty();

        PrivacyCalls.Reply stale = calls.patch(HOLDS + "/" + id, officer, "\"7\"", Map.of("reviewBy", TODAY.toString()));
        assertThat(stale.status()).isEqualTo(412);
        PrivacyCalls.Reply moved = calls.patch(HOLDS + "/" + id, officer, placed.etag(),
                Map.of("reviewBy", TODAY.toString()));
        assertThat(moved.status()).as(moved.body().toString()).isEqualTo(200);
        assertThat(moved.body().path("reviewDue").asBoolean()).as("due on its review date").isTrue();
        PrivacyCalls.Reply due = calls.get(HOLDS + "?filter=" + URLEncoder.encode(
                "active = true AND reviewBy <= \"" + TODAY + "\"", StandardCharsets.UTF_8), officer);
        assertThat(due.body().path("data").findValuesAsString("id")).containsExactly(id);
        PrivacyCalls.Reply past = calls.patch(HOLDS + "/" + id, officer, moved.etag(),
                Map.of("reviewBy", TODAY.minusDays(1).toString()));
        assertThat(PrivacyCalls.fieldErrors(past)).containsExactly("reviewBy:LEGAL_HOLD_REVIEW_BY_PAST");

        PrivacyCalls.Reply released = calls.post(HOLDS + "/" + id + ":release", officer,
                Map.of("etag", moved.etag()));
        assertThat(released.status()).as(released.body().toString()).isEqualTo(200);
        assertThat(released.text("status")).isEqualTo("released");
        assertThat(released.body().path("reviewDue").asBoolean()).isFalse();
        assertThat(calls.post(HOLDS + "/" + id + ":release", officer, Map.of()).status()).isEqualTo(409);
        assertThat(calls.patch(HOLDS + "/" + id, officer, released.etag(),
                Map.of("reviewBy", TODAY.plusYears(1).toString())).status()).isEqualTo(409);

        assertThat(outbox(tenant)).anyMatch(e -> e.startsWith("privacy.legal_hold.placed "))
                .anyMatch(e -> e.startsWith("privacy.legal_hold.updated "))
                .anyMatch(e -> e.startsWith("privacy.legal_hold.released "));
    }

    @Test
    void whatAHoldNamesMustFitItsScope() throws Exception {
        UUID tenant = UUID.randomUUID();
        String officer = calls.administrator(tenant, OFFICER);

        PrivacyCalls.Reply noSubject = calls.post(HOLDS, officer, hold("subject", null));
        assertThat(PrivacyCalls.fieldErrors(noSubject)).containsExactly("subjectPersonId:LEGAL_HOLD_TARGET_REQUIRED");

        Map<String, Object> category = hold("category", null);
        category.put("categoryCode", "horoscope");
        category.put("module", "sis");
        assertThat(PrivacyCalls.fieldErrors(calls.post(HOLDS, officer, category))).containsExactlyInAnyOrder(
                "categoryCode:LEGAL_HOLD_CATEGORY_UNKNOWN", "module:LEGAL_HOLD_TARGET_NOT_ALLOWED");

        Map<String, Object> records = hold("records", null);
        records.put("module", "Student Info");
        records.put("entityType", "student");
        assertThat(PrivacyCalls.fieldErrors(calls.post(HOLDS, officer, records))).containsExactlyInAnyOrder(
                "entityIds:LEGAL_HOLD_TARGET_REQUIRED", "module:LEGAL_HOLD_MODULE_INVALID");

        Map<String, Object> late = hold("subject", UUID.randomUUID());
        late.put("reviewBy", TODAY.minusDays(1).toString());
        assertThat(PrivacyCalls.fieldErrors(calls.post(HOLDS, officer, late)))
                .containsExactly("reviewBy:LEGAL_HOLD_REVIEW_BY_PAST");
    }

    @Test
    void holdsArePlacedOnlyWithATenantWideGrantAndNeverWhileImpersonating() throws Exception {
        UUID tenant = UUID.randomUUID();
        Map<String, Object> body = hold("subject", UUID.randomUUID());
        assertThat(calls.post(HOLDS, calls.schoolAdministrator(tenant, OFFICER), body).status()).isEqualTo(403);
        assertThat(calls.post(HOLDS, calls.impersonatedAdministrator(tenant, UUID.randomUUID(), OFFICER), body)
                .status()).isEqualTo(403);
        assertThat(calls.post(HOLDS, calls.administrator(tenant, "privacy.legal_hold.read"), body).status())
                .isEqualTo(403);
        assertThat(calls.get(HOLDS + ":active?subjectPersonId=" + UUID.randomUUID(),
                calls.administrator(tenant, OFFICER)).status()).as("the check is for modules only").isEqualTo(403);
    }

    @Test
    void aModuleLearnsWhichActiveHoldsCoverASubject() throws Exception {
        UUID tenant = UUID.randomUUID();
        String officer = calls.administrator(tenant, OFFICER);
        UUID subject = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        String own = placed(officer, hold("subject", subject));
        String othersOwn = placed(officer, hold("subject", other));
        Map<String, Object> category = hold("category", null);
        category.put("categoryCode", "health");
        String everyonesHealth = placed(officer, category);
        Map<String, Object> sisRecords = hold("records", subject);
        sisRecords.putAll(Map.of("module", "sis", "entityType", "student",
                "entityIds", List.of(UUID.randomUUID().toString())));
        String inSis = placed(officer, sisRecords);
        Map<String, Object> financeRecords = hold("records", null);
        financeRecords.putAll(Map.of("module", "finance", "entityType", "invoice",
                "entityIds", List.of(UUID.randomUUID().toString())));
        String inFinance = placed(officer, financeRecords);
        String released = placed(officer, hold("subject", subject));
        assertThat(calls.post(HOLDS + "/" + released + ":release", officer, Map.of()).status()).isEqualTo(200);

        assertThat(activeFor(tenant, subject, null)).containsExactly(own, everyonesHealth, inSis, inFinance)
                .doesNotContain(othersOwn, released);
        assertThat(activeFor(tenant, subject, "sis")).containsExactly(own, everyonesHealth, inSis);
        assertThat(activeFor(UUID.randomUUID(), subject, null)).as("another tenant's holds never").isEmpty();
    }
}
