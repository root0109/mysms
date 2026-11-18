package com.mysms.lms;

import static com.mysms.lms.LmsFixtures.expect;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.GrpcParity;
import com.mysms.core.testkit.GrpcParity.Answer;
import com.mysms.core.testkit.GrpcParityContract;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.lms.LmsFixtures.Reply;
import com.mysms.lms.business.QuizAttemptToken;
import com.mysms.lms.config.LmsDatabaseConfig;
import io.grpc.ManagedChannel;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Who may grade and how a running quiz ends, over REST and gRPC against real PostgreSQL: grading by the cohort's
 * teaching staff only, a grade keeping what its input omits, the placement's due time closing an attempt, the quiz's
 * violation limit, the member profile's permission, and what the public may see and filter of courses, reviews and
 * cohorts.
 */
@SpringBootTest(classes = LmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=TeachingRulesComponentTest")
class TeachingRulesComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String GRADE = "/v1/lms/gradebook-entries:grade";
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(LmsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private LmsFixtures fx;
    private UUID tenant;
    private UUID institution;
    private UUID cohort;
    private UUID studentId;
    private String admin;
    private OffsetDateTime startedAt;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "lms");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.secrets.keys", () -> LmsFixtures.SECRETS);
        registry.add("mysms.secrets.active-key", () -> "test");
    }

    @AfterAll
    static void closeChannel() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    @BeforeEach
    void school() {
        fx = new LmsFixtures(TOKENS, database, port);
        tenant = UUID.randomUUID();
        institution = UUID.randomUUID();
        admin = fx.admin(tenant);
        cohort = fx.cohort(tenant, institution);
        studentId = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.cohort_enrollment (id, tenant_id, cohort_id, user_id) VALUES (:id, :tenantId, "
                + ":cohort, :user)", "id", UUID.randomUUID(), "cohort", cohort, "user", studentId);
    }

    private Answer grpc(String rpcName, String token, String json) {
        if (channel == null) {
            channel = GrpcParity.channel("TeachingRulesComponentTest");
        }
        return LmsFixtures.grpc(channel, rpcName, token, json);
    }

    private UUID teacherOf(UUID inCohort) {
        UUID user = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.cohort_teacher (id, tenant_id, cohort_id, user_id) VALUES (:id, :tenantId, "
                + ":cohort, :user)", "id", UUID.randomUUID(), "cohort", inCohort, "user", user);
        return user;
    }

    private String grade(UUID item, String grade) {
        return "{\"contentItemId\":\"" + item + "\",\"userId\":\"" + studentId + "\",\"grade\":" + grade + "}";
    }

    // ---------------------------------------------------------------------------------------------- gradebook

    @Test
    void onlyTheCohortsTeachingStaffGrade() throws Exception {
        UUID item = fx.contentItem(tenant, cohort, "link", "link_url", "https://example.com", "20");
        String grader = fx.member(tenant, institution, "lms.gradebook_entry.grade_entry");
        Reply refused = expect(fx.call("POST", GRADE, grader, grade(item, "{\"points\":\"10\"}")), 403);
        GrpcParityContract.assertSameAnswer(refused.asAnswer(),
                grpc("GradeEntry", grader, grade(item, "{\"points\":\"10\"}")));
        String teacher = fx.user(teacherOf(cohort), tenant, institution, "lms.gradebook_entry.grade_entry");
        expect(fx.call("POST", GRADE, teacher, grade(item, "{\"points\":\"10\"}")), 200);
        String otherTeacher = fx.user(teacherOf(fx.cohort(tenant, institution)), tenant, institution,
                "lms.gradebook_entry.batch_grade_entries");
        Reply batch = expect(fx.call("POST", "/v1/lms/gradebook-entries:batchGrade", otherTeacher, "{\"entries\":["
                + grade(item, "{\"points\":\"12\"}") + "]}"), 200);
        assertThat(batch.body().path("items").get(0).path("error").asString()).isEqualTo("PERMISSION_DENIED");
    }

    @Test
    void aGradeKeepsWhatItsInputOmits() throws Exception {
        UUID item = fx.contentItem(tenant, cohort, "link", "link_url", "https://example.com", "20");
        expect(fx.call("POST", GRADE, admin, grade(item, "{\"points\":\"15\"}")), 200);
        Reply commented = expect(fx.call("POST", GRADE, admin, grade(item, "{\"feedback\":\"Well argued\"}")), 200);
        assertThat(commented.body().path("manualPoints").asString()).isEqualTo("15.00");
        assertThat(commented.body().path("points").asString()).isEqualTo("15.00");
        assertThat(commented.body().path("feedback").asString()).isEqualTo("Well argued");

        expect(fx.call("POST", GRADE, admin, grade(item, "{\"status\":\"excused\"}")), 200);
        Reply published = expect(fx.call("POST", GRADE, admin, grade(item, "{\"publish\":true}")), 200);
        assertThat(published.body().path("status").asString()).as("a publish keeps the excuse").isEqualTo("excused");
        assertThat(published.body().path("manualPoints").asString()).isEqualTo("15.00");
    }

    @Test
    void quizAnswersAreMarkedByTheCohortsTeachingStaffOnly() throws Exception {
        UUID quiz = quiz(null);
        UUID item = quizItem(quiz, null, false, null);
        UUID attempt = attempt(quiz, item);
        fx.sql(tenant, "UPDATE lms.quiz_submission SET status = 'submitted' WHERE id = :id", "id", attempt);
        String answers = "\"answers\":[{\"questionId\":\"" + UUID.randomUUID() + "\",\"marks\":\"1\"}]";
        String body = "{" + answers + "}";
        String grader = fx.member(tenant, institution, "lms.quiz_submission.grade_quiz_answers");
        String path = "/v1/lms/quiz-submissions/" + attempt + ":gradeAnswers";
        Reply refused = expect(fx.call("POST", path, grader, body), 403);
        GrpcParityContract.assertSameAnswer(refused.asAnswer(), grpc("GradeQuizAnswers", grader,
                "{\"quizSubmissionId\":\"" + attempt + "\"," + answers + "}"));
        String teacher = fx.user(teacherOf(cohort), tenant, institution, "lms.quiz_submission.grade_quiz_answers");
        // past the reach check, the cohort's teacher meets the body's own refusal: no such question
        expect(fx.call("POST", path, teacher, body), 422);
    }

    // ------------------------------------------------------------------------------------------- quiz deadlines

    private UUID quiz(Integer maxViolations) {
        UUID quiz = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.quiz (id, tenant_id, institution_id, title, passing_percentage, total_marks, "
                        + "proctoring_enabled, max_violations) VALUES (:id, :tenantId, :institution, 'Quiz', 50, 10, "
                        + "true, :max)", "id", quiz, "institution", institution, "max", maxViolations);
        return quiz;
    }

    /** The quiz placed in the cohort, due at the given time (null: none), taking late work for the given days. */
    private UUID quizItem(UUID quiz, OffsetDateTime dueAt, boolean late, Integer lateDays) {
        UUID item = fx.contentItem(tenant, cohort, "quiz", "quiz_id", quiz, "20");
        fx.sql(tenant, "UPDATE lms.content_item SET due_at = :due, late_submission_allowed = :late, "
                + "late_submission_days = :days WHERE id = :id", "id", item, "due", dueAt, "late", late,
                "days", lateDays);
        return item;
    }

    private static OffsetDateTime started() {
        return OffsetDateTime.now(ZoneOffset.UTC).minusHours(2).truncatedTo(ChronoUnit.MICROS);
    }

    private UUID attempt(UUID quiz, UUID item) {
        UUID attempt = UUID.randomUUID();
        startedAt = started();
        fx.sql(tenant, "INSERT INTO lms.quiz_submission (id, tenant_id, quiz_id, user_id, content_item_id, cohort_id, "
                        + "status, started_at, attempt_number) VALUES (:id, :tenantId, :quiz, :user, :item, :cohort, "
                        + "'in_progress', :started, 1)", "id", attempt, "quiz", quiz, "user", studentId, "item", item,
                "cohort", cohort, "started", startedAt);
        return attempt;
    }

    private String student() {
        return fx.user(studentId, tenant, institution, "lms.quiz_submission.save_quiz_responses",
                "lms.quiz.submit_quiz");
    }

    private String save(UUID attempt) {
        return "{\"attemptToken\":\"" + QuizAttemptToken.of(attempt, 1, startedAt)
                + "\",\"clientSequence\":\"1\",\"answers\":[]}";
    }

    private String submit(UUID attempt, String violations) {
        return "{\"attemptToken\":\"" + QuizAttemptToken.of(attempt, 1, startedAt) + "\",\"answers\":[],"
                + "\"violations\":[" + violations + "]}";
    }

    @Test
    void thePlacementsDueTimeEndsAnAttemptThatHasNoTimer() throws Exception {
        UUID quiz = quiz(null);
        UUID attempt = attempt(quiz, quizItem(quiz, OffsetDateTime.now(ZoneOffset.UTC).minusHours(1), false, null));
        Reply late = expect(fx.call("POST", "/v1/lms/quiz-submissions/" + attempt + ":saveResponses", student(),
                save(attempt)), 409);
        assertThat(late.body().path("code").asString()).isEqualTo("ATTEMPT_TIME_UP");
        Reply submitted = expect(fx.call("POST", "/v1/lms/quiz-submissions/" + attempt + ":submit", student(),
                submit(attempt, "")), 200);
        assertThat(submitted.body().path("submissionReason").asString()).isEqualTo("time_up");
    }

    @Test
    void aPlacementTakingLateWorkRecordsALateSubmission() throws Exception {
        UUID quiz = quiz(null);
        UUID attempt = attempt(quiz, quizItem(quiz, OffsetDateTime.now(ZoneOffset.UTC).minusHours(1), true, 2));
        expect(fx.call("POST", "/v1/lms/quiz-submissions/" + attempt + ":saveResponses", student(), save(attempt)),
                200);
        Reply submitted = expect(fx.call("POST", "/v1/lms/quiz-submissions/" + attempt + ":submit", student(),
                submit(attempt, "")), 200);
        assertThat(submitted.body().path("submissionReason").asString()).isEqualTo("late");
    }

    @Test
    void reachingTheViolationLimitEndsTheAttemptForViolations() throws Exception {
        UUID quiz = quiz(2);
        UUID attempt = attempt(quiz, quizItem(quiz, null, false, null));
        String violation = "{\"eventType\":\"tab_switch\",\"severity\":\"violation\",\"occurredAt\":\""
                + OffsetDateTime.now(ZoneOffset.UTC) + "\"}";
        Reply submitted = expect(fx.call("POST", "/v1/lms/quiz-submissions/" + attempt + ":submit", student(),
                submit(attempt, violation + "," + violation)), 200);
        assertThat(submitted.body().path("submissionReason").asString()).isEqualTo("violations");
        assertThat(submitted.body().path("violationCount").asInt()).isEqualTo(2);
    }

    // ------------------------------------------------------------------------------------------ public readers

    @Test
    void aMemberProfileNeedsItsPermission() throws Exception {
        String without = fx.member(tenant, institution, "lms.enrollment.read");
        expect(fx.call("GET", "/v1/lms/members/" + studentId + ":profile", without, null), 403);
    }

    @Test
    void thePublicNeitherSeesNorFiltersOnWhoWroteAReview() throws Exception {
        UUID course = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.course (id, tenant_id, institution_id, title, is_published, lifecycle_status, "
                + "author_comments) VALUES (:id, :tenantId, :institution, 'Open', true, 'published', 'internal')",
                "id", course, "institution", institution);
        fx.sql(tenant, "INSERT INTO lms.course_review (id, tenant_id, course_id, user_id, rating) VALUES (:id, "
                + ":tenantId, :course, :user, 4)", "id", UUID.randomUUID(), "course", course, "user", studentId);
        String reader = fx.member(tenant, institution, "lms.enrollment.read");
        Reply reviews = expect(fx.call("GET", "/v1/lms/courses/" + course + "/reviews", reader, null), 200);
        assertThat(reviews.body().path("data").get(0).has("userId")).isFalse();
        expect(fx.call("GET", "/v1/lms/courses/" + course + "/reviews?filter=" + encode("userId = \"" + studentId
                + "\""), reader, null), 400);
        expect(fx.call("GET", "/v1/lms/courses?filter=" + encode("authorComments = \"internal\""), reader, null), 400);
        expect(fx.call("GET", "/v1/lms/courses?filter=" + encode("authorComments = \"internal\""), admin, null), 200);
    }

    @Test
    void thePublicSeesOpenCohortsInTheGuestProjection() throws Exception {
        fx.sql(tenant, "UPDATE lms.cohort SET allow_self_enrollment = true, end_date = DATE '2099-12-31', "
                + "external_ref = 'INTERNAL' WHERE id = :id", "id", cohort);
        UUID closed = fx.cohort(tenant, institution);
        String reader = fx.member(tenant, institution, "lms.enrollment.read");
        Reply open = expect(fx.call("GET", "/v1/lms/cohorts/" + cohort, reader, null), 200);
        assertThat(open.body().has("externalRef")).isFalse();
        GrpcParityContract.assertSameAnswer(open.asAnswer(), grpc("GetCohort", reader, "{\"id\":\"" + cohort + "\"}"));
        expect(fx.call("GET", "/v1/lms/cohorts/" + closed, reader, null), 404);
        assertThat(expect(fx.call("GET", "/v1/lms/cohorts", reader, null), 200).body().path("data")
                .findValuesAsString("id")).containsExactly(cohort.toString());
        expect(fx.call("GET", "/v1/lms/cohorts/" + cohort + "/courses", reader, null), 200);
        expect(fx.call("GET", "/v1/lms/cohorts/" + cohort, null, null), 404);
        expect(fx.call("GET", "/v1/lms/cohorts?filter=" + encode("externalRef = \"INTERNAL\""), reader, null), 400);
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
