package com.mysms.org;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.org.config.OrgDatabaseConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * org.ActivateAcademicYear and org.GenerateTerms over HTTP against real PostgreSQL as the application role: one active
 * and one current year per institution, terms planned from the year's structure, idempotent re-runs, overlaps
 * refused, and the caller's institutions and permissions. Each test works in a tenant of its own.
 */
@SpringBootTest(classes = OrgApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrgAcademicYearWorkflowComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String ACTIVATE = "org.academic_year.activate_academic_year";
    private static final String GENERATE = "org.academic_year.generate_terms";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(OrgDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "org");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {

        List<String> names() {
            return body.path("items").findValuesAsString("name");
        }
    }

    /** One school of its own tenant and an administrator of it. */
    private final class School {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final String admin = token(tenant, institution.toString(), ACTIVATE, GENERATE, "org.academic_year.read",
                "org.academic_year.close_academic_year");

        School() {
            sql("INSERT INTO org.institution (id, tenant_id, code, name, row_version) VALUES ('" + institution
                    + "', :tenantId, 'S" + institution.toString().substring(0, 8) + "', 'Test school', 1)");
        }

        /** A year of the school; answers its id. */
        UUID year(String name, String status, boolean current, String structure) {
            UUID id = UUID.randomUUID();
            sql("INSERT INTO org.academic_year (id, tenant_id, institution_id, name, start_date, end_date, status,"
                    + " is_current, term_structure, row_version) VALUES ('" + id + "', :tenantId, '" + institution
                    + "', '" + name + "', '2027-04-01', '2028-03-31', '" + status + "', " + current + ", "
                    + (structure == null ? "NULL" : "'" + structure + "'") + ", 1)");
            return id;
        }

        String value(String query) {
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> database.jdbc().sql(query).query(String.class)
                            .single()));
        }

        void sql(String statement) {
            CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> database.jdbc().sql(statement).update()));
        }
    }

    private Reply post(String path, String body, String token) throws Exception {
        HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .POST(HttpRequest.BodyPublishers.ofString(body)).header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token).build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    private Reply activate(UUID year, String body, String token) throws Exception {
        return post("/v1/org/academic-years/" + year + ":activate", body, token);
    }

    private Reply generate(UUID year, String token) throws Exception {
        return post("/v1/org/academic-years/" + year + ":generateTerms", "{}", token);
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
    void aPlannedYearBecomesTheOneActiveAndCurrentYearOfItsInstitution() throws Exception {
        School school = new School();
        UUID markedCurrent = school.year("2026-27", "planned", true, null);
        UUID next = school.year("2027-28", "planned", false, null);

        Reply activated = activate(next, "{\"etag\":\"\\\"1\\\"\"}", school.admin);
        assertThat(activated.status()).as(activated.text()).isEqualTo(200);
        assertThat(activated.body().path("status").asString()).isEqualTo("active");
        assertThat(activated.body().path("isCurrent").asBoolean()).isTrue();
        assertThat(school.value("SELECT is_current::text FROM org.academic_year WHERE id = '" + markedCurrent + "'"))
                .as("the other year lost the current mark").isEqualTo("false");
        assertThat(school.value("SELECT payload FROM org.outbox_event WHERE tenant_id = :tenantId AND event_type = "
                + "'org.academic_year.activate'")).contains(next.toString()).contains(school.institution.toString());
        assertThat(school.value("SELECT count(*)::text FROM org.outbox_event WHERE tenant_id = :tenantId AND "
                + "event_type = 'audit.recorded' AND payload LIKE '%\"activate\"%'")).isEqualTo("1");

        Reply second = activate(markedCurrent, "{}", school.admin);
        assertThat(second.status()).as("one active year per institution: " + second.text()).isEqualTo(409);
        assertThat(activate(next, "{}", school.admin).status()).as("already active").isEqualTo(409);
        assertThat(post("/v1/org/academic-years/" + next + ":close", "{}", school.admin).status()).isEqualTo(200);
        assertThat(activate(next, "{}", school.admin).status()).as("a closed year stays closed").isEqualTo(409);
        assertThat(activate(markedCurrent, "{}", school.admin).status()).as("once the other is closed")
                .isEqualTo(200);
    }

    @Test
    void activationChecksTheEtagTheScopeAndThePermission() throws Exception {
        School school = new School();
        UUID year = school.year("2027-28", "planned", false, null);

        assertThat(activate(year, "{\"etag\":\"\\\"5\\\"\"}", school.admin).status()).isEqualTo(412);
        assertThat(activate(year, "{}", token(school.tenant, school.institution.toString(), "org.academic_year.update"))
                .status()).isEqualTo(403);
        assertThat(activate(year, "{}", token(school.tenant, UUID.randomUUID().toString(), ACTIVATE)).status())
                .as("another school's administrator").isEqualTo(404);
        assertThat(activate(year, "{}", token(UUID.randomUUID(), "*", ACTIVATE)).status()).as("another tenant")
                .isEqualTo(404);
        assertThat(school.value("SELECT status FROM org.academic_year WHERE id = '" + year + "'"))
                .isEqualTo("planned");
    }

    @Test
    void termsAreGeneratedFromTheStructureOnceAndNestInTheirSemesters() throws Exception {
        School school = new School();
        UUID year = school.year("2027-28", "planned", false, "two_semesters_quarterly");

        Reply generated = generate(year, school.admin);
        assertThat(generated.status()).as(generated.text()).isEqualTo(200);
        assertThat(generated.names()).containsExactly("Semester 1", "Quarter 1", "Quarter 2", "Semester 2",
                "Quarter 3", "Quarter 4");
        JsonNode items = generated.body().path("items");
        assertThat(items.get(1).path("parentTermId").asString()).isEqualTo(items.get(0).path("id").asString());
        assertThat(items.get(4).path("parentTermId").asString()).isEqualTo(items.get(3).path("id").asString());
        assertThat(items.get(0).has("parentTermId")).isFalse();
        assertThat(items.get(0).path("startDate").asString()).isEqualTo("2027-04-01");
        assertThat(items.get(3).path("endDate").asString()).isEqualTo("2028-03-31");
        assertThat(items.get(5).path("endDate").asString()).isEqualTo("2028-03-31");
        String events = "SELECT count(*)::text FROM org.outbox_event WHERE tenant_id = :tenantId"
                + " AND event_type = 'org.academic_term.created'";
        assertThat(school.value(events)).isEqualTo("6");

        Reply again = generate(year, school.admin);
        assertThat(again.status()).isEqualTo(200);
        assertThat(again.body().path("items").findValuesAsString("id"))
                .as("a re-run answers the same terms").isEqualTo(items.findValuesAsString("id"));
        assertThat(school.value(events)).as("and writes nothing").isEqualTo("6");
    }

    @Test
    void runsAtTheSameTimeTakeTurnsAndWriteTheTermsOnce() throws Exception {
        School school = new School();
        UUID year = school.year("2027-28", "planned", false, "two_semesters_quarterly");
        int runs = 6;
        ExecutorService pool = Executors.newFixedThreadPool(runs);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Reply>> replies = new ArrayList<>();
            for (int i = 0; i < runs; i++) {
                replies.add(pool.submit(() -> {
                    start.await();
                    return generate(year, school.admin);
                }));
            }
            start.countDown();
            Set<List<String>> answers = new HashSet<>();
            for (Future<Reply> reply : replies) {
                Reply done = reply.get(60, TimeUnit.SECONDS);
                assertThat(done.status()).as("every run is the idempotent answer: " + done.text()).isEqualTo(200);
                answers.add(done.body().path("items").findValuesAsString("id"));
            }
            assertThat(answers).as("all runs answer the same terms").hasSize(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(school.value("SELECT count(*)::text FROM org.academic_term WHERE tenant_id = :tenantId"
                + " AND deleted_at IS NULL")).isEqualTo("6");
    }

    @Test
    void overlapsAYearWithoutStructureAndAClosedYearAreRefused() throws Exception {
        School school = new School();
        UUID year = school.year("2027-28", "active", true, "four_quarters");
        school.sql("INSERT INTO org.academic_term (tenant_id, academic_year_id, name, start_date, end_date, row_version)"
                + " VALUES (:tenantId, '" + year + "', 'Summer camp', '2027-05-01', '2027-05-31', 1)");
        Reply overlapping = generate(year, school.admin);
        assertThat(overlapping.status()).as(overlapping.text()).isEqualTo(409);
        assertThat(school.value("SELECT count(*)::text FROM org.academic_term WHERE tenant_id = :tenantId"))
                .as("nothing was written").isEqualTo("1");

        Reply none = generate(school.year("2028-29", "planned", false, "other"), school.admin);
        assertThat(none.status()).isEqualTo(422);
        assertThat(none.body().path("errors").get(0).path("field").asString()).isEqualTo("termStructure");
        assertThat(generate(school.year("2029-30", "closed", false, "two_semesters"), school.admin).status())
                .isEqualTo(409);
        UUID tooShort = UUID.randomUUID();
        school.sql("INSERT INTO org.academic_year (id, tenant_id, institution_id, name, start_date, end_date,"
                + " term_structure, row_version) VALUES ('" + tooShort + "', :tenantId, '" + school.institution
                + "', 'Short', '2030-01-01', '2030-01-02', 'four_quarters', 1)");
        assertThat(generate(tooShort, school.admin).status()).isEqualTo(422);
    }

    @Test
    void generatingNeedsItsPermissionInsideTheCallersInstitutions() throws Exception {
        School school = new School();
        UUID year = school.year("2027-28", "planned", false, "two_semesters");

        assertThat(generate(year, token(school.tenant, school.institution.toString(), "org.academic_term.create"))
                .status()).isEqualTo(403);
        assertThat(generate(year, token(school.tenant, UUID.randomUUID().toString(), GENERATE)).status())
                .isEqualTo(404);
        assertThat(generate(UUID.randomUUID(), school.admin).status()).isEqualTo(404);
        assertThat(generate(year, token(school.tenant, "*", GENERATE)).names())
                .as("a tenant-wide grant").containsExactly("Semester 1", "Semester 2");
    }
}
