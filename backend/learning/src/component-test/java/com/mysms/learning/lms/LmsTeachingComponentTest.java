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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
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
 * The teaching side of lms against real PostgreSQL: enrolment approval, batch enrolment, course versions, evaluator
 * slots, certificates, the teacher dashboard and unread counts, the settings singleton, assignment groups with peer
 * review and grades, the repository, quiz attempts, and the operations whose file store is not reachable here (503).
 */
@SpringBootTest(classes = LearningApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LmsTeachingComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID SCHOOL = UUID.fromString("0196f000-0000-7000-8000-00000000c001");
    private static final UUID TEACHER = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID CLASSMATE = UUID.randomUUID();
    private static final UUID OUTSIDER = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, TEACHER, List.of("lms.*"));
    private static final String LEARNER = token(TENANT, STUDENT, List.of("lms.course.enroll_in_course",
            "lms.assignment_group.request_to_join_group", "lms.assignment_group.submit_peer_review",
            "lms.course.issue_my_certificate", "lms.quiz.start_quiz_attempt", "lms.forum.get_unread_counts",
            "lms.content_item.get_web_resource_launch", "lms.quiz_submission.save_quiz_responses",
            "lms.quiz.submit_quiz"));
    private static final String CLASSMATE_TOKEN = token(TENANT, CLASSMATE, List.of(
            "lms.assignment_group.request_to_join_group", "lms.assignment_group.submit_peer_review",
            "lms.course.enroll_in_course"));
    private static final String NOBODY = token(TENANT, OUTSIDER, List.of("hr.leave_application.read"));

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

    private static String token(UUID tenant, UUID user, List<String> permissions) {
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

        String code() {
            return body.path("code").asString();
        }

        String text(String field) {
            return body.path(field).asString();
        }
    }

    private Reply call(String method, String path, String token, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()));
    }

    private void sql(String... statements) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(TENANT, null);
        CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(() -> {
            for (String statement : statements) {
                database.jdbc().sql(statement).update();
            }
            return null;
        }));
    }

    private int count(String query) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(TENANT, null);
        return CallContextHolder.callAs(context, () -> TestTransactions.of(database).read(() ->
                database.jdbc().sql(query).query(Integer.class).single()));
    }

    // ---- fixtures

    private record Course(UUID id, UUID chapter) {
    }

    private Course publishedCourse(String title) throws Exception {
        Reply created = call("POST", "/v1/lms/courses", ADMIN, """
                {"title":"%s","institutionId":"%s"}""".formatted(title, SCHOOL));
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        UUID id = UUID.fromString(created.text("id"));
        sql("UPDATE lms.course SET status = 'approved', lifecycle_status = 'published', is_published = true,"
                + " row_version = row_version + 1 WHERE id = '" + id + "'");
        Reply chapter = call("POST", "/v1/lms/courses/" + id + "/chapters", ADMIN,
                "{\"title\":\"Week one\",\"sortOrder\":0}");
        assertThat(chapter.status()).as(chapter.body().toString()).isEqualTo(201);
        return new Course(id, UUID.fromString(chapter.text("id")));
    }

    private UUID cohortOf(UUID courseId) {
        UUID cohort = UUID.randomUUID();
        sql("INSERT INTO lms.cohort (id, tenant_id, institution_id, title, start_date, end_date, timezone)"
                + " VALUES ('" + cohort + "', '" + TENANT + "', '" + SCHOOL + "', 'Batch', '2026-01-01',"
                + " '2026-12-31', 'UTC')",
            "INSERT INTO lms.cohort_course (id, tenant_id, cohort_id, course_id) VALUES ('" + UUID.randomUUID()
                + "', '" + TENANT + "', '" + cohort + "', '" + courseId + "')");
        return cohort;
    }

    private UUID assignmentItem(Course course, UUID cohort) throws Exception {
        Reply assignment = call("POST", "/v1/lms/assignments", ADMIN, """
                {"title":"Essay","question":"Write.","submissionType":"text"}""");
        assertThat(assignment.status()).as(assignment.body().toString()).isEqualTo(201);
        Reply item = call("POST", "/v1/lms/content-items", ADMIN, """
                {"chapterId":"%s","courseId":"%s","itemKind":"assignment","assignmentId":"%s","title":"Essay"}"""
                .formatted(course.chapter(), course.id(), assignment.text("id")));
        assertThat(item.status()).as(item.body().toString()).isEqualTo(201);
        UUID itemId = UUID.fromString(item.text("id"));
        if (cohort != null) {
            sql("UPDATE lms.content_item SET cohort_id = '" + cohort + "' WHERE id = '" + itemId + "'");
        }
        return itemId;
    }

    private UUID groupOf(UUID itemId, UUID cohort) throws Exception {
        Reply group = call("POST", "/v1/lms/content-items/" + itemId + "/groups", ADMIN, """
                {"cohortId":"%s","name":"Group A"}""".formatted(cohort));
        assertThat(group.status()).as(group.body().toString()).isEqualTo(201);
        return UUID.fromString(group.text("id"));
    }

    private void enrol(String token, UUID courseId, int expected) throws Exception {
        Reply enrolled = call("POST", "/v1/lms/courses/" + courseId + ":enroll", token, "{}");
        assertThat(enrolled.status()).as(enrolled.body().toString()).isEqualTo(expected);
    }

    // ---- enrolment approval and batch enrolment

    @Test
    void anEnrolmentRequestIsApprovedIntoACohortOnce() throws Exception {
        Course course = publishedCourse("Approval");
        UUID cohort = cohortOf(course.id());
        Reply requested = call("POST", "/v1/lms/courses/" + course.id() + "/enrollment-requests",
                token(TENANT, STUDENT, List.of("lms.enrollment_request.create")), """
                {"courseId":"%s"}""".formatted(course.id()));
        assertThat(requested.status()).as(requested.body().toString()).isEqualTo(201);
        String requestPath = "/v1/lms/courses/" + course.id() + "/enrollment-requests/" + requested.text("id");

        Reply wrongCohort = call("POST", requestPath + ":approve", ADMIN,
                "{\"cohortId\":\"" + UUID.randomUUID() + "\"}");
        assertThat(wrongCohort.status()).as(wrongCohort.body().toString()).isEqualTo(422);

        Reply approved = call("POST", requestPath + ":approve", ADMIN, "{\"cohortId\":\"" + cohort + "\"}");
        assertThat(approved.status()).as(approved.body().toString()).isEqualTo(200);
        assertThat(approved.text("status")).isEqualTo("approved");
        assertThat(approved.text("decidedBy")).isEqualTo(TEACHER.toString());
        assertThat(count("SELECT count(*) FROM lms.cohort_enrollment WHERE cohort_id = '" + cohort
                + "' AND user_id = '" + STUDENT + "'")).isEqualTo(1);

        Reply again = call("POST", requestPath + ":approve", ADMIN, "{\"cohortId\":\"" + cohort + "\"}");
        assertThat(again.status()).as("a decided request stays decided").isEqualTo(409);

        Reply unpermitted = call("POST", requestPath + ":approve", NOBODY, "{\"cohortId\":\"" + cohort + "\"}");
        assertThat(unpermitted.status()).isEqualTo(403);
    }

    @Test
    void aBatchEnrolmentPreviewsAndEnrolsAndAPaidCourseWaitsForPayment() throws Exception {
        Course course = publishedCourse("Batch");
        enrol(LEARNER, course.id(), 200);
        Reply preview = call("POST", "/v1/lms/courses/" + course.id() + ":batchEnroll", ADMIN, """
                {"userIds":["%s","%s"],"validateOnly":true}""".formatted(STUDENT, CLASSMATE),
                "Idempotency-Key", UUID.randomUUID().toString());
        assertThat(preview.status()).as(preview.body().toString()).isEqualTo(200);
        assertThat(preview.body().path("items").size()).isEqualTo(2);
        assertThat(preview.body().path("items").get(0).path("alreadyEnrolled").asBoolean()).isTrue();
        assertThat(count("SELECT count(*) FROM lms.enrollment WHERE course_id = '" + course.id() + "'"))
                .as("a preview writes nothing").isEqualTo(1);

        Reply enrolled = call("POST", "/v1/lms/courses/" + course.id() + ":batchEnroll", ADMIN, """
                {"userIds":["%s","%s"]}""".formatted(STUDENT, CLASSMATE),
                "Idempotency-Key", UUID.randomUUID().toString());
        assertThat(enrolled.status()).as(enrolled.body().toString()).isEqualTo(200);
        assertThat(count("SELECT count(*) FROM lms.enrollment WHERE course_id = '" + course.id() + "'")).isEqualTo(2);

        sql("UPDATE lms.course SET is_paid = true, price = 499.00, currency_id = '" + UUID.randomUUID()
                + "', row_version = row_version + 1 WHERE id = '" + course.id() + "'");
        UUID stranger = UUID.randomUUID();
        Reply paid = call("POST", "/v1/lms/courses/" + course.id() + ":batchEnroll", ADMIN, """
                {"userIds":["%s"]}""".formatted(stranger), "Idempotency-Key", UUID.randomUUID().toString());
        assertThat(paid.status()).isEqualTo(200);
        assertThat(paid.body().path("items").get(0).path("paymentRequired").asBoolean()).isTrue();
        assertThat(count("SELECT count(*) FROM lms.enrollment WHERE user_id = '" + stranger + "'")).isEqualTo(0);

        Reply empty = call("POST", "/v1/lms/courses/" + course.id() + ":batchEnroll", ADMIN, "{\"userIds\":[]}",
                "Idempotency-Key", UUID.randomUUID().toString());
        assertThat(empty.status()).isEqualTo(422);
    }

    // ---- course versions

    @Test
    void aCourseVersionCopiesTheStructureAsAFreshDraft() throws Exception {
        Course course = publishedCourse("Versioned");
        Reply lesson = call("POST", "/v1/lms/lessons", ADMIN, """
                {"chapterId":"%s","title":"Lesson","sortOrder":0}""".formatted(course.chapter()));
        assertThat(lesson.status()).as(lesson.body().toString()).isEqualTo(201);
        Reply item = call("POST", "/v1/lms/content-items", ADMIN, """
                {"chapterId":"%s","courseId":"%s","itemKind":"lesson","lessonId":"%s","title":"Lesson"}"""
                .formatted(course.chapter(), course.id(), lesson.text("id")));
        assertThat(item.status()).as(item.body().toString()).isEqualTo(201);

        Reply version = call("POST", "/v1/lms/courses/" + course.id() + ":createVersion", ADMIN,
                "{\"versionKind\":\"major\",\"authorComments\":\"Second run\"}");
        assertThat(version.status()).as(version.body().toString()).isEqualTo(201);
        UUID next = UUID.fromString(version.text("id"));
        assertThat(version.body().path("versionMajor").asInt()).isEqualTo(2);
        assertThat(version.body().path("versionMinor").asInt()).isEqualTo(0);
        assertThat(version.text("previousVersionId")).isEqualTo(course.id().toString());
        assertThat(version.text("rootCourseId")).isEqualTo(course.id().toString());
        assertThat(version.text("lifecycleStatus")).isEqualTo("draft");
        assertThat(count("SELECT count(*) FROM lms.chapter WHERE course_id = '" + next + "'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM lms.content_item WHERE course_id = '" + next + "'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM lms.lesson l JOIN lms.chapter c ON c.id = l.chapter_id"
                + " WHERE c.course_id = '" + next + "'")).as("the lesson was copied, not shared").isEqualTo(1);
    }

    // ---- item kinds

    @Test
    void anUpdateTurnsAnItemIntoAProgrammingExercise() throws Exception {
        Course course = publishedCourse("Exercise kind");
        Reply lesson = call("POST", "/v1/lms/lessons", ADMIN, """
                {"chapterId":"%s","title":"Lesson","sortOrder":0}""".formatted(course.chapter()));
        assertThat(lesson.status()).as(lesson.body().toString()).isEqualTo(201);
        Reply item = call("POST", "/v1/lms/content-items", ADMIN, """
                {"chapterId":"%s","courseId":"%s","itemKind":"lesson","lessonId":"%s","title":"Lesson"}"""
                .formatted(course.chapter(), course.id(), lesson.text("id")));
        assertThat(item.status()).as(item.body().toString()).isEqualTo(201);
        UUID exercise = UUID.randomUUID();
        sql("INSERT INTO lms.programming_exercise (id, tenant_id, title, problem_statement, language) VALUES ('"
                + exercise + "', '" + TENANT + "', 'FizzBuzz', 'Print the numbers.', 'python')");

        Reply changed = call("PATCH", "/v1/lms/content-items/" + item.text("id"), ADMIN, """
                {"itemKind":"exercise","exerciseId":"%s","lessonId":null}""".formatted(exercise),
                "If-Match", item.text("etag"));
        assertThat(changed.status()).as(changed.body().toString()).isEqualTo(200);
        assertThat(changed.text("itemKind")).isEqualTo("exercise");
        assertThat(changed.text("exerciseId")).isEqualTo(exercise.toString());
    }

    // ---- evaluator slots and certificates

    @Test
    void evaluatorSlotsExpandAvailabilityWithoutBookedOnes() throws Exception {
        Course course = publishedCourse("Evaluated");
        UUID evaluator = UUID.randomUUID();
        UUID evaluatorUser = UUID.randomUUID();
        LocalDate monday = LocalDate.now().plusDays(1);
        while (monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            monday = monday.plusDays(1);
        }
        sql("INSERT INTO lms.evaluator (id, tenant_id, user_id) VALUES ('" + evaluator + "', '" + TENANT + "', '"
                + evaluatorUser + "')",
            "INSERT INTO lms.evaluator_availability (id, tenant_id, evaluator_id, weekday, start_time, end_time)"
                + " VALUES ('" + UUID.randomUUID() + "', '" + TENANT + "', '" + evaluator + "', 'monday',"
                + " '09:00', '10:00')",
            "INSERT INTO lms.evaluator_availability (id, tenant_id, evaluator_id, weekday, start_time, end_time)"
                + " VALUES ('" + UUID.randomUUID() + "', '" + TENANT + "', '" + evaluator + "', 'monday',"
                + " '10:00', '11:00')",
            "UPDATE lms.course SET default_evaluator_id = '" + evaluator + "', row_version = row_version + 1"
                + " WHERE id = '" + course.id() + "'",
            "INSERT INTO lms.certificate_request (id, tenant_id, course_id, user_id, evaluator_user_id,"
                + " request_date, start_time, end_time, status) VALUES ('" + UUID.randomUUID() + "', '" + TENANT
                + "', '" + course.id() + "', '" + STUDENT + "', '" + evaluatorUser + "', '" + monday
                + "', '09:00', '10:00', 'upcoming')");
        Reply slots = call("GET", "/v1/lms/courses/" + course.id() + ":evaluatorSlots?fromDate=" + monday
                + "&toDate=" + monday, ADMIN, null);
        assertThat(slots.status()).as(slots.body().toString()).isEqualTo(200);
        assertThat(slots.body().path("data").size()).as("the booked slot is left out").isEqualTo(1);
        assertThat(slots.body().path("data").get(0).path("startTime").asString()).startsWith("10:00");

        Reply tooWide = call("GET", "/v1/lms/courses/" + course.id() + ":evaluatorSlots?fromDate=" + monday
                + "&toDate=" + monday.plusDays(200), ADMIN, null);
        assertThat(tooWide.status()).isEqualTo(422);
    }

    @Test
    void aCertificateIsIssuedOnceTheCourseIsComplete() throws Exception {
        Course course = publishedCourse("Certified");
        sql("UPDATE lms.course SET certification_enabled = true, row_version = row_version + 1 WHERE id = '"
                + course.id() + "'");
        enrol(LEARNER, course.id(), 200);
        Reply early = call("POST", "/v1/lms/courses/" + course.id() + ":issueMyCertificate", LEARNER, "{}");
        assertThat(early.status()).isEqualTo(409);
        assertThat(early.code()).isEqualTo("COURSE_NOT_COMPLETE");

        sql("UPDATE lms.enrollment SET progress_percent = 100, row_version = row_version + 1 WHERE course_id = '"
                + course.id() + "' AND user_id = '" + STUDENT + "'");
        Reply issued = call("POST", "/v1/lms/courses/" + course.id() + ":issueMyCertificate", LEARNER, "{}");
        assertThat(issued.status()).as(issued.body().toString()).isEqualTo(200);
        assertThat(issued.text("userId")).isEqualTo(STUDENT.toString());
        Reply again = call("POST", "/v1/lms/courses/" + course.id() + ":issueMyCertificate", LEARNER, "{}");
        assertThat(again.status()).isEqualTo(200);
        assertThat(again.text("id")).as("issuing twice returns the same certificate").isEqualTo(issued.text("id"));

        sql("UPDATE lms.course SET paid_certificate = true, row_version = row_version + 1 WHERE id = '"
                + course.id() + "'",
            "DELETE FROM lms.certificate WHERE course_id = '" + course.id() + "'");
        Reply unpaid = call("POST", "/v1/lms/courses/" + course.id() + ":issueMyCertificate", LEARNER, "{}");
        assertThat(unpaid.status()).isEqualTo(409);
        assertThat(unpaid.code()).isEqualTo("PAYMENT_REQUIRED");
    }

    // ---- the teacher dashboard and unread counts

    @Test
    void theTeacherDashboardCountsGradingUnreadPostsAndAlerts() throws Exception {
        Course course = publishedCourse("Taught");
        UUID cohort = cohortOf(course.id());
        UUID item = assignmentItem(course, cohort);
        UUID forum = UUID.randomUUID();
        UUID section = UUID.randomUUID();
        UUID topic = UUID.randomUUID();
        UUID post = UUID.randomUUID();
        sql("INSERT INTO lms.cohort_teacher (id, tenant_id, cohort_id, user_id) VALUES ('" + UUID.randomUUID()
                + "', '" + TENANT + "', '" + cohort + "', '" + TEACHER + "')",
            "INSERT INTO lms.assignment_submission (id, tenant_id, assignment_id, user_id, content_item_id,"
                + " finalized_at) SELECT gen_random_uuid(), tenant_id, assignment_id, '" + STUDENT + "', id, now()"
                + " FROM lms.content_item WHERE id = '" + item + "'",
            "INSERT INTO lms.forum (id, tenant_id, cohort_id, title) VALUES ('" + forum + "', '" + TENANT + "', '"
                + cohort + "', 'General')",
            "INSERT INTO lms.forum_section (id, tenant_id, forum_id, title) VALUES ('" + section + "', '" + TENANT
                + "', '" + forum + "', 'Week 1')",
            "INSERT INTO lms.forum_topic (id, tenant_id, forum_section_id, title) VALUES ('" + topic + "', '"
                + TENANT + "', '" + section + "', 'Hello')",
            "INSERT INTO lms.forum_post (id, tenant_id, forum_topic_id, author_user_id, body) VALUES ('"
                + post + "', '" + TENANT + "', '" + topic + "', '" + STUDENT + "', 'Hi there')",
            "INSERT INTO lms.student_alert (id, tenant_id, user_id, cohort_id, subject) VALUES ('"
                + UUID.randomUUID() + "', '" + TENANT + "', '" + STUDENT + "', '" + cohort + "', 'Falling behind')");
        Reply dashboard = call("GET", "/v1/lms/me/teacher-dashboard", ADMIN, null);
        assertThat(dashboard.status()).as(dashboard.body().toString()).isEqualTo(200);
        assertThat(dashboard.body().path("toGrade").asInt()).isEqualTo(1);
        assertThat(dashboard.body().path("unreadPosts").asInt()).isEqualTo(1);
        assertThat(dashboard.body().path("cohorts").size()).isEqualTo(1);
        assertThat(dashboard.body().path("alerts").size()).isEqualTo(1);

        // read marks as the forum writes them: a topic-wide mark older than the post leaves it unread; the post's own
        // row (marked read or flagged one by one) makes it read, however many rows the topic has
        sql("INSERT INTO lms.forum_read_state (id, tenant_id, user_id, forum_topic_id, read_at) VALUES ('"
                + UUID.randomUUID() + "', '" + TENANT + "', '" + TEACHER + "', '" + topic + "', '2000-01-01T00:00:00Z')");
        assertThat(call("GET", "/v1/lms/me/teacher-dashboard", ADMIN, null).body().path("unreadPosts").asInt())
                .isEqualTo(1);
        sql("INSERT INTO lms.forum_read_state (id, tenant_id, user_id, forum_topic_id, forum_post_id, is_flagged)"
                + " VALUES ('" + UUID.randomUUID() + "', '" + TENANT + "', '" + TEACHER + "', '" + topic + "', '" + post
                + "', true)");
        assertThat(call("GET", "/v1/lms/me/teacher-dashboard", ADMIN, null).body().path("unreadPosts").asInt())
                .isEqualTo(0);

        sql("INSERT INTO lms.cohort_enrollment (id, tenant_id, cohort_id, user_id) VALUES ('" + UUID.randomUUID()
                + "', '" + TENANT + "', '" + cohort + "', '" + STUDENT + "')");
        Reply counts = call("GET", "/v1/lms/me/unread-counts", LEARNER, null);
        assertThat(counts.status()).as(counts.body().toString()).isEqualTo(200);
        assertThat(counts.body().path("forumPosts").asInt()).as("own posts are not unread").isEqualTo(0);
        Reply classmateCounts = call("GET", "/v1/lms/me/teacher-dashboard", ADMIN, null);
        assertThat(classmateCounts.status()).isEqualTo(200);
    }

    // ---- the settings singleton

    @Test
    void theSettingsSingletonStartsFromDefaultsAndKeepsItsSecretMasked() throws Exception {
        Reply unsaved = call("GET", "/v1/lms/settings", ADMIN, null);
        assertThat(unsaved.status()).as(unsaved.body().toString()).isEqualTo(200);
        assertThat(unsaved.body().path("allowGuestAccess").asBoolean()).isTrue();
        assertThat(unsaved.body().path("id").isMissingNode() || unsaved.body().path("id").isNull())
                .as("no row yet").isTrue();
        assertThat(unsaved.text("etag")).isEqualTo("\"0\"");

        Reply stale = call("PATCH", "/v1/lms/settings", ADMIN,
                "{\"disableSignup\":false}", "If-Match", "\"7\"");
        assertThat(stale.status()).isEqualTo(412);

        Reply saved = call("PATCH", "/v1/lms/settings", ADMIN,
                "{\"disableSignup\":false,\"unsplashAccessKey\":\"key-123456\"}", "If-Match", "\"0\"");
        assertThat(saved.status()).as(saved.body().toString()).isEqualTo(200);
        assertThat(saved.body().path("disableSignup").asBoolean()).isFalse();
        assertThat(saved.body().path("unsplashAccessKey").isMissingNode()
                || saved.body().path("unsplashAccessKey").isNull()).as("the plaintext never returns").isTrue();
        assertThat(saved.text("unsplashAccessKeyMasked")).endsWith("3456");

        Reply unpermitted = call("PATCH", "/v1/lms/settings", NOBODY, "{\"disableSignup\":true}",
                "If-Match", saved.text("etag"));
        assertThat(unpermitted.status()).isEqualTo(403);
    }

    // ---- groups, peer review, grades

    @Test
    void aGroupTakesRequestsMembersPeerReviewsAndFinalGrades() throws Exception {
        Course course = publishedCourse("Grouped");
        UUID cohort = cohortOf(course.id());
        UUID item = assignmentItem(course, cohort);
        UUID group = groupOf(item, cohort);
        enrol(LEARNER, course.id(), 200);
        enrol(CLASSMATE_TOKEN, course.id(), 200);
        String base = "/v1/lms/content-items/" + item + "/groups/" + group;

        Reply requested = call("POST", base + ":requestToJoin", LEARNER, "{}");
        assertThat(requested.status()).as(requested.body().toString()).isEqualTo(200);
        assertThat(requested.text("status")).isEqualTo("requested");
        Reply again = call("POST", base + ":requestToJoin", LEARNER, "{}");
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.code()).isEqualTo("ALREADY_REQUESTED");

        Reply members = call("POST", base + ":setMembers", ADMIN, """
                {"addUserIds":["%s","%s"]}""".formatted(STUDENT, CLASSMATE));
        assertThat(members.status()).as(members.body().toString()).isEqualTo(200);
        assertThat(members.body().path("items").size()).isEqualTo(2);
        assertThat(members.body().path("items").get(0).path("status").asString()).isEqualTo("active");

        Reply self = call("POST", base + ":submitPeerReview", LEARNER, """
                {"reviews":[{"userId":"%s","points":"5.00"}]}""".formatted(STUDENT));
        assertThat(self.status()).as("reviewing oneself").isEqualTo(422);
        Reply review = call("POST", base + ":submitPeerReview", LEARNER, """
                {"reviews":[{"userId":"%s","points":"4.00"}]}""".formatted(CLASSMATE));
        assertThat(review.status()).as(review.body().toString()).isEqualTo(200);
        Reply reviewBack = call("POST", base + ":submitPeerReview", CLASSMATE_TOKEN, """
                {"reviews":[{"userId":"%s","points":"3.00"}]}""".formatted(STUDENT));
        assertThat(reviewBack.status()).as(reviewBack.body().toString()).isEqualTo(200);

        Reply finalized = call("POST", base + ":finalizePeerReview", ADMIN, "{}");
        assertThat(finalized.status()).as(finalized.body().toString()).isEqualTo(200);
        assertThat(finalized.body().path("items").size()).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM lms.gradebook_entry WHERE content_item_id = '" + item
                + "' AND peer_points IS NOT NULL")).isEqualTo(2);

        sql("UPDATE lms.gradebook_entry SET status = 'graded', points = 8, row_version = row_version + 1"
                + " WHERE content_item_id = '" + item + "' AND user_id = '" + STUDENT + "'");
        Reply published = call("POST", "/v1/lms/content-items/" + item + ":publishGrades", ADMIN, "{}");
        assertThat(published.status()).as(published.body().toString()).isEqualTo(200);
        assertThat(published.body().path("items").size()).as("only graded entries publish").isEqualTo(1);
        assertThat(published.body().path("items").get(0).path("isPublished").asBoolean()).isTrue();

        Reply reassigned = call("POST", "/v1/lms/content-items/" + item + ":reassign", ADMIN, """
                {"userIds":["%s"],"comment":"One more try"}""".formatted(STUDENT));
        assertThat(reassigned.status()).as(reassigned.body().toString()).isEqualTo(200);
        assertThat(reassigned.body().path("items").size()).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM lms.gradebook_entry WHERE content_item_id = '" + item
                + "' AND status = 'reassigned'")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM lms.reassignment_log WHERE content_item_id = '" + item + "'"))
                .isEqualTo(1);

        Reply neither = call("POST", "/v1/lms/content-items/" + item + ":reassign", ADMIN, "{}");
        assertThat(neither.status()).isEqualTo(422);
    }

    // ---- copies and the repository

    @Test
    void anItemCopyDuplicatesAPrivateAssignmentAndTheRepositoryPlacesSharedOnes() throws Exception {
        Course course = publishedCourse("Copied");
        UUID item = assignmentItem(course, null);
        Reply second = call("POST", "/v1/lms/courses/" + course.id() + "/chapters", ADMIN,
                "{\"title\":\"Week two\",\"sortOrder\":1}");
        assertThat(second.status()).isEqualTo(201);
        UUID target = UUID.fromString(second.text("id"));

        Reply copied = call("POST", "/v1/lms/content-items/" + item + ":copy", ADMIN,
                "{\"targetChapterId\":\"" + target + "\"}");
        assertThat(copied.status()).as(copied.body().toString()).isEqualTo(201);
        assertThat(copied.text("chapterId")).isEqualTo(target.toString());
        assertThat(copied.body().path("isOriginal").asBoolean()).isFalse();
        assertThat(count("SELECT count(*) FROM lms.assignment")).as("the private assignment was duplicated")
                .isEqualTo(2);

        Reply quiz = call("POST", "/v1/lms/quizzes", ADMIN, """
                {"title":"Shared quiz","passingPercentage":50,"totalMarks":"10.00"}""");
        assertThat(quiz.status()).as(quiz.body().toString()).isEqualTo(201);
        UUID quizId = UUID.fromString(quiz.text("id"));
        Reply notShared = call("POST", "/v1/lms/content-items:addFromRepository", ADMIN, """
                {"chapterId":"%s","itemKind":"quiz","sourceIds":["%s"]}""".formatted(target, quizId));
        assertThat(notShared.status()).as("only repository items place").isEqualTo(422);

        sql("UPDATE lms.quiz SET is_in_repository = true, row_version = row_version + 1 WHERE id = '" + quizId
                + "'");
        Reply placed = call("POST", "/v1/lms/content-items:addFromRepository", ADMIN, """
                {"chapterId":"%s","itemKind":"quiz","sourceIds":["%s"]}""".formatted(target, quizId));
        assertThat(placed.status()).as(placed.body().toString()).isEqualTo(200);
        assertThat(placed.body().path("items").size()).isEqualTo(1);
        assertThat(placed.body().path("items").get(0).path("quizId").asString()).isEqualTo(quizId.toString());
    }

    // ---- quiz attempts

    @Test
    void aQuizAttemptOpensWithinItsLimitsAndResumes() throws Exception {
        Course course = publishedCourse("Quizzed");
        Reply quiz = call("POST", "/v1/lms/quizzes", ADMIN, """
                {"title":"Checks","passingPercentage":50,"totalMarks":"10.00","maxAttempts":1}""");
        assertThat(quiz.status()).as(quiz.body().toString()).isEqualTo(201);
        UUID quizId = UUID.fromString(quiz.text("id"));
        UUID question = UUID.randomUUID();
        UUID option = UUID.randomUUID();
        sql("INSERT INTO lms.question (id, tenant_id, question_text, question_type) VALUES ('" + question + "', '"
                + TENANT + "', 'Is water wet?', 'choices')",
            "INSERT INTO lms.question_option (id, tenant_id, question_id, option_text, is_correct, sort_order)"
                + " VALUES ('" + option + "', '" + TENANT + "', '" + question + "', 'Yes', true, 0)",
            "INSERT INTO lms.quiz_question (id, tenant_id, quiz_id, question_id, marks, sort_order) VALUES ('"
                + UUID.randomUUID() + "', '" + TENANT + "', '" + quizId + "', '" + question + "', 5.00, 0)");
        Reply item = call("POST", "/v1/lms/content-items", ADMIN, """
                {"chapterId":"%s","courseId":"%s","itemKind":"quiz","quizId":"%s","title":"Checks"}"""
                .formatted(course.chapter(), course.id(), quizId));
        assertThat(item.status()).as(item.body().toString()).isEqualTo(201);
        UUID itemId = UUID.fromString(item.text("id"));
        enrol(LEARNER, course.id(), 200);

        Reply attempt = call("POST", "/v1/lms/content-items/" + itemId + ":startQuizAttempt", LEARNER, "{}");
        assertThat(attempt.status()).as(attempt.body().toString()).isEqualTo(200);
        assertThat(attempt.body().path("questions").size()).isEqualTo(1);
        JsonNode view = attempt.body().path("questions").get(0);
        assertThat(view.path("options").size()).isEqualTo(1);
        assertThat(view.path("options").get(0).has("isCorrect")).as("answers never travel").isFalse();
        assertThat(attempt.body().path("attemptNumber").asInt()).isEqualTo(1);

        Reply resumed = call("POST", "/v1/lms/content-items/" + itemId + ":startQuizAttempt", LEARNER, "{}");
        assertThat(resumed.status()).isEqualTo(200);
        assertThat(resumed.text("quizSubmissionId")).as("the open attempt resumes")
                .isEqualTo(attempt.text("quizSubmissionId"));

        // the token the start hands out is the one saving and submitting check, over the delivered question set
        assertThat(resumed.text("attemptToken")).isEqualTo(attempt.text("attemptToken"));
        String steps = "/v1/lms/quiz-submissions/" + attempt.text("quizSubmissionId") + ":";
        String answer = "\"answers\":[{\"questionId\":\"" + question + "\",\"selectedOptionIds\":[\"" + option + "\"]}]";
        Reply saved = call("POST", steps + "saveResponses", LEARNER, "{\"attemptToken\":\"" + attempt.text("attemptToken")
                + "\",\"clientSequence\":\"1\"," + answer + "}");
        assertThat(saved.status()).as(saved.body().toString()).isEqualTo(200);
        assertThat(saved.body().path("savedCount").asInt()).isEqualTo(1);
        Reply submitted = call("POST", steps + "submit", LEARNER, "{\"attemptToken\":\"" + attempt.text("attemptToken")
                + "\"," + answer + "}");
        assertThat(submitted.status()).as(submitted.body().toString()).isEqualTo(200);
        assertThat(submitted.body().path("score").asString()).as("5 of the delivered question's 5 marks")
                .isEqualTo("5.00");
        assertThat(submitted.body().path("percentage").asString()).isEqualTo("100.00");
        Reply exhausted = call("POST", "/v1/lms/content-items/" + itemId + ":startQuizAttempt", LEARNER, "{}");
        assertThat(exhausted.status()).isEqualTo(409);
        assertThat(exhausted.code()).isEqualTo("NO_ATTEMPTS_LEFT");

        Reply outsider = call("POST", "/v1/lms/content-items/" + itemId + ":startQuizAttempt",
                token(TENANT, OUTSIDER, List.of("lms.quiz.start_quiz_attempt")), "{}");
        assertThat(outsider.status()).as("not a learner of the placement").isEqualTo(404);
    }

    // ---- launches and the unreachable file store

    @Test
    void aLinkItemLaunchesAndFileBackedLaunchesNeedTheFileStore() throws Exception {
        Course course = publishedCourse("Launched");
        Reply link = call("POST", "/v1/lms/content-items", ADMIN, """
                {"chapterId":"%s","courseId":"%s","itemKind":"link","linkUrl":"https://example.org/read","title":"Read"}"""
                .formatted(course.chapter(), course.id()));
        assertThat(link.status()).as(link.body().toString()).isEqualTo(201);
        enrol(LEARNER, course.id(), 200);
        Reply launched = call("GET", "/v1/lms/content-items/" + link.text("id") + ":launch", LEARNER, null);
        assertThat(launched.status()).as(launched.body().toString()).isEqualTo(200);
        assertThat(launched.text("url")).isEqualTo("https://example.org/read");

        UUID file = UUID.randomUUID();
        Reply fileItem = call("POST", "/v1/lms/content-items", ADMIN, """
                {"chapterId":"%s","courseId":"%s","itemKind":"file","fileId":"%s","title":"Notes"}"""
                .formatted(course.chapter(), course.id(), file));
        assertThat(fileItem.status()).as(fileItem.body().toString()).isEqualTo(201);
        Reply unreachable = call("GET", "/v1/lms/content-items/" + fileItem.text("id") + ":launch", LEARNER, null);
        assertThat(unreachable.status()).as("platform is not reachable here").isEqualTo(503);

        Reply lesson = call("POST", "/v1/lms/lessons", ADMIN, """
                {"chapterId":"%s","title":"L","sortOrder":0}""".formatted(course.chapter()));
        Reply lessonItem = call("POST", "/v1/lms/content-items", ADMIN, """
                {"chapterId":"%s","courseId":"%s","itemKind":"lesson","lessonId":"%s","title":"L"}"""
                .formatted(course.chapter(), course.id(), lesson.text("id")));
        Reply notLaunchable = call("GET", "/v1/lms/content-items/" + lessonItem.text("id") + ":launch", LEARNER,
                null);
        assertThat(notLaunchable.status()).isEqualTo(409);
        assertThat(notLaunchable.code()).isEqualTo("NOT_LAUNCHABLE");
    }

    @Test
    void theFileStoreOperationsAnswer503WhileThePlatformIsUnreachable() throws Exception {
        Course course = publishedCourse("Packed");
        Reply syllabus = call("GET", "/v1/lms/courses/" + course.id() + ":syllabus", ADMIN, null);
        assertThat(syllabus.status()).as(syllabus.body().toString()).isEqualTo(503);
        Reply badFormat = call("GET", "/v1/lms/courses/" + course.id() + ":syllabus?format=pdf", ADMIN, null);
        assertThat(badFormat.status()).as("only html is generated").isEqualTo(422);

        Reply export = call("POST", "/v1/lms/courses/" + course.id() + ":export", ADMIN, "{}");
        assertThat(export.status()).as(export.body().toString()).isEqualTo(503);
        assertThat(count("SELECT count(*) FROM lms.operation WHERE operation_name = 'lms.ExportCourse'"
                + " AND status = 'failed'")).as("the operation records the failure").isEqualTo(1);

        UUID item = assignmentItem(course, null);
        Reply download = call("POST", "/v1/lms/content-items/" + item + ":downloadSubmissions", ADMIN, "{}");
        assertThat(download.status()).as(download.body().toString()).isEqualTo(503);
    }

    // ---- isolation

    @Test
    void anotherTenantsRowsAreInvisible() throws Exception {
        Course course = publishedCourse("Private");
        String otherAdmin = token(UUID.randomUUID(), UUID.randomUUID(), List.of("lms.*"));
        Reply unseen = call("POST", "/v1/lms/courses/" + course.id() + ":createVersion", otherAdmin,
                "{\"versionKind\":\"minor\"}");
        assertThat(unseen.status()).as(unseen.body().toString()).isEqualTo(404);
        Reply settings = call("GET", "/v1/lms/settings", otherAdmin, null);
        assertThat(settings.status()).isEqualTo(200);
        assertThat(settings.text("etag")).as("the other tenant sees only the defaults").isEqualTo("\"0\"");
    }
}
