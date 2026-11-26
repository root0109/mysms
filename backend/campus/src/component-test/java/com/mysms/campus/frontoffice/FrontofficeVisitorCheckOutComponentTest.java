package com.mysms.campus.frontoffice;

import com.mysms.campus.CampusApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.campus.frontoffice.config.FrontofficeDatabaseConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * frontoffice.CheckOutVisitor over HTTP against real PostgreSQL: the exit time is recorded once, never before the
 * entry, under the body etag, only inside the caller's institutions and with the step's own permission.
 */
@SpringBootTest(classes = CampusApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FrontofficeVisitorCheckOutComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String PERMISSION = "frontoffice.visitor_log.check_out_visitor";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(FrontofficeDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "frontoffice", "library", "transport", "facility");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {
    }

    /** One school of its own tenant with a desk clerk who may check visitors out. */
    private final class School {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final String clerk = token(tenant, institution.toString(), PERMISSION, "frontoffice.visitor_log.read");

        /** A visitor of the school, still inside; answers its id. */
        UUID visitor(String entryTime) {
            UUID id = UUID.randomUUID();
            sql("INSERT INTO frontoffice.visitor_log (id, tenant_id, institution_id, visitor_name, visitor_type,"
                    + " purpose, visit_date, entry_time, row_version) VALUES ('" + id + "', :tenantId, '" + institution
                    + "', 'Test visitor', 'parent', 'meeting', '2026-02-03', " + entryTime + ", 1)");
            return id;
        }

        long count(String statement) {
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> database.jdbc().sql(statement)
                            .query(Long.class).single()));
        }

        void sql(String statement) {
            CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> database.jdbc().sql(statement).update()));
        }
    }

    private Reply checkOut(UUID visitor, String body, String token) throws Exception {
        HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                        + "/v1/frontoffice/visitor-logs/" + visitor + ":checkOut"))
                .POST(HttpRequest.BodyPublishers.ofString(body)).header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token).build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    private static String token(UUID tenant, String institution, String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", "*".equals(institution) ? List.of(permissions) : List.of());
        claims.put("ins", List.of(institution));
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    @Test
    void aVisitorIsCheckedOutOnceWithAuditAndEvent() throws Exception {
        School school = new School();
        UUID visitor = school.visitor("'09:00'");

        Reply out = checkOut(visitor, "{\"exitTime\":\"17:30\",\"etag\":\"\\\"1\\\"\"}", school.clerk);
        assertThat(out.status()).as(out.text()).isEqualTo(200);
        assertThat(out.body().path("exitTime").asString()).startsWith("17:30");
        assertThat(out.body().path("etag").asString()).isEqualTo("\"2\"");
        assertThat(school.count("SELECT count(*) FROM frontoffice.outbox_event WHERE tenant_id = :tenantId"
                + " AND event_type = 'frontoffice.visitor_log.checkOutVisitor'")).isEqualTo(1);
        assertThat(school.count("SELECT count(*) FROM frontoffice.outbox_event WHERE tenant_id = :tenantId"
                + " AND event_type = 'audit.recorded' AND aggregate_id = '" + visitor
                + "' AND payload LIKE '%checkOutVisitor%'")).as("audited in the same transaction").isEqualTo(1);

        Reply again = checkOut(visitor, "{\"exitTime\":\"18:00\"}", school.clerk);
        assertThat(again.status()).as("a visitor leaves once: " + again.text()).isEqualTo(409);
    }

    @Test
    void theExitIsAValidTimeNotBeforeTheEntryAndMatchesTheEtag() throws Exception {
        School school = new School();
        UUID visitor = school.visitor("'09:00'");

        Reply before = checkOut(visitor, "{\"exitTime\":\"08:59\"}", school.clerk);
        assertThat(before.status()).as(before.text()).isEqualTo(422);
        assertThat(before.body().path("errors").get(0).path("field").asString()).isEqualTo("exitTime");
        assertThat(checkOut(visitor, "{\"exitTime\":\"half past five\"}", school.clerk).status()).isEqualTo(422);
        assertThat(checkOut(visitor, "{}", school.clerk).status()).as("exitTime is required").isEqualTo(422);
        assertThat(checkOut(visitor, "{\"exitTime\":\"17:30\",\"etag\":\"\\\"7\\\"\"}", school.clerk).status())
                .as("a stale etag").isEqualTo(412);
        assertThat(checkOut(visitor, "{\"exitTime\":\"17:30\"}", school.clerk).status()).as("nothing changed before")
                .isEqualTo(200);

        UUID tomorrow = school.visitor("'09:00'");
        school.sql("UPDATE frontoffice.visitor_log SET visit_date = (now() AT TIME ZONE 'UTC')::date + 2 WHERE id = '"
                + tomorrow + "'");
        Reply early = checkOut(tomorrow, "{\"exitTime\":\"17:30\"}", school.clerk);
        assertThat(early.status()).as("a visit still to come: " + early.text()).isEqualTo(422);
        assertThat(early.body().path("errors").get(0).path("code").asString()).isEqualTo("VisitNotStarted");

        UUID noEntryTime = school.visitor("NULL");
        assertThat(checkOut(noEntryTime, "{\"exitTime\":\"00:05\"}", school.clerk).status())
                .as("without an entry time any exit is accepted").isEqualTo(200);
    }

    @Test
    void onlyTheStepsPermissionInsideTheCallersInstitutionsAndTenant() throws Exception {
        School school = new School();
        UUID visitor = school.visitor("'09:00'");
        String body = "{\"exitTime\":\"17:30\"}";

        assertThat(checkOut(visitor, body, token(school.tenant, school.institution.toString(),
                "frontoffice.visitor_log.update")).status()).as("the step's own permission").isEqualTo(403);
        assertThat(checkOut(visitor, body, token(school.tenant, UUID.randomUUID().toString(), PERMISSION)).status())
                .as("another school's clerk").isEqualTo(404);
        assertThat(checkOut(visitor, body, token(UUID.randomUUID(), "*", PERMISSION)).status())
                .as("another tenant").isEqualTo(404);
        assertThat(checkOut(UUID.randomUUID(), body, school.clerk).status()).isEqualTo(404);
        school.sql("UPDATE frontoffice.visitor_log SET deleted_at = now() WHERE id = '" + visitor + "'");
        assertThat(checkOut(visitor, body, school.clerk).status()).as("a deleted visit").isEqualTo(404);
    }
}
