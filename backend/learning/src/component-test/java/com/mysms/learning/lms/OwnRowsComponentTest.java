package com.mysms.learning.lms;

import com.mysms.learning.LearningApplication;
import static com.mysms.learning.lms.LmsFixtures.expect;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.GrpcParity;
import com.mysms.core.testkit.GrpcParity.Answer;
import com.mysms.core.testkit.GrpcParityContract;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.learning.lms.LmsFixtures.Reply;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import io.grpc.ManagedChannel;
import java.util.ArrayList;
import java.util.List;
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
import tools.jackson.databind.JsonNode;

/**
 * The records a person writes for themself, over REST and gRPC against real PostgreSQL: whose row it is comes from
 * the token whatever the body says, grading fields are the workflows' own, another student's rows are absent (404,
 * never listed) and a cohort's teaching staff reach their cohort's rows — assignment submissions, forum posts and
 * votes, certificate bookings, bookmarks, annotations, course reviews, quiz attempts and their answers.
 */
@SpringBootTest(classes = LearningApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=OwnRowsComponentTest")
class OwnRowsComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String SUBMISSIONS = "/v1/lms/assignment-submissions";
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
    private UUID studentA;
    private UUID studentB;
    private UUID teacher;

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
        cohort = fx.cohort(tenant, institution);
        studentA = enrolled(cohort);
        studentB = enrolled(cohort);
        teacher = teaching(cohort);
    }

    private UUID enrolled(UUID inCohort) {
        UUID user = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.cohort_enrollment (id, tenant_id, cohort_id, user_id) VALUES (:id, :tenantId, "
                + ":cohort, :user)", "id", UUID.randomUUID(), "cohort", inCohort, "user", user);
        return user;
    }

    private UUID teaching(UUID inCohort) {
        UUID user = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.cohort_teacher (id, tenant_id, cohort_id, user_id) VALUES (:id, :tenantId, "
                + ":cohort, :user)", "id", UUID.randomUUID(), "cohort", inCohort, "user", user);
        return user;
    }

    /**
     * The user's token: the permissions of the records under test in the institution, as a student or a teacher role
     * carries them — not lms.cohort.update, which makes a cohort's manager.
     */
    private String as(UUID user) {
        return fx.user(user, tenant, institution, "lms.assignment_submission.*", "lms.forum_post.*",
                "lms.forum_post_vote.*", "lms.certificate_request.*", "lms.course_review.*", "lms.annotation.*",
                "lms.bookmark.*", "lms.quiz_submission.*", "lms.quiz_answer.*");
    }

    private Answer grpc(String rpcName, String token, String json) {
        if (channel == null) {
            channel = GrpcParity.channel("OwnRowsComponentTest");
        }
        return LmsFixtures.grpc(channel, rpcName, token, json);
    }

    private static List<String> ids(JsonNode list) {
        List<String> ids = new ArrayList<>();
        list.forEach(row -> ids.add(row.path("id").asString()));
        return ids;
    }

    private static String etag(Reply reply) {
        return reply.body().path("etag").asString();
    }

    // ------------------------------------------------------------------------------------- assignment submissions

    private UUID placedAssignment(UUID inCohort) {
        UUID assignment = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.assignment (id, tenant_id, institution_id, title, question, submission_type) "
                + "VALUES (:id, :tenantId, :institution, 'Essay', 'Write.', 'text')", "id", assignment,
                "institution", institution);
        fx.contentItem(tenant, inCohort, "assignment", "assignment_id", assignment, "20");
        return assignment;
    }

    @Test
    void aSubmissionIsTheCallersOwnDraftAndOnlyItsStudentAndTheCohortsTeachersReachIt() throws Exception {
        UUID assignment = placedAssignment(cohort);
        UUID item = fx.value(tenant, "SELECT id FROM lms.content_item WHERE assignment_id = :a", UUID.class,
                "a", assignment);
        Reply created = expect(fx.call("POST", SUBMISSIONS, as(studentA), "{\"assignmentId\":\"" + assignment
                + "\",\"contentItemId\":\"" + item + "\",\"userId\":\"" + studentB + "\",\"answer\":\"Mine\","
                + "\"status\":\"pass\",\"isFinal\":true,\"evaluatorUserId\":\"" + teacher + "\","
                + "\"evaluatorComments\":\"Self-graded\"}"), 201);
        String id = created.body().path("id").asString();
        assertThat(created.body().path("userId").asString()).isEqualTo(studentA.toString());
        assertThat(created.body().path("status").asString()).isEqualTo("not_graded");
        assertThat(created.body().path("isFinal").asBoolean()).isFalse();
        assertThat(created.body().has("evaluatorUserId")).isFalse();
        assertThat(created.body().has("evaluatorComments")).isFalse();

        // another student: absent over both transports, never listed, cannot change it
        expect(fx.call("GET", SUBMISSIONS + "/" + id, as(studentB), null), 404);
        GrpcParityContract.assertSameAnswer(fx.call("GET", SUBMISSIONS + "/" + id, as(studentB), null).asAnswer(),
                grpc("GetLmsAssignmentSubmission", as(studentB), "{\"id\":\"" + id + "\"}"));
        assertThat(ids(expect(fx.call("GET", SUBMISSIONS, as(studentB), null), 200).body().path("data")))
                .doesNotContain(id);
        expect(fx.call("PATCH", SUBMISSIONS + "/" + id, as(studentB), "{\"answer\":\"Theirs\"}", "If-Match",
                "\"1\""), 404);
        // its student updates the answer, never the grading fields
        Reply updated = expect(fx.call("PATCH", SUBMISSIONS + "/" + id, as(studentA), "{\"answer\":\"Better\","
                + "\"status\":\"pass\",\"evaluatorComments\":\"x\"}", "If-Match", "\"1\""), 200);
        assertThat(updated.body().path("answer").asString()).isEqualTo("Better");
        assertThat(updated.body().path("status").asString()).isEqualTo("not_graded");
        assertThat(ids(expect(fx.call("GET", SUBMISSIONS, as(studentA), null), 200).body().path("data")))
                .containsExactly(id);

        // the cohort's teacher reads and grades it; another cohort's teacher does not reach it
        assertThat(ids(expect(fx.call("GET", SUBMISSIONS, as(teacher), null), 200).body().path("data")))
                .containsExactly(id);
        expect(fx.call("GET", SUBMISSIONS + "/" + id, as(teacher), null), 200);
        UUID stranger = teaching(fx.cohort(tenant, institution));
        expect(fx.call("GET", SUBMISSIONS + "/" + id, as(stranger), null), 404);
        expect(fx.call("POST", SUBMISSIONS + "/" + id + ":finalize", as(studentA), "{}"), 200);
        expect(fx.call("POST", SUBMISSIONS + "/" + id + ":grade", as(studentA), "{\"status\":\"pass\"}"), 403);
        expect(fx.call("POST", SUBMISSIONS + "/" + id + ":grade", as(stranger), "{\"status\":\"pass\"}"), 404);
        Reply graded = expect(fx.call("POST", SUBMISSIONS + "/" + id + ":grade", as(teacher),
                "{\"status\":\"pass\",\"evaluatorComments\":\"Good\"}"), 200);
        assertThat(graded.body().path("evaluatorUserId").asString()).isEqualTo(teacher.toString());
    }

    @Test
    void aGroupSubmissionNeedsTheCallerInTheGroup() throws Exception {
        UUID assignment = placedAssignment(cohort);
        UUID group = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.assignment_group (id, tenant_id, content_item_id, cohort_id, name) SELECT :id, "
                + ":tenantId, id, cohort_id, 'Team' FROM lms.content_item WHERE assignment_id = :a", "id", group,
                "a", assignment);
        expect(fx.call("POST", SUBMISSIONS, as(studentA), "{\"assignmentId\":\"" + assignment + "\",\"groupId\":\""
                + group + "\"}"), 422);
    }

    // ------------------------------------------------------------------------------------ forum posts and votes

    private UUID topic(UUID inCohort) {
        UUID forum = UUID.randomUUID();
        UUID section = UUID.randomUUID();
        UUID topic = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.forum (id, tenant_id, cohort_id, title) VALUES (:id, :tenantId, :cohort, "
                + "'Forum')", "id", forum, "cohort", inCohort);
        fx.sql(tenant, "INSERT INTO lms.forum_section (id, tenant_id, forum_id, title) VALUES (:id, :tenantId, "
                + ":forum, 'General')", "id", section, "forum", forum);
        UUID prompt = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.discussion_prompt (id, tenant_id, institution_id, title) VALUES (:id, "
                + ":tenantId, :institution, 'Prompt')", "id", prompt, "institution", institution);
        fx.sql(tenant, "INSERT INTO lms.forum_topic (id, tenant_id, forum_section_id, discussion_prompt_id, title) "
                + "VALUES (:id, :tenantId, :section, :prompt, 'Week 1')", "id", topic, "section", section,
                "prompt", prompt);
        return topic;
    }

    @Test
    void aPostIsWrittenByTheCallerAndOnlyByTheCohortsMembers() throws Exception {
        UUID topic = topic(cohort);
        String posts = "/v1/lms/forum-topics/" + topic + "/posts";
        String body = "{\"forumTopicId\":\"" + topic + "\",\"authorUserId\":\"" + studentB + "\",\"body\":\"Hi\","
                + "\"isHidden\":true,\"likeCount\":99,\"isTeacherPost\":true}";
        Reply post = expect(fx.call("POST", posts, as(studentA), body), 201);
        assertThat(post.body().path("authorUserId").asString()).isEqualTo(studentA.toString());
        assertThat(post.body().path("isTeacherPost").asBoolean()).isFalse();
        assertThat(post.body().path("isHidden").asBoolean()).isFalse();
        assertThat(post.body().path("likeCount").asInt()).isZero();
        Reply byTeacher = expect(fx.call("POST", posts, as(teacher), body), 201);
        assertThat(byTeacher.body().path("isTeacherPost").asBoolean()).isTrue();
        expect(fx.call("POST", posts, as(UUID.randomUUID()), body), 403);
        String id = post.body().path("id").asString();
        expect(fx.call("PATCH", posts + "/" + id, as(studentB), "{\"body\":\"Edited\"}", "If-Match", "\"1\""), 404);
        expect(fx.call("PATCH", posts + "/" + id, as(studentA), "{\"body\":\"Edited\"}", "If-Match", "\"1\""), 200);

        // one vote per user per post, always the caller's
        String vote = "{\"forumPostId\":\"" + id + "\",\"userId\":\"" + studentB + "\",\"isLike\":true}";
        Reply voted = expect(fx.call("POST", "/v1/lms/forum-post-votes", as(studentA), vote), 201);
        assertThat(voted.body().path("userId").asString()).isEqualTo(studentA.toString());
        expect(fx.call("POST", "/v1/lms/forum-post-votes", as(studentA), vote), 409);
        expect(fx.call("POST", "/v1/lms/forum-post-votes", as(studentB), vote), 201);
    }

    // ------------------------------------------------------- bookings, bookmarks, annotations and course reviews

    @Test
    void theOwnerOfEveryOtherOwnRecordIsTheCaller() throws Exception {
        UUID course = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.course (id, tenant_id, institution_id, title) VALUES (:id, :tenantId, "
                + ":institution, 'Course')", "id", course, "institution", institution);
        Reply booking = expect(fx.call("POST", "/v1/lms/certificate-requests", as(studentA), "{\"courseId\":\""
                + course + "\",\"userId\":\"" + studentB + "\",\"requestDate\":\"2026-12-01\",\"startTime\":\"09:00\","
                + "\"endTime\":\"10:00\",\"status\":\"completed\"}"), 201);
        assertThat(booking.body().path("userId").asString()).isEqualTo(studentA.toString());
        assertThat(booking.body().path("status").asString()).isEqualTo("upcoming");
        expect(fx.call("DELETE", "/v1/lms/certificate-requests/" + booking.body().path("id").asString(),
                as(studentB), null, "If-Match", "\"1\""), 404);

        Reply review = expect(fx.call("POST", "/v1/lms/courses/" + course + "/reviews", as(studentA),
                "{\"courseId\":\"" + course + "\",\"userId\":\"" + studentB + "\",\"rating\":\"5\"}"), 201);
        assertThat(review.body().path("userId").asString()).isEqualTo(studentA.toString());
        expect(fx.call("DELETE", "/v1/lms/courses/" + course + "/reviews/" + review.body().path("id").asString(),
                as(studentB), null, "If-Match", "\"1\""), 404);

        Reply annotation = expect(fx.call("POST", "/v1/lms/annotations", as(studentA), "{\"userId\":\"" + studentB
                + "\",\"cohortId\":\"" + cohort + "\",\"annotationType\":\"note\",\"description\":\"Mine\"}"), 201);
        assertThat(annotation.body().path("userId").asString()).isEqualTo(studentA.toString());
        String annotationId = annotation.body().path("id").asString();
        expect(fx.call("GET", "/v1/lms/annotations/" + annotationId, as(studentB), null), 404);
        assertThat(ids(expect(fx.call("GET", "/v1/lms/annotations", as(studentB), null), 200).body().path("data")))
                .doesNotContain(annotationId);
    }

    @Test
    void bookmarksAreReadAndChangedByTheirOwnerOnly() throws Exception {
        Reply bookmark = expect(fx.call("POST", "/v1/lms/bookmarks", as(studentA), "{\"userId\":\"" + studentB
                + "\",\"title\":\"Notes\",\"url\":\"/lms/notes\",\"cohortId\":\"" + cohort + "\"}"), 201);
        assertThat(bookmark.body().path("userId").asString()).isEqualTo(studentA.toString());
        String path = "/v1/lms/bookmarks/" + bookmark.body().path("id").asString();
        expect(fx.call("GET", path, as(studentB), null), 404);
        GrpcParityContract.assertSameAnswer(fx.call("GET", path, as(studentB), null).asAnswer(),
                grpc("GetBookmark", as(studentB), "{\"id\":\"" + bookmark.body().path("id").asString() + "\"}"));
        expect(fx.call("PATCH", path, as(studentB), "{\"title\":\"Theirs\"}", "If-Match", "\"1\""), 404);
        expect(fx.call("DELETE", path, as(studentB), null, "If-Match", "\"1\""), 404);
        assertThat(ids(expect(fx.call("GET", "/v1/lms/bookmarks", as(studentB), null), 200).body().path("data")))
                .isEmpty();
        assertThat(ids(expect(fx.call("GET", "/v1/lms/bookmarks", as(studentA), null), 200).body().path("data")))
                .containsExactly(bookmark.body().path("id").asString());
        expect(fx.call("PATCH", path, as(studentA), "{\"title\":\"Renamed\"}", "If-Match", etag(bookmark)), 200);
    }

    // -------------------------------------------------------------------------------- quiz attempts and answers

    private UUID quiz(boolean showAnswers, boolean showFeedback) {
        UUID quiz = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.quiz (id, tenant_id, institution_id, title, passing_percentage, total_marks, "
                        + "show_answers, show_feedback) VALUES (:id, :tenantId, :institution, 'Quiz', 50, 10, "
                        + ":answers, :feedback)", "id", quiz, "institution", institution, "answers", showAnswers,
                "feedback", showFeedback);
        return quiz;
    }

    private UUID attempt(UUID quiz, UUID user, String status) {
        UUID attempt = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.quiz_submission (id, tenant_id, quiz_id, user_id, cohort_id, status) "
                + "VALUES (:id, :tenantId, :quiz, :user, :cohort, :status)", "id", attempt, "quiz", quiz,
                "user", user, "cohort", cohort, "status", status);
        fx.sql(tenant, "INSERT INTO lms.quiz_answer (id, tenant_id, quiz_submission_id, answer, is_correct, "
                + "marks_awarded, marks_possible, feedback) VALUES (:id, :tenantId, :attempt, 'B', true, 2, 2, "
                + "'Well reasoned')", "id", UUID.randomUUID(), "attempt", attempt);
        return attempt;
    }

    @Test
    void anotherStudentsAttemptsAndAnswersAreAbsent() throws Exception {
        UUID attempt = attempt(quiz(true, true), studentA, "graded");
        String path = "/v1/lms/quiz-submissions/" + attempt;
        expect(fx.call("GET", path, as(studentB), null), 404);
        expect(fx.call("GET", path + "/answers", as(studentB), null), 404);
        GrpcParityContract.assertSameAnswer(fx.call("GET", path + "/answers", as(studentB), null).asAnswer(),
                grpc("ListQuizAnswers", as(studentB), "{\"quizSubmissionId\":\"" + attempt + "\"}"));
        assertThat(ids(expect(fx.call("GET", "/v1/lms/quiz-submissions", as(studentB), null), 200).body()
                .path("data"))).doesNotContain(attempt.toString());
        assertThat(ids(expect(fx.call("GET", "/v1/lms/quiz-submissions", as(teacher), null), 200).body()
                .path("data"))).contains(attempt.toString());
        expect(fx.call("GET", path, as(studentA), null), 200);
    }

    @Test
    void aStudentSeesCorrectnessMarksAndFeedbackOnlyAsTheQuizsReviewAllows() throws Exception {
        UUID running = attempt(quiz(true, true), studentA, "in_progress");
        JsonNode during = expect(fx.call("GET", "/v1/lms/quiz-submissions/" + running + "/answers", as(studentA),
                null), 200).body().path("data").get(0);
        assertThat(during.has("isCorrect")).isFalse();
        assertThat(during.has("marksAwarded")).isFalse();
        assertThat(during.has("feedback")).isFalse();
        JsonNode overGrpc = grpc("ListQuizAnswers", as(studentA), "{\"quizSubmissionId\":\"" + running + "\"}")
                .body().path("items").get(0);
        assertThat(overGrpc.has("isCorrect")).isFalse();

        UUID noFeedback = attempt(quiz(true, false), studentA, "graded");
        JsonNode after = expect(fx.call("GET", "/v1/lms/quiz-submissions/" + noFeedback + "/answers", as(studentA),
                null), 200).body().path("data").get(0);
        assertThat(after.path("isCorrect").asBoolean()).isTrue();
        assertThat(after.path("marksAwarded").asString()).isEqualTo("2.00");
        assertThat(after.has("feedback")).isFalse();

        UUID hidden = attempt(quiz(false, true), studentA, "graded");
        assertThat(expect(fx.call("GET", "/v1/lms/quiz-submissions/" + hidden + "/answers", as(studentA), null), 200)
                .body().path("data").get(0).has("isCorrect")).isFalse();
        JsonNode staff = expect(fx.call("GET", "/v1/lms/quiz-submissions/" + running + "/answers", as(teacher),
                null), 200).body().path("data").get(0);
        assertThat(staff.path("isCorrect").asBoolean()).isTrue();
        assertThat(staff.path("feedback").asString()).isEqualTo("Well reasoned");
    }
}
