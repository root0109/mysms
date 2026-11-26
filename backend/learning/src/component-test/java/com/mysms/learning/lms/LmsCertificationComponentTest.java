package com.mysms.learning.lms;

import com.mysms.learning.LearningApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
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
 * Certification over HTTP against real PostgreSQL as the application role: generating a cohort's certificates,
 * verifying and downloading one, driving a booking into its evaluation, issuing the certificate a passed evaluation
 * earns, awarding badges, and linking an evaluator's calendar. Each test works in a tenant of its own; what needs
 * another service (iam names, platform files) answers 503 here, and the calendar provider is configured per test.
 */
@SpringBootTest(classes = LearningApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LmsCertificationComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(LmsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "lms");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.lms.calendar.google.client-id", () -> "calendar-client-1");
    }

    private record Reply(int status, JsonNode body, String text) {
        String code() {
            return body.path("code").asString();
        }
    }

    /** One certifying cohort of one tenant, and helpers that put rows in place as the tenant. */
    private final class Program {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final UUID cohort = UUID.randomUUID();
        final UUID course = UUID.randomUUID();
        final String admin = token(tenant, UUID.randomUUID(), List.of("*"), List.of("*"), "*");

        Program() {
            sql("INSERT INTO lms.course (id, tenant_id, institution_id, title, row_version) "
                    + "VALUES (:a, :tenantId, :b, 'Maths', 1)", course, institution);
            sql("INSERT INTO lms.cohort (id, tenant_id, institution_id, title, start_date, end_date, timezone, "
                    + "is_published, lifecycle_status, certification_enabled, row_version) VALUES (:a, :tenantId, "
                    + ":b, 'Maths 2026', DATE '2026-01-10', DATE '2026-06-20', 'Asia/Kolkata', true, 'active', "
                    + "true, 1)", cohort, institution);
        }

        UUID student() {
            UUID user = UUID.randomUUID();
            sql("INSERT INTO lms.cohort_enrollment (id, tenant_id, cohort_id, user_id, is_active, row_version) "
                    + "VALUES (:a, :tenantId, :b, :c, true, 1)", UUID.randomUUID(), cohort, user);
            return user;
        }

        UUID booking(UUID student, UUID evaluatorUser, String status) {
            UUID booking = UUID.randomUUID();
            sql("INSERT INTO lms.certificate_request (id, tenant_id, course_id, user_id, evaluator_user_id, "
                    + "request_date, start_time, end_time, status, row_version) VALUES (:a, :tenantId, :b, :c, :d, "
                    + "DATE '2026-03-01', TIME '09:00', TIME '10:00', '" + status + "', 1)", booking, course, student,
                    evaluatorUser);
            return booking;
        }

        void sql(String statement, Object... values) {
            CallContext context = CallContext.anonymous(TRACE).withIdentity(tenant, null);
            CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(() -> {
                var spec = database.jdbc().sql(statement);
                String[] names = {"a", "b", "c", "d"};
                for (int at = 0; at < values.length; at++) {
                    spec = spec.param(names[at], values[at]);
                }
                return spec.update();
            }));
        }

        int count(String where) {
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> database.jdbc()
                            .sql("SELECT count(*) FROM " + where).query(Integer.class).single()));
        }
    }

    // ---- cohort certificates

    @Test
    void generatingCohortCertificatesCountsIssuedAndSkipped() throws Exception {
        Program school = new Program();
        String generate = "/v1/lms/cohorts/" + school.cohort + ":generateCertificates";
        UUID first = school.student();
        UUID second = school.student();

        school.sql("UPDATE lms.cohort SET certification_enabled = false WHERE tenant_id = :tenantId AND id = :a",
                school.cohort);
        assertThat(call("POST", generate, school.admin, "{\"userIds\":[\"" + first
                + "\"],\"issueDate\":\"2026-06-21\",\"templateCode\":\"default\"}").code())
                .isEqualTo("CERTIFICATION_DISABLED");
        school.sql("UPDATE lms.cohort SET certification_enabled = true WHERE tenant_id = :tenantId AND id = :a",
                school.cohort);
        assertThat(call("POST", generate, school.admin, "{\"userIds\":[\"" + UUID.randomUUID()
                + "\"],\"issueDate\":\"2026-06-21\",\"templateCode\":\"default\"}").status())
                .as("every user must be an active student").isEqualTo(422);

        Reply issued = call("POST", generate, school.admin, "{\"userIds\":[\"" + first + "\",\"" + second
                + "\"],\"issueDate\":\"2026-06-21\",\"templateCode\":\"default\",\"publish\":true}");
        assertThat(issued.status()).as(issued.text()).isEqualTo(202);
        assertThat(issued.body().path("result").path("issued").asInt()).isEqualTo(2);
        assertThat(issued.body().path("result").path("skipped").asInt()).isEqualTo(0);
        assertThat(school.count("lms.certificate WHERE cohort_id IS NOT NULL AND is_published")).isEqualTo(2);

        Reply again = call("POST", generate, school.admin, "{\"userIds\":[\"" + first
                + "\"],\"issueDate\":\"2026-06-21\",\"templateCode\":\"default\"}");
        assertThat(again.body().path("result").path("issued").asInt()).isEqualTo(0);
        assertThat(again.body().path("result").path("skipped").asInt()).isEqualTo(1);
    }

    @Test
    void verifyingTellsOnlyWhetherAPublishedCertificateIsCurrent() throws Exception {
        Program school = new Program();
        UUID student = school.student();
        call("POST", "/v1/lms/cohorts/" + school.cohort + ":generateCertificates", school.admin,
                "{\"userIds\":[\"" + student + "\"],\"issueDate\":\"2026-06-21\",\"templateCode\":\"default\","
                        + "\"publish\":true}");
        String certificate = certificateOf(school, student);

        Reply valid = call("GET", "/v1/lms/certificates/" + certificate + ":verify", school.admin, null);
        assertThat(valid.status()).as(valid.text()).isEqualTo(200);
        assertThat(valid.body().path("valid").asBoolean()).isTrue();
        assertThat(valid.body().path("title").asString()).isEqualTo("Maths 2026");

        school.sql("UPDATE lms.certificate SET expiry_date = DATE '2026-07-01' WHERE tenant_id = :tenantId");
        assertThat(call("GET", "/v1/lms/certificates/" + certificate + ":verify", school.admin, null).body()
                .path("valid").asBoolean()).as("an expired certificate is not current").isFalse();

        school.sql("UPDATE lms.certificate SET expiry_date = NULL, is_published = false WHERE tenant_id = :tenantId");
        assertThat(call("GET", "/v1/lms/certificates/" + certificate + ":verify", school.admin, null).body()
                .path("valid").asBoolean()).as("an unpublished certificate verifies as invalid").isFalse();
        assertThat(call("GET", "/v1/lms/certificates/" + UUID.randomUUID() + ":verify", school.admin, null).body()
                .path("valid").asBoolean()).isFalse();
    }

    @Test
    void downloadingRendersThePdfOnceIamAndPlatformAnswer() throws Exception {
        Program school = new Program();
        UUID student = school.student();
        call("POST", "/v1/lms/cohorts/" + school.cohort + ":generateCertificates", school.admin,
                "{\"userIds\":[\"" + student + "\"],\"issueDate\":\"2026-06-21\",\"templateCode\":\"default\"}");
        String certificate = certificateOf(school, student);
        assertThat(call("GET", "/v1/lms/certificates/" + UUID.randomUUID() + ":download", school.admin, null)
                .status()).isEqualTo(404);
        assertThat(call("GET", "/v1/lms/certificates/" + certificate + ":download", school.admin, null).status())
                .as("the student's name comes from iam, which is not running here").isEqualTo(503);
        // another member who may read and download certificates but neither holds this one nor issues them
        String member = token(school.tenant, UUID.randomUUID(), List.of(school.institution.toString()), List.of(),
                "lms.certificate.read", "lms.certificate.download_certificate");
        assertThat(call("GET", "/v1/lms/certificates/" + certificate + ":download", member, null).status())
                .as("not the holder's, and no staff of its institution: absent").isEqualTo(404);
    }

    // ---- bookings and evaluations

    @Test
    void startingAnEvaluationCompletesTheBookingOnce() throws Exception {
        Program school = new Program();
        UUID student = school.student();
        UUID evaluatorUser = UUID.randomUUID();
        UUID booking = school.booking(student, evaluatorUser, "upcoming");
        String start = "/v1/lms/certificate-requests/" + booking + ":startEvaluation";
        String evaluator = token(school.tenant, evaluatorUser, List.of(school.institution.toString()), List.of(),
                "lms.certificate_request.start_evaluation", "lms.certificate_request.read");
        String bystander = token(school.tenant, UUID.randomUUID(), List.of(school.institution.toString()), List.of(),
                "lms.certificate_request.start_evaluation", "lms.certificate_request.read");

        assertThat(call("POST", start, bystander, "{}").status()).as("only the booking's evaluator").isEqualTo(403);
        Reply opened = call("POST", start, evaluator, "{}");
        assertThat(opened.status()).as(opened.text()).isEqualTo(200);
        assertThat(opened.body().path("status").asString()).isEqualTo("in_progress");
        assertThat(opened.body().path("certificateRequestId").asString()).isEqualTo(booking.toString());
        assertThat(school.count("lms.certificate_request WHERE status = 'completed'")).isEqualTo(1);

        assertThat(call("POST", start, evaluator, "{}").status()).as("a completed booking is not evaluated again")
                .isEqualTo(409);
        UUID second = school.booking(school.student(), evaluatorUser, "upcoming");
        school.sql("UPDATE lms.certificate_evaluation SET certificate_request_id = :a WHERE tenant_id = :tenantId",
                second);
        assertThat(call("POST", "/v1/lms/certificate-requests/" + second + ":startEvaluation", evaluator, "{}")
                .code()).isEqualTo("EVALUATION_EXISTS");
    }

    @Test
    void aPassedEvaluationEarnsTheCertificateOnce() throws Exception {
        Program school = new Program();
        UUID student = school.student();
        UUID evaluatorUser = UUID.randomUUID();
        UUID evaluation = UUID.randomUUID();
        school.sql("INSERT INTO lms.certificate_evaluation (id, tenant_id, course_id, user_id, evaluator_user_id, "
                + "evaluation_date, start_time, status, row_version) VALUES (:a, :tenantId, :b, :c, :d, "
                + "DATE '2026-03-01', TIME '09:00', 'in_progress', 1)", evaluation, school.course, student,
                evaluatorUser);
        String issue = "/v1/lms/certificate-evaluations/" + evaluation + ":issueCertificate";
        String evaluator = token(school.tenant, evaluatorUser, List.of(school.institution.toString()), List.of(),
                "lms.certificate_evaluation.issue_certificate_from_evaluation", "lms.certificate_evaluation.read");

        String fullIssue = "{\"issueDate\":\"2026-03-02\",\"templateCode\":\"default\"}";
        assertThat(call("POST", issue, evaluator, fullIssue).status()).as("only a passed evaluation").isEqualTo(409);
        school.sql("UPDATE lms.certificate_evaluation SET status = 'pass' WHERE tenant_id = :tenantId AND id = :a",
                evaluation);
        assertThat(call("POST", issue, evaluator, "{\"issueDate\":\"2026-03-02\",\"expiryDate\":\"2026-03-01\","
                + "\"templateCode\":\"default\"}").status()).as("an expiry may not precede the issue date")
                .isEqualTo(422);

        Reply issued = call("POST", issue, evaluator, "{\"issueDate\":\"2026-03-02\",\"templateCode\":\"default\"}");
        assertThat(issued.status()).as(issued.text()).isEqualTo(200);
        assertThat(issued.body().path("userId").asString()).isEqualTo(student.toString());
        assertThat(issued.body().path("certificateNumber").asString()).isNotEmpty();
        assertThat(call("POST", issue, evaluator, fullIssue).code()).isEqualTo("CERTIFICATE_EXISTS");
    }

    // ---- badges

    @Test
    void aBadgeIsAwardedTodayAndOnlyOnceWhenItSaysSo() throws Exception {
        Program school = new Program();
        UUID badge = UUID.randomUUID();
        UUID student = school.student();
        school.sql("INSERT INTO lms.badge (id, tenant_id, title, description, image_file_id, target_entity, "
                + "trigger_event, condition_expr, user_field, grant_only_once, is_enabled, row_version) "
                + "VALUES (:a, :tenantId, 'Finisher', 'Completed every module.', :b, 'lms.enrollment', 'manual', "
                + "'true', 'user_id', true, false, 1)", badge, UUID.randomUUID());
        String award = "/v1/lms/badges/" + badge + ":award";
        String body = "{\"userId\":\"" + student + "\"}";

        assertThat(call("POST", award, school.admin, body).code()).isEqualTo("BADGE_DISABLED");
        school.sql("UPDATE lms.badge SET is_enabled = true WHERE tenant_id = :tenantId AND id = :a", badge);
        Reply awarded = call("POST", award, school.admin, body);
        assertThat(awarded.status()).as(awarded.text()).isEqualTo(200);
        assertThat(awarded.body().path("badgeId").asString()).isEqualTo(badge.toString());
        assertThat(awarded.body().path("issuedOn").asString()).isNotEmpty();
        assertThat(call("POST", award, school.admin, body).code()).isEqualTo("ALREADY_AWARDED");
        school.sql("UPDATE lms.badge SET grant_only_once = false WHERE tenant_id = :tenantId AND id = :a", badge);
        assertThat(call("POST", award, school.admin, body).status()).isEqualTo(200);
    }

    // ---- the evaluator's calendar

    @Test
    void linkingACalendarStartsOAuthAndUnlinkingClearsIt() throws Exception {
        Program school = new Program();
        UUID evaluator = UUID.randomUUID();
        UUID evaluatorUser = UUID.randomUUID();
        school.sql("INSERT INTO lms.evaluator (id, tenant_id, user_id, row_version) VALUES (:a, :tenantId, :b, 1)",
                evaluator, evaluatorUser);
        String connect = "/v1/lms/evaluators/" + evaluator + ":connectCalendar";
        String own = token(school.tenant, evaluatorUser, List.of("*"), List.of("*"), "*");

        assertThat(call("POST", connect, own, "{\"redirectUri\":\"not a url\"}").status()).isEqualTo(422);
        assertThat(call("POST", connect, school.admin, "{\"redirectUri\":\"https://app.example/calendar/done\"}")
                .status()).as("only the evaluator links their own calendar").isEqualTo(403);
        Reply link = call("POST", connect, own, "{\"redirectUri\":\"https://app.example/calendar/done\"}");
        assertThat(link.status()).as(link.text()).isEqualTo(200);
        assertThat(link.body().path("url").asString())
                .startsWith("https://accounts.google.com/o/oauth2/v2/auth?response_type=code")
                .contains("client_id=calendar-client-1")
                .contains("redirect_uri=https%3A%2F%2Fapp.example%2Fcalendar%2Fdone").contains("state=");
        assertThat(link.body().path("expiresAt").asString()).isNotEmpty();
        assertThat(school.count("lms.evaluator WHERE calendar_provider = 'google' AND calendar_state IS NOT NULL"))
                .isEqualTo(1);

        Reply unlinked = call("POST", "/v1/lms/evaluators/" + evaluator + ":disconnectCalendar", school.admin, "{}");
        assertThat(unlinked.status()).as(unlinked.text()).isEqualTo(200);
        assertThat(school.count("lms.evaluator WHERE calendar_provider IS NULL")).isEqualTo(1);
        assertThat(call("POST", "/v1/lms/evaluators/" + evaluator + ":disconnectCalendar", school.admin, "{}")
                .status()).as("nothing is linked any more").isEqualTo(409);
    }

    private String certificateOf(Program school, UUID student) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(school.tenant, null),
                () -> TestTransactions.of(database).write(() -> database.jdbc()
                        .sql("SELECT id FROM lms.certificate WHERE user_id = :user").param("user", student)
                        .query(UUID.class).single())).toString();
    }

    // ---- helpers

    private Reply call(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        request.header("Authorization", "Bearer " + token);
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    /** A token of the account with the permissions in the institutions (and tenant-wide where named). */
    private static String token(UUID tenant, UUID user, List<String> institutions, List<String> tenantWide,
            String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", tenantWide);
        claims.put("ins", institutions);
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }
}
