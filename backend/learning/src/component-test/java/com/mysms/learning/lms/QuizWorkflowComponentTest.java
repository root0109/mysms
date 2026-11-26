package com.mysms.learning.lms;

import com.mysms.learning.LearningApplication;
import static com.mysms.learning.lms.LmsFixtures.expect;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.learning.lms.LmsFixtures.Reply;
import com.mysms.learning.lms.business.QuizAttemptToken;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The quiz workflows over HTTP against real PostgreSQL: building a quiz's question set, instant feedback on practice
 * quizzes, and a student's attempt — autosave with its stale-sequence guard and attempt token, pause and resume,
 * submission marked against the answer keys with hand-worked marks, a teacher's marking of open-ended answers, and
 * the marked score posted to the gradebook. A student acts only on their own attempt; another tenant sees nothing.
 */
@SpringBootTest(classes = LearningApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuizWorkflowComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String[] STUDENT_PERMISSIONS = {"lms.quiz_submission.save_quiz_responses",
        "lms.quiz_submission.pause_quiz_attempt", "lms.quiz_submission.resume_quiz_attempt", "lms.quiz.submit_quiz",
        "lms.quiz.check_quiz_answer"};

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(LmsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private LmsFixtures fx;
    private UUID tenant;
    private UUID institution;
    private String admin;
    private UUID studentId;
    private String student;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "lms");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.secrets.keys", () -> LmsFixtures.SECRETS);
        registry.add("mysms.secrets.active-key", () -> "test");
    }

    @BeforeEach
    void school() {
        fx = new LmsFixtures(TOKENS, database, port);
        tenant = UUID.randomUUID();
        institution = UUID.randomUUID();
        admin = fx.admin(tenant);
        studentId = UUID.randomUUID();
        student = fx.user(studentId, tenant, institution, STUDENT_PERMISSIONS);
    }

    // ---- seeding

    private UUID quiz(boolean showFeedback) {
        UUID quiz = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.quiz (id, tenant_id, institution_id, title, passing_percentage, total_marks, "
                        + "allow_pause, show_feedback, show_answers) VALUES (:id, :tenantId, :institution, 'Quiz', 50, "
                        + "10, true, :feedback, true)",
                "id", quiz, "institution", institution, "feedback", showFeedback);
        return quiz;
    }

    /** A choices question with options A (correct, explained) and B. */
    private UUID choices(String marks) {
        UUID question = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.question (id, tenant_id, question_type, question_text, marks) "
                + "VALUES (:id, :tenantId, 'choices', 'Pick A', :marks)", "id", question,
                "marks", new BigDecimal(marks));
        fx.sql(tenant, "INSERT INTO lms.question_option (id, tenant_id, question_id, sort_order, option_text, "
                        + "is_correct, explanation) VALUES (:a, :tenantId, :question, 1, 'A', true, 'A is right'), "
                        + "(:b, :tenantId, :question, 2, 'B', false, NULL)",
                "a", optionA(question), "b", optionB(question), "question", question);
        return question;
    }

    private UUID question(String type, String marks) {
        UUID question = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.question (id, tenant_id, question_type, question_text, marks, numeric_answer, "
                        + "numeric_tolerance) VALUES (:id, :tenantId, :type, 'Q', :marks, "
                        + "CASE WHEN :type = 'numeric' THEN 4.0 END, CASE WHEN :type = 'numeric' THEN 0.5 END)",
                "id", question, "type", type, "marks", new BigDecimal(marks));
        return question;
    }

    private UUID optionA(UUID question) {
        return UUID.nameUUIDFromBytes(("A" + question).getBytes());
    }

    private UUID optionB(UUID question) {
        return UUID.nameUUIDFromBytes(("B" + question).getBytes());
    }

    private void place(UUID quiz, UUID question, String marks, int order) {
        fx.sql(tenant, "INSERT INTO lms.quiz_question (id, tenant_id, quiz_id, question_id, marks, sort_order) "
                        + "VALUES (:id, :tenantId, :quiz, :question, :marks, :order)",
                "id", UUID.randomUUID(), "quiz", quiz, "question", question, "marks", new BigDecimal(marks),
                "order", order);
    }

    /** An in-progress attempt of the student; answers the attempt id (its token is {@link #token}). */
    private UUID attempt(UUID quiz, UUID item, UUID cohort, UUID user) {
        UUID attempt = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.quiz_submission (id, tenant_id, quiz_id, user_id, content_item_id, cohort_id, "
                        + "status, started_at, attempt_number) VALUES (:id, :tenantId, :quiz, :user, :item, :cohort, "
                        + "'in_progress', :started, 1)",
                "id", attempt, "quiz", quiz, "user", user, "item", item, "cohort", cohort, "started", started());
        return attempt;
    }

    /** The delivered answer sheet StartQuizAttempt would have written: one blank row per question. */
    private void deliver(UUID attempt, UUID question, String marks, int order) {
        fx.sql(tenant, "INSERT INTO lms.quiz_answer (id, tenant_id, quiz_submission_id, question_id, marks_possible, "
                        + "sort_order) VALUES (:id, :tenantId, :attempt, :question, :marks, :order)",
                "id", UUID.randomUUID(), "attempt", attempt, "question", question,
                "marks", new BigDecimal(marks), "order", order);
    }

    private static OffsetDateTime started() {
        return OffsetDateTime.parse("2026-11-12T09:00:00Z").withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
    }

    private static String token(UUID attempt) {
        return QuizAttemptToken.of(attempt, 1, started());
    }

    private String path(UUID attempt, String step) {
        return "/v1/lms/quiz-submissions/" + attempt + ":" + step;
    }

    // ---- question set

    @Test
    void setQuizQuestionsAddsRemovesAndGuardsThePublishedQuiz() throws Exception {
        UUID quiz = quiz(true);
        UUID q1 = choices("6");
        UUID q2 = question("numeric", "4");
        String path = "/v1/lms/quizzes/" + quiz + ":setQuestions";
        Reply set = expect(fx.call("POST", path, admin,
                "{\"addQuestionIds\":[\"" + q1 + "\",\"" + q2 + "\"]}"), 200);
        assertThat(set.body().path("items")).hasSize(2);
        Reply removed = expect(fx.call("POST", path, admin, "{\"removeQuestionIds\":[\"" + q2 + "\"]}"), 200);
        assertThat(removed.body().path("items")).hasSize(1);
        expect(fx.call("POST", path, admin, "{\"addQuestionIds\":[\"" + UUID.randomUUID() + "\"]}"), 422);
        expect(fx.call("POST", path, fx.member(tenant, institution, "lms.quiz.read"),
                "{\"addQuestionIds\":[\"" + q2 + "\"]}"), 403);
        expect(fx.call("POST", "/v1/lms/quizzes/" + quiz + ":setQuestions", fx.admin(UUID.randomUUID()),
                "{\"addQuestionIds\":[\"" + q2 + "\"]}"), 404);
        fx.sql(tenant, "UPDATE lms.quiz SET published_at = now() WHERE id = :id", "id", quiz);
        Reply published = expect(fx.call("POST", path, admin, "{\"addQuestionIds\":[\"" + q2 + "\"]}"), 409);
        assertThat(published.body().path("code").asString()).isEqualTo("QUIZ_PUBLISHED");
    }

    @Test
    void checkQuizAnswerGivesInstantFeedbackOnPracticeQuizzesOnly() throws Exception {
        UUID quiz = quiz(true);
        UUID q1 = choices("6");
        place(quiz, q1, "6", 1);
        String path = "/v1/lms/quizzes/" + quiz + ":checkAnswer";
        Reply right = expect(fx.call("POST", path, student, "{\"questionId\":\"" + q1 + "\",\"answer\":"
                + "{\"questionId\":\"" + q1 + "\",\"selectedOptionIds\":[\"" + optionA(q1) + "\"]}}"), 200);
        assertThat(right.body().path("isCorrect").asBoolean()).isTrue();
        Reply wrong = expect(fx.call("POST", path, student, "{\"questionId\":\"" + q1 + "\",\"answer\":"
                + "{\"questionId\":\"" + q1 + "\",\"selectedOptionIds\":[\"" + optionB(q1) + "\"]}}"), 200);
        assertThat(wrong.body().path("isCorrect").asBoolean()).isFalse();
        UUID silent = quiz(false);
        place(silent, q1, "6", 1);
        Reply refused = expect(fx.call("POST", "/v1/lms/quizzes/" + silent + ":checkAnswer", student,
                "{\"questionId\":\"" + q1 + "\",\"answer\":{\"questionId\":\"" + q1 + "\"}}"), 409);
        assertThat(refused.body().path("code").asString()).isEqualTo("FEEDBACK_NOT_AVAILABLE");
    }

    // ---- the attempt

    @Test
    void anAttemptIsSavedPausedResumedAndMarkedOnSubmission() throws Exception {
        UUID quiz = quiz(true);
        UUID q1 = choices("6");
        UUID q2 = question("numeric", "4");
        place(quiz, q1, "6", 1);
        place(quiz, q2, "4", 2);
        UUID cohort = fx.cohort(tenant, institution);
        UUID item = fx.contentItem(tenant, cohort, "quiz", "quiz_id", quiz, "20");
        UUID attempt = attempt(quiz, item, cohort, studentId);
        deliver(attempt, q1, "6", 1);
        deliver(attempt, q2, "4", 2);
        String token = token(attempt);

        Reply saved = expect(fx.call("POST", path(attempt, "saveResponses"), student,
                "{\"attemptToken\":\"" + token + "\",\"clientSequence\":\"1\",\"answers\":[{\"questionId\":\"" + q1
                        + "\",\"selectedOptionIds\":[\"" + optionA(q1) + "\"]}]}"), 200);
        assertThat(saved.body().path("savedCount").asInt()).isEqualTo(1);
        Reply stale = expect(fx.call("POST", path(attempt, "saveResponses"), student,
                "{\"attemptToken\":\"" + token + "\",\"clientSequence\":\"1\",\"answers\":[{\"questionId\":\"" + q1
                        + "\",\"selectedOptionIds\":[\"" + optionB(q1) + "\"]}]}"), 200);
        assertThat(stale.body().path("savedCount").asInt()).isZero();
        assertThat(stale.body().path("ignoredStale").asInt()).isEqualTo(1);
        Reply badToken = expect(fx.call("POST", path(attempt, "saveResponses"), student,
                "{\"attemptToken\":\"forged\",\"clientSequence\":\"2\",\"answers\":[]}"), 409);
        assertThat(badToken.body().path("code").asString()).isEqualTo("ATTEMPT_TOKEN_MISMATCH");

        Reply paused = expect(fx.call("POST", path(attempt, "pause"), student, "{}"), 200);
        assertThat(paused.body().path("status").asString()).isEqualTo("paused");
        expect(fx.call("POST", path(attempt, "saveResponses"), student,
                "{\"attemptToken\":\"" + token + "\",\"clientSequence\":\"2\",\"answers\":[]}"), 409);
        expect(fx.call("POST", path(attempt, "resume"), student, "{}"), 200);

        // marked by hand: A is correct (6.00) and 4.2 lies within 4.0 ± 0.5 (4.00): 10 of 10, passed at 100 %
        Reply submitted = expect(fx.call("POST", path(attempt, "submit"), student,
                "{\"attemptToken\":\"" + token + "\",\"answers\":[{\"questionId\":\"" + q2
                        + "\",\"numericValue\":\"4.2\"}]}"), 200);
        assertThat(submitted.body().path("status").asString()).isEqualTo("graded");
        assertThat(submitted.body().path("score").asString()).isEqualTo("10.00");
        assertThat(submitted.body().path("percentage").asString()).isEqualTo("100.00");
        assertThat(submitted.body().path("correctCount").asInt()).isEqualTo(2);
        assertThat(fx.value(tenant, "SELECT auto_points FROM lms.gradebook_entry WHERE content_item_id = :item "
                + "AND user_id = :user", BigDecimal.class, "item", item, "user", studentId))
                .as("100 % of the item's 20 points").isEqualByComparingTo("20.00");

        Reply again = expect(fx.call("POST", path(attempt, "submit"), student,
                "{\"attemptToken\":\"" + token + "\",\"answers\":[]}"), 409);
        assertThat(again.body().path("detail").asString()).contains("already been submitted");
    }

    @Test
    void openEndedAnswersWaitForTheTeacherAndTheirMarksPostTheScore() throws Exception {
        UUID quiz = quiz(true);
        UUID q1 = choices("6");
        UUID q2 = question("open_ended", "4");
        place(quiz, q1, "6", 1);
        place(quiz, q2, "4", 2);
        UUID cohort = fx.cohort(tenant, institution);
        UUID item = fx.contentItem(tenant, cohort, "quiz", "quiz_id", quiz, "20");
        UUID attempt = attempt(quiz, item, cohort, studentId);

        Reply submitted = expect(fx.call("POST", path(attempt, "submit"), student,
                "{\"attemptToken\":\"" + token(attempt) + "\",\"answers\":[{\"questionId\":\"" + q1
                        + "\",\"selectedOptionIds\":[\"" + optionB(q1) + "\"]},{\"questionId\":\"" + q2
                        + "\",\"answerText\":\"An essay.\"}]}"), 200);
        assertThat(submitted.body().path("status").asString()).isEqualTo("submitted");
        assertThat(fx.value(tenant, "SELECT count(*) FROM lms.gradebook_entry WHERE content_item_id = :item",
                Integer.class, "item", item)).as("nothing posts while a teacher's marks are awaited").isZero();

        String grade = "/v1/lms/quiz-submissions/" + attempt + ":gradeAnswers";
        expect(fx.call("POST", grade, student, "{\"answers\":[]}"), 403);
        expect(fx.call("POST", grade, admin, "{\"answers\":[{\"questionId\":\"" + q2 + "\",\"marks\":\"9\"}]}"), 422);
        // marked by hand: B was wrong (0.00), the teacher gives 3 of 4: 3 of 10 is 30 %, 6.00 of the 20 points
        Reply graded = expect(fx.call("POST", grade, admin,
                "{\"answers\":[{\"questionId\":\"" + q2 + "\",\"marks\":\"3\",\"feedback\":\"Decent\"}]}"), 200);
        assertThat(graded.body().path("status").asString()).isEqualTo("graded");
        assertThat(graded.body().path("score").asString()).isEqualTo("3.00");
        assertThat(graded.body().path("percentage").asString()).isEqualTo("30.00");
        assertThat(fx.value(tenant, "SELECT auto_points FROM lms.gradebook_entry WHERE content_item_id = :item "
                + "AND user_id = :user", BigDecimal.class, "item", item, "user", studentId))
                .isEqualByComparingTo("6.00");
    }

    @Test
    void aStudentActsOnlyOnTheirOwnAttemptAndTenantsAreIsolated() throws Exception {
        UUID quiz = quiz(true);
        UUID q1 = choices("6");
        place(quiz, q1, "6", 1);
        UUID other = attempt(quiz, null, null, UUID.randomUUID());
        expect(fx.call("POST", path(other, "submit"), student,
                "{\"attemptToken\":\"" + token(other) + "\",\"answers\":[]}"), 404);
        expect(fx.call("POST", path(other, "pause"), student, "{}"), 404);
        String strangerStudent = new LmsFixtures(TOKENS, database, port)
                .user(UUID.randomUUID(), UUID.randomUUID(), institution, STUDENT_PERMISSIONS);
        expect(fx.call("POST", path(other, "submit"), strangerStudent,
                "{\"attemptToken\":\"" + token(other) + "\",\"answers\":[]}"), 404);
    }
}
