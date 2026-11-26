package com.mysms.students.careers;

import com.mysms.students.StudentsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.students.careers.config.CareersDatabaseConfig;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
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
 * The careers board's public reads and hiring steps against real PostgreSQL with row-level security: guests see
 * open openings only, applications move under their guard and etag, and e-mail needs SMTP and iam before anything is
 * recorded. An application's applicant is the caller, and an applicant reaches only their own applications.
 */
@SpringBootTest(classes = StudentsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CareersHiringComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID POSTER = UUID.randomUUID();
    private static final String RECRUITER = token(TENANT, List.of("careers.*"));
    private static final String MEMBER = token(TENANT, List.of("careers.job_opening.report_job_opening"));
    private static final String NOBODY = token(TENANT, List.of());

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(CareersDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "careers", "sis", "admission");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private static String token(UUID tenant, List<String> permissions) {
        return token(UUID.randomUUID(), tenant, permissions);
    }

    private static String token(UUID user, UUID tenant, List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", permissions);
        claims.put("tperms", permissions);
        claims.put("ins", List.of("*"));
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    private record Reply(int status, JsonNode body) {
    }

    private Reply call(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()));
    }

    private UUID opening(String status) throws Exception {
        Reply created = call("POST", "/v1/careers/job-openings", RECRUITER, """
                {"title":"Physics teacher","location":"Pune","countryId":"%s","employmentType":"full_time",
                 "description":"Teach physics","companyName":"Green Valley School","companyWebsite":"https://example.org",
                 "companyEmail":"jobs@example.org","companyLogoFileId":"%s","postedByUserId":"%s"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), POSTER));
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        UUID id = UUID.fromString(created.body().path("id").asString());
        if ("closed".equals(status)) {
            assertThat(call("POST", "/v1/careers/job-openings/" + id + ":close", RECRUITER, "{}").status())
                    .isEqualTo(200);
        }
        return id;
    }

    private int audits(UUID id, String step) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(TENANT, null);
        return CallContextHolder.callAs(context, () -> TestTransactions.of(database).read(() -> database.jdbc()
                .sql("SELECT count(*) FROM careers.outbox_event WHERE tenant_id = :tenantId AND aggregate_id = :id"
                        + " AND event_type = :type")
                .param("id", id).param("type", "careers.job_opening." + step).query(Integer.class).single()));
    }

    @Test
    void thePublicSeesOpenOpeningsOnlyInTheGuestProjection() throws Exception {
        UUID open = opening("open");
        UUID closed = opening("closed");
        assertThat(call("GET", "/v1/careers/job-openings/" + open, RECRUITER, null).body()
                .path("postedByUserId").asString()).isEqualTo(POSTER.toString());

        Reply seen = call("GET", "/v1/careers/job-openings/" + open, NOBODY, null);
        assertThat(seen.status()).isEqualTo(200);
        assertThat(seen.body().has("postedByUserId")).isFalse();
        assertThat(seen.body().has("createdBy")).isFalse();
        assertThat(call("GET", "/v1/careers/job-openings/" + closed, NOBODY, null).status()).isEqualTo(404);
        JsonNode listed = call("GET", "/v1/careers/job-openings?size=100", NOBODY, null).body().path("data");
        assertThat(listed.findValuesAsString("id")).contains(open.toString()).doesNotContain(closed.toString());
        assertThat(listed.get(0).has("postedByUserId")).isFalse();
        assertThat(call("GET", "/v1/careers/job-openings?size=100", RECRUITER, null).body().path("data")
                .findValuesAsString("id")).contains(open.toString(), closed.toString());

        String byPoster = "/v1/careers/job-openings?filter=" + java.net.URLEncoder.encode("postedByUserId = \""
                + POSTER + "\"", java.nio.charset.StandardCharsets.UTF_8);
        assertThat(call("GET", byPoster, NOBODY, null).status()).as("the public filters only on what it sees")
                .isEqualTo(400);
        assertThat(call("GET", byPoster, RECRUITER, null).status()).isEqualTo(200);
        assertThat(call("GET", "/v1/careers/job-openings", null, null).status())
                .as("a guest who names no school").isEqualTo(404);
        assertThat(call("GET", "/v1/careers/job-openings/" + open, token(UUID.randomUUID(), List.of()), null)
                .status()).as("another tenant").isEqualTo(404);
    }

    @Test
    void aReportKeepsItsReasonAndNeedsAVisibleOpening() throws Exception {
        UUID open = opening("open");
        UUID closed = opening("closed");
        assertThat(call("POST", "/v1/careers/job-openings/" + open + ":report", MEMBER, "{\"reason\":\"Spam\"}")
                .status()).isEqualTo(204);
        assertThat(audits(open, "report")).isEqualTo(1);
        assertThat(call("POST", "/v1/careers/job-openings/" + closed + ":report", MEMBER, "{\"reason\":\"Spam\"}")
                .status()).as("closed openings are not public").isEqualTo(404);
        assertThat(call("POST", "/v1/careers/job-openings/" + open + ":report", MEMBER, "{\"reason\":\"  \"}")
                .status()).isEqualTo(422);
        assertThat(call("POST", "/v1/careers/job-openings/" + open + ":report", NOBODY, "{\"reason\":\"Spam\"}")
                .status()).isEqualTo(403);
        assertThat(audits(open, "report")).isEqualTo(1);
    }

    @Test
    void anApplicationMovesThroughItsHiringSteps() throws Exception {
        UUID open = opening("open");
        String applications = "/v1/careers/job-openings/" + open + "/applications";
        Reply applied = call("POST", applications, RECRUITER, """
                {"resumeFileId":"%s"}""".formatted(UUID.randomUUID()));
        assertThat(applied.status()).isEqualTo(201);
        String item = applications + "/" + applied.body().path("id").asString();
        String stale = applied.body().path("etag").asString();

        Reply shortlisted = call("POST", item + ":setStatus", RECRUITER, "{\"status\":\"shortlisted\"}");
        assertThat(shortlisted.status()).isEqualTo(200);
        assertThat(shortlisted.body().path("status").asString()).isEqualTo("shortlisted");
        assertThat(call("POST", item + ":setStatus", RECRUITER,
                "{\"status\":\"hired\",\"etag\":" + JSON.writeValueAsString(stale) + "}").status())
                .as("a stale etag").isEqualTo(412);
        assertThat(call("POST", item + ":setStatus", RECRUITER, "{\"status\":\"submitted\"}").status())
                .as("no step back").isEqualTo(409);
        assertThat(call("POST", item + ":setStatus", NOBODY, "{\"status\":\"hired\"}").status()).isEqualTo(403);
        assertThat(call("POST", applications + "/" + UUID.randomUUID() + ":setStatus", RECRUITER,
                "{\"status\":\"hired\"}").status()).isEqualTo(404);
        Reply hired = call("POST", item + ":setStatus", RECRUITER, "{\"status\":\"hired\"}");
        assertThat(hired.body().path("status").asString()).isEqualTo("hired");
        assertThat(call("POST", item + ":setStatus", RECRUITER, "{\"status\":\"rejected\"}").status())
                .as("a decided application does not move").isEqualTo(409);
    }

    @Test
    void writingToAnApplicantNeedsMailAndIamBeforeAnythingIsRecorded() throws Exception {
        UUID open = opening("open");
        Reply applied = call("POST", "/v1/careers/job-openings/" + open + "/applications", RECRUITER, """
                {"resumeFileId":"%s"}""".formatted(UUID.randomUUID()));
        String path = "/v1/careers/job-applications/" + applied.body().path("id").asString() + ":email";
        Reply refused = call("POST", path, RECRUITER, "{\"subject\":\"Interview\",\"message\":\"Monday 10:00\"}");
        assertThat(refused.status()).isEqualTo(503);
        assertThat(refused.body().path("code").asString()).isEqualTo("NOT_CONFIGURED");
        assertThat(call("POST", path, RECRUITER, "{\"subject\":\"Two\\nlines\",\"message\":\"x\"}").status())
                .isEqualTo(422);
        assertThat(call("POST", path, NOBODY, "{\"subject\":\"Interview\",\"message\":\"x\"}").status())
                .isEqualTo(403);
    }

    @Test
    void anApplicantIsTheCallerAndReachesOnlyTheirOwnApplications() throws Exception {
        UUID open = opening("open");
        String applications = "/v1/careers/job-openings/" + open + "/applications";
        UUID studentA = UUID.randomUUID();
        UUID studentB = UUID.randomUUID();
        List<String> applying = List.of("careers.job_application.create", "careers.job_application.read",
                "careers.job_application.update", "careers.job_application.delete");
        String a = token(studentA, TENANT, applying);
        String b = token(studentB, TENANT, applying);

        Reply ofB = call("POST", applications, b, "{\"resumeFileId\":\"%s\"}".formatted(UUID.randomUUID()));
        assertThat(ofB.status()).isEqualTo(201);
        Reply forged = call("POST", applications, a, "{\"applicantUserId\":\"%s\",\"resumeFileId\":\"%s\"}"
                .formatted(studentB, UUID.randomUUID()));
        assertThat(forged.status()).as(forged.body().toString()).isEqualTo(201);
        assertThat(forged.body().path("applicantUserId").asString()).as("the applicant comes from the token")
                .isEqualTo(studentA.toString());
        String itemOfA = applications + "/" + forged.body().path("id").asString();
        String itemOfB = applications + "/" + ofB.body().path("id").asString();

        assertThat(call("GET", itemOfB, a, null).status()).as("another applicant's application").isEqualTo(404);
        assertThat(call("GET", itemOfA, a, null).body().path("applicantUserId").asString())
                .isEqualTo(studentA.toString());
        assertThat(call("GET", applications + "?size=100", a, null).body().path("data").findValuesAsString("id"))
                .containsExactly(forged.body().path("id").asString());
        String etagOfB = ofB.body().path("etag").asString();
        assertThat(callWith("PATCH", itemOfB, a, "{\"resumeFileId\":\"%s\"}".formatted(UUID.randomUUID()), etagOfB)
                .status()).as("nor change it").isEqualTo(404);
        assertThat(callWith("DELETE", itemOfB, a, null, etagOfB).status()).as("nor withdraw it").isEqualTo(404);
        Reply moved = callWith("PATCH", itemOfA, a,
                "{\"applicantUserId\":\"%s\"}".formatted(studentB), forged.body().path("etag").asString());
        assertThat(moved.body().path("applicantUserId").asString()).as("the applicant is not writable: ignored")
                .isNotEqualTo(studentB.toString());

        assertThat(call("GET", applications + "?size=100", RECRUITER, null).body().path("data")
                .findValuesAsString("id")).as("recruiting staff reach every application")
                .contains(forged.body().path("id").asString(), ofB.body().path("id").asString());
        assertThat(call("GET", itemOfB, RECRUITER, null).body().path("applicantUserId").asString())
                .isEqualTo(studentB.toString());
    }

    private Reply callWith(String method, String path, String token, String body, String ifMatch) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", "Bearer " + token).header("If-Match", ifMatch);
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()));
    }
}
