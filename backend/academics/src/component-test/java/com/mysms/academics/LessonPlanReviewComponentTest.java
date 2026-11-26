package com.mysms.academics;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.authclient.testing.TestTokens;
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
 * The review of a lesson plan over HTTP against real PostgreSQL as the application role: a teacher submits a plan,
 * the head approves or rejects it, each decision is kept as a review record, and only an approved plan is published.
 * Each test works in a tenant of its own.
 */
@SpringBootTest(classes = AcademicsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LessonPlanReviewComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String PLANS = "/v1/academics/lesson-plans";
    private static final String READ = "academics.lesson_plan.read";
    private static final String APPROVE = "academics.lesson_plan.approve_lesson_plan";
    private static final String REJECT = "academics.lesson_plan.reject_lesson_plan";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(AcademicsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "academics", "timetable", "attendance", "assessment");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {
    }

    /** One school of one tenant: a class a teacher teaches, its administrator, the teacher and the head. */
    private final class School {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final UUID teaching = UUID.randomUUID();
        final String admin = token(UUID.randomUUID(), tenant, List.of("*"), "*");
        final String teacher = token(UUID.randomUUID(), tenant, List.of(institution.toString()), READ,
                "academics.lesson_plan.create", "academics.lesson_plan.submit_lesson_plan");
        final UUID headId = UUID.randomUUID();
        final String head = token(headId, tenant, List.of(institution.toString()), READ, APPROVE, REJECT,
                "academics.lesson_plan_approval.read", "academics.lesson_plan.publish_lesson_plan");

        School() {
            UUID grade = UUID.randomUUID();
            UUID section = UUID.randomUUID();
            UUID classSection = UUID.randomUUID();
            UUID subject = UUID.randomUUID();
            UUID classSubject = UUID.randomUUID();
            String start = "(:tenantId, '" + institution + "', ";
            sql("INSERT INTO academics.grade_level (tenant_id, institution_id, name, slug, id) VALUES " + start
                            + "'Grade 7', 'grade-7', '" + grade + "')",
                    "INSERT INTO academics.section (tenant_id, institution_id, name, code, id) VALUES " + start
                            + "'A', 'A', '" + section + "')",
                    "INSERT INTO academics.class_section (tenant_id, institution_id, academic_year_id, grade_level_id,"
                            + " section_id, id) VALUES " + start + "'" + UUID.randomUUID() + "', '" + grade + "', '"
                            + section + "', '" + classSection + "')",
                    "INSERT INTO academics.subject (tenant_id, institution_id, name, id) VALUES " + start
                            + "'Science', '" + subject + "')",
                    "INSERT INTO academics.class_subject (tenant_id, class_section_id, subject_id, id) VALUES "
                            + "(:tenantId, '" + classSection + "', '" + subject + "', '" + classSubject + "')",
                    "INSERT INTO academics.teaching_assignment (tenant_id, class_subject_id, staff_id, id) VALUES "
                            + "(:tenantId, '" + classSubject + "', '" + UUID.randomUUID() + "', '" + teaching + "')");
        }

        private void sql(String... statements) {
            CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> {
                        for (String statement : statements) {
                            database.jdbc().sql(statement).update();
                        }
                        return null;
                    }));
        }

        /** A plan the teacher wrote and submitted: it waits for review. */
        String submittedPlan() throws Exception {
            Reply created = call("POST", PLANS, teacher, "{\"teachingAssignmentId\":\"" + teaching
                    + "\",\"unitNo\":\"3\",\"unitName\":\"Forces\",\"title\":\"Friction\","
                    + "\"plannedDurationMinutes\":\"2700s\",\"description\":\"Why things stop\"}");
            assertThat(created.status()).as(created.text()).isEqualTo(201);
            String plan = created.body().path("id").asString();
            Reply submitted = call("POST", PLANS + "/" + plan + ":submit", teacher, "{}");
            assertThat(submitted.status()).as(submitted.text()).isEqualTo(200);
            assertThat(submitted.body().path("status").asString()).isEqualTo("pending");
            return plan;
        }

        JsonNode reviews(String plan) throws Exception {
            Reply list = call("GET", PLANS + "/" + plan + "/approvals?sort=createTime", head, null);
            assertThat(list.status()).as(list.text()).isEqualTo(200);
            return list.body().path("data");
        }
    }

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

    /** A write under If-Match; answers the status. */
    private int patchOrDelete(String method, String path, String token, String body, String etag) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", "Bearer " + token).header("If-Match", etag);
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private static String token(UUID user, UUID tenant, List<String> institutions, String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", institutions.contains("*") ? List.of("*") : List.of());
        claims.put("ins", institutions);
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    private List<String> events(UUID tenant, String plan) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).read(() -> database.jdbc().sql("SELECT event_type FROM "
                        + "academics.outbox_event WHERE tenant_id = :tenantId AND aggregate_id = :aggregate "
                        + "AND event_type <> 'audit.recorded' ORDER BY id")
                        .param("aggregate", UUID.fromString(plan)).query(String.class).list()));
    }

    @Test
    void theHeadApprovesASubmittedPlanTheDecisionIsKeptAndThePlanCanThenBePublished() throws Exception {
        School school = new School();
        String plan = school.submittedPlan();
        String approve = PLANS + "/" + plan + ":approve";

        assertThat(call("POST", approve, school.teacher, "{}").status()).as("a teacher cannot approve").isEqualTo(403);
        assertThat(call("POST", approve, school.head, "{\"etag\":\"\\\"99\\\"\"}").status()).isEqualTo(412);
        assertThat(call("POST", approve, school.head, "{\"reason\":\"" + "x".repeat(4001) + "\"}").status())
                .isEqualTo(422);
        assertThat(call("POST", PLANS + "/" + plan + ":publish", school.head, "{}").status())
                .as("not before it is approved").isEqualTo(409);
        assertThat(school.reviews(plan)).as("refused calls record nothing").isEmpty();

        Reply approved = call("POST", approve, school.head, "{\"reason\":\"  Clear and complete  \"}");
        assertThat(approved.status()).as(approved.text()).isEqualTo(200);
        assertThat(approved.body().path("status").asString()).isEqualTo("approved");
        JsonNode review = school.reviews(plan);
        assertThat(review).hasSize(1);
        assertThat(review.get(0).path("decision").asString()).isEqualTo("approved");
        assertThat(review.get(0).path("comments").asString()).isEqualTo("Clear and complete");
        assertThat(review.get(0).path("decidedByUserId").asString()).isEqualTo(school.headId.toString());
        assertThat(review.get(0).path("decidedAt").asString()).isNotEmpty();

        assertThat(call("POST", approve, school.head, "{}").status()).as("nothing left to review").isEqualTo(409);
        assertThat(school.reviews(plan)).hasSize(1);
        Reply published = call("POST", PLANS + "/" + plan + ":publish", school.head, "{}");
        assertThat(published.status()).as(published.text()).isEqualTo(200);
        assertThat(published.body().path("isPublished").asBoolean()).isTrue();
        assertThat(events(school.tenant, plan)).containsExactly("academics.lesson_plan.created",
                "academics.lesson_plan.submit", "academics.lesson_plan.approve", "academics.lesson_plan.publish");
    }

    @Test
    void aRejectionIsKeptWithItsReasonAndAReviewNeedsThePlanInsideTheCallersInstitutionsAndTenant() throws Exception {
        School school = new School();
        String plan = school.submittedPlan();
        String approve = PLANS + "/" + plan + ":approve";

        String elsewhere = token(UUID.randomUUID(), school.tenant, List.of(UUID.randomUUID().toString()), "*");
        assertThat(call("POST", approve, elsewhere, "{}").status()).as("another institution").isEqualTo(404);
        assertThat(call("POST", approve, token(UUID.randomUUID(), UUID.randomUUID(), List.of("*"), "*"), "{}")
                .status()).as("another tenant").isEqualTo(404);

        Reply rejected = call("POST", PLANS + "/" + plan + ":reject", school.head, "{\"reason\":\"Add the practical\"}");
        assertThat(rejected.status()).as(rejected.text()).isEqualTo(200);
        assertThat(rejected.body().path("status").asString()).isEqualTo("rejected");
        JsonNode review = school.reviews(plan);
        assertThat(review).hasSize(1);
        assertThat(review.get(0).path("decision").asString()).isEqualTo("rejected");
        assertThat(review.get(0).path("comments").asString()).isEqualTo("Add the practical");
        assertThat(review.get(0).path("decidedByUserId").asString()).isEqualTo(school.headId.toString());
        assertThat(call("POST", approve, school.head, "{}").status()).as("a rejected plan is not approved")
                .isEqualTo(409);
        assertThat(patchOrDelete("DELETE", PLANS + "/" + plan, school.admin, null,
                rejected.body().path("etag").asString())).as("a reviewed plan is archived, not deleted").isEqualTo(409);

        // the review is seen where its plan is, and nobody rewrites or removes it by hand
        String reviews = PLANS + "/" + plan + "/approvals";
        String one = reviews + "/" + review.get(0).path("id").asString();
        assertThat(call("GET", reviews, elsewhere, null).status()).as("another school's reviews").isEqualTo(404);
        assertThat(call("GET", one, elsewhere, null).status()).isEqualTo(404);
        assertThat(call("GET", PLANS + "/" + UUID.randomUUID() + "/approvals/" + review.get(0).path("id").asString(),
                school.admin, null).status()).as("under another plan").isEqualTo(404);
        assertThat(call("GET", one, school.head, null).status()).isEqualTo(200);
        assertThat(call("POST", reviews, school.admin, "{\"lessonPlanId\":\"" + plan
                + "\",\"decision\":\"approved\",\"comments\":\"forged\"}").status()).isEqualTo(409);
        assertThat(patchOrDelete("PATCH", one, school.admin, "{\"decision\":\"approved\"}",
                review.get(0).path("etag").asString())).isEqualTo(409);
        assertThat(patchOrDelete("DELETE", one, school.admin, null, review.get(0).path("etag").asString()))
                .isEqualTo(409);
        assertThat(school.reviews(plan).get(0).path("decision").asString()).isEqualTo("rejected");

        String other = school.submittedPlan();
        Reply withoutReason = call("POST", PLANS + "/" + other + ":approve", school.head, "{}");
        assertThat(withoutReason.status()).as(withoutReason.text()).isEqualTo(200);
        assertThat(school.reviews(other).get(0).path("comments").asString()).as("no reason given").isEmpty();
    }
}
