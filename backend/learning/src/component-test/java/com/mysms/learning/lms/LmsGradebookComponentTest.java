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
 * The cohort's grading reads and steps over HTTP against real PostgreSQL, with hand-worked numbers: per-item
 * performance, student progress, publishing final grades, conditional release and the timetable. The gradebook grid
 * itself names students through iam, so here it answers 503 after its own checks; its arithmetic is
 * GradeCalculator's, unit-tested with the same numbers.
 */
@SpringBootTest(classes = LearningApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LmsGradebookComponentTest {

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
    }

    private record Reply(int status, JsonNode body, String text) {
    }

    /**
     * One graded cohort: homework weighs 60, quizzes 40. Asha has 8/10 homework (80%) and 15/20 quiz (75%), so her
     * total is (60×80 + 40×75) / 100 = 78.00. Ravi is excused from the homework and has 10/20 quiz (50%), so only
     * the quiz category is in play and his total is 50.00.
     */
    private final class GradedCohort {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final UUID cohort = UUID.randomUUID();
        final UUID teacherUser = UUID.randomUUID();
        final UUID asha = UUID.randomUUID();
        final UUID ravi = UUID.randomUUID();
        final UUID homework = UUID.randomUUID();
        final UUID quiz = UUID.randomUUID();
        final String admin = token(tenant, UUID.randomUUID(), List.of("*"), List.of("*"), "*");
        final String teacher;

        GradedCohort() {
            sql("INSERT INTO lms.cohort (id, tenant_id, institution_id, title, start_date, end_date, timezone, "
                    + "is_published, lifecycle_status, row_version) VALUES (:a, :tenantId, :b, 'Maths 2026', "
                    + "DATE '2026-01-10', DATE '2026-06-20', 'Asia/Kolkata', true, 'active', 1)", cohort, institution);
            sql("INSERT INTO lms.cohort_teacher (id, tenant_id, cohort_id, user_id, is_active, row_version) "
                    + "VALUES (:a, :tenantId, :b, :c, true, 1)", UUID.randomUUID(), cohort, teacherUser);
            teacher = token(tenant, teacherUser, List.of(institution.toString()), List.of(), "*");
            for (UUID student : List.of(asha, ravi)) {
                sql("INSERT INTO lms.cohort_enrollment (id, tenant_id, cohort_id, user_id, is_active, row_version) "
                        + "VALUES (:a, :tenantId, :b, :c, true, 1)", UUID.randomUUID(), cohort, student);
            }
            UUID homeworkCategory = UUID.randomUUID();
            UUID quizCategory = UUID.randomUUID();
            sql("INSERT INTO lms.grading_category (id, tenant_id, cohort_id, name, weight_percent, row_version) "
                    + "VALUES (:a, :tenantId, :b, 'Homework', 60, 1)", homeworkCategory, cohort);
            sql("INSERT INTO lms.grading_category (id, tenant_id, cohort_id, name, weight_percent, row_version) "
                    + "VALUES (:a, :tenantId, :b, 'Quizzes', 40, 1)", quizCategory, cohort);
            UUID homeworkSource = UUID.randomUUID();
            UUID quizSource = UUID.randomUUID();
            sql("INSERT INTO lms.assignment (id, tenant_id, title, question, submission_type, row_version) "
                    + "VALUES (:a, :tenantId, 'Homework 1', 'Solve the sheet.', 'document', 1)", homeworkSource);
            sql("INSERT INTO lms.quiz (id, tenant_id, title, passing_percentage, total_marks, row_version) "
                    + "VALUES (:a, :tenantId, 'Quiz 1', 40, 20, 1)", quizSource);
            sql("INSERT INTO lms.content_item (id, tenant_id, cohort_id, item_kind, assignment_id, title, "
                    + "is_graded, points, grading_category_id, row_version) VALUES (:a, :tenantId, :b, "
                    + "'assignment', :c, 'Homework 1', true, 10, :d, 1)", homework, cohort, homeworkSource,
                    homeworkCategory);
            sql("INSERT INTO lms.content_item (id, tenant_id, cohort_id, item_kind, quiz_id, title, is_graded, "
                    + "points, grading_category_id, row_version) VALUES (:a, :tenantId, :b, 'quiz', :c, 'Quiz 1', "
                    + "true, 20, :d, 1)", quiz, cohort, quizSource, quizCategory);
            entry(homework, asha, "8", "graded", true);
            entry(quiz, asha, "15", "graded", false);
            entry(homework, ravi, null, "excused", true);
            entry(quiz, ravi, "10", "graded", true);
        }

        void entry(UUID item, UUID student, String points, String status, boolean published) {
            sql("INSERT INTO lms.gradebook_entry (id, tenant_id, cohort_id, content_item_id, user_id, points, "
                    + "status, is_published, row_version) VALUES (:a, :tenantId, :b, :c, :d, "
                    + (points == null ? "NULL" : points) + ", '" + status + "', " + published + ", 1)",
                    UUID.randomUUID(), cohort, item, student);
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
    }

    @Test
    void performanceAveragesTheGradedEntriesPerItem() throws Exception {
        GradedCohort maths = new GradedCohort();
        String performance = "/v1/lms/cohorts/" + maths.cohort + ":performance";
        String student = token(maths.tenant, maths.asha, List.of(maths.institution.toString()), List.of(),
                "lms.cohort.get_cohort_performance", "lms.cohort.read");
        assertThat(call("GET", performance, student, null).status()).as("teaching staff only").isEqualTo(403);

        Reply read = call("GET", performance, maths.teacher, null);
        assertThat(read.status()).as(read.text()).isEqualTo(200);
        Map<String, JsonNode> byItem = new HashMap<>();
        read.body().path("data").forEach(item -> byItem.put(item.path("contentItemId").asString(), item));
        JsonNode homework = byItem.get(maths.homework.toString());
        assertThat(homework.path("averagePercent").asString()).as("only Asha's 80% counts").isEqualTo("80.00");
        assertThat(homework.path("medianPercent").asString()).isEqualTo("80.00");
        assertThat(homework.path("submitted").asInt()).isEqualTo(1);
        assertThat(homework.path("graded").asInt()).isEqualTo(1);
        assertThat(homework.path("missing").asInt()).as("an excused student is not missing").isEqualTo(0);
        JsonNode quiz = byItem.get(maths.quiz.toString());
        assertThat(quiz.path("averagePercent").asString()).as("(75 + 50) / 2").isEqualTo("62.50");
        assertThat(quiz.path("medianPercent").asString()).isEqualTo("62.50");
        assertThat(quiz.path("graded").asInt()).isEqualTo(2);
    }

    @Test
    void progressCountsAccountedItemsAndAStudentSeesOnlyTheirOwn() throws Exception {
        GradedCohort maths = new GradedCohort();
        String progress = "/v1/lms/cohorts/" + maths.cohort + ":progress";
        Reply all = call("GET", progress, maths.teacher, null);
        assertThat(all.status()).as(all.text()).isEqualTo(200);
        assertThat(all.body().path("data")).hasSize(2);
        all.body().path("data").forEach(row -> {
            assertThat(row.path("assessmentsCompleted").asInt()).isEqualTo(2);
            assertThat(row.path("assessmentsTotal").asInt()).isEqualTo(2);
        });

        String asha = token(maths.tenant, maths.asha, List.of(maths.institution.toString()), List.of(),
                "lms.cohort.get_cohort_progress", "lms.cohort.read");
        Reply own = call("GET", progress, asha, null);
        assertThat(own.status()).as(own.text()).isEqualTo(200);
        assertThat(own.body().path("data")).hasSize(1);
        assertThat(own.body().path("data").path(0).path("userId").asString()).isEqualTo(maths.asha.toString());
    }

    @Test
    void finalGradesAreTheWeightedTotalsWhenTheCohortHasNoScheme() throws Exception {
        GradedCohort maths = new GradedCohort();
        String publish = "/v1/lms/cohorts/" + maths.cohort + ":publishFinalGrades";
        String student = token(maths.tenant, maths.asha, List.of(maths.institution.toString()), List.of(),
                "lms.cohort.publish_final_grades", "lms.cohort.read");
        assertThat(call("POST", publish, student, "{}").status()).as("teaching staff only").isEqualTo(403);

        Reply published = call("POST", publish, maths.teacher, "{}");
        assertThat(published.status()).as(published.text()).isEqualTo(200);
        Map<String, JsonNode> byUser = new HashMap<>();
        published.body().path("items").forEach(row -> byUser.put(row.path("userId").asString(), row));
        assertThat(byUser.get(maths.asha.toString()).path("finalGrade").asString())
                .as("(60×80 + 40×75) / 100").isEqualTo("78.00");
        assertThat(byUser.get(maths.ravi.toString()).path("finalGrade").asString())
                .as("only the quiz category is in play").isEqualTo("50.00");
        assertThat(byUser.get(maths.asha.toString()).path("gradePublished").asBoolean()).isTrue();

        maths.sql("UPDATE lms.cohort SET grading_scheme_id = :a WHERE tenant_id = :tenantId AND id = :b",
                UUID.randomUUID(), maths.cohort);
        assertThat(call("POST", publish, maths.teacher, "{}").status())
                .as("grading bands come from assessment, which is not running here").isEqualTo(503);
        maths.sql("UPDATE lms.cohort SET lifecycle_status = 'archived' WHERE tenant_id = :tenantId AND id = :a",
                maths.cohort);
        assertThat(call("POST", publish, maths.teacher, "{}").status()).isEqualTo(409);
    }

    @Test
    void gradebookNamesComeFromIam() throws Exception {
        GradedCohort maths = new GradedCohort();
        assertThat(call("GET", "/v1/lms/cohorts/" + maths.cohort + ":gradebook", maths.teacher, null).status())
                .as("names come from iam, which is not running here").isEqualTo(503);
    }

    @Test
    void releaseRulesGrantTheTargetsTheirConditionsEarn() throws Exception {
        GradedCohort maths = new GradedCohort();
        UUID rule = UUID.randomUUID();
        maths.sql("INSERT INTO lms.release_rule (id, tenant_id, cohort_id, source_content_item_id, title, "
                + "stop_if_true, is_disabled, reassign_source, sort_order, row_version) VALUES (:a, :tenantId, :b, "
                + ":c, 'Pass homework first', false, false, false, 1, 1)", rule, maths.cohort, maths.homework);
        maths.sql("INSERT INTO lms.release_rule_condition (id, tenant_id, release_rule_id, operator, condition_type, "
                + "value, sort_order, row_version) VALUES (:a, :tenantId, :b, 'gte', 'score_at_least', '70', 1, 1)",
                UUID.randomUUID(), rule);
        maths.sql("INSERT INTO lms.release_rule_target (id, tenant_id, release_rule_id, content_item_id, "
                + "row_version) VALUES (:a, :tenantId, :b, :c, 1)", UUID.randomUUID(), rule, maths.quiz);
        String evaluate = "/v1/lms/cohorts/" + maths.cohort + ":evaluateReleaseRules";

        Reply decided = call("POST", evaluate, maths.admin, "{}");
        assertThat(decided.status()).as(decided.text()).isEqualTo(200);
        Map<String, Boolean> released = new HashMap<>();
        decided.body().path("items").forEach(grant -> released.put(grant.path("userId").asString(),
                grant.path("isReleased").asBoolean()));
        assertThat(released).as("Asha's 80% passes the gate, Ravi's excused homework does not")
                .containsEntry(maths.asha.toString(), true).containsEntry(maths.ravi.toString(), false);

        assertThat(call("POST", evaluate, maths.admin, "{}").body().path("items"))
                .as("nothing changed, so nothing is answered").isEmpty();

        maths.sql("UPDATE lms.cohort SET lifecycle_status = 'archived' WHERE tenant_id = :tenantId AND id = :a",
                maths.cohort);
        assertThat(call("POST", evaluate, maths.admin, "{}").status()).isEqualTo(409);
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
