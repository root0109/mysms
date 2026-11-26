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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
 * The cohort workflows over HTTP against real PostgreSQL as the application role (row-level security on): membership
 * (self-enrolment with seat locking, batch enrolment, setting members, the roster, the access window), announcements,
 * imports and exports, the syllabus, live-class invite retries, member profiles and the tenant's LMS settings. Each
 * test works in a tenant of its own.
 *
 * <p>This module's test runs lms alone: the parts that need another service (iam for names, platform for files)
 * answer 503 here, after every check lms makes itself.
 */
@SpringBootTest(classes = LearningApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LmsCohortWorkflowComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String COHORTS = "/v1/lms/cohorts/";

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
        String code() {
            return body.path("code").asString();
        }
    }

    /** One cohort of one tenant with its teacher, and helpers that put rows in place as the tenant. */
    private final class Classroom {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final UUID cohort = UUID.randomUUID();
        final UUID teacherUser = UUID.randomUUID();
        final String admin = token(tenant, UUID.randomUUID(), List.of("*"), List.of("*"), "*");
        final String teacher;

        Classroom() {
            sql("INSERT INTO lms.cohort (id, tenant_id, institution_id, title, start_date, end_date, timezone, "
                    + "is_published, allow_self_enrollment, lifecycle_status, seat_count, row_version) "
                    + "VALUES (:a, :tenantId, :b, 'Algebra 2026', DATE '2026-01-10', DATE '2026-06-20', "
                    + "'Asia/Kolkata', true, true, 'active', 10, 1)", cohort, institution);
            sql("INSERT INTO lms.cohort_teacher (id, tenant_id, cohort_id, user_id, is_active, row_version) "
                    + "VALUES (:a, :tenantId, :b, :c, true, 1)", UUID.randomUUID(), cohort, teacherUser);
            teacher = token(tenant, teacherUser, List.of(institution.toString()), List.of(), "*");
        }

        void set(String assignment) {
            sql("UPDATE lms.cohort SET " + assignment + " WHERE tenant_id = :tenantId AND id = :a", cohort);
        }

        UUID student(boolean active) {
            UUID user = UUID.randomUUID();
            sql("INSERT INTO lms.cohort_enrollment (id, tenant_id, cohort_id, user_id, is_active, row_version) "
                    + "VALUES (:a, :tenantId, :b, :c, " + active + ", 1)", UUID.randomUUID(), cohort, user);
            return user;
        }

        String studentToken(UUID user, String... permissions) {
            return token(tenant, user, List.of(institution.toString()), List.of(), permissions);
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

    // ---- self-enrolment

    @Test
    void selfEnrolmentChecksTheGateThenTakesASeat() throws Exception {
        Classroom room = new Classroom();
        UUID learner = UUID.randomUUID();
        String student = room.studentToken(learner, "lms.cohort.enroll_in_cohort", "lms.cohort.read");
        String enroll = COHORTS + room.cohort + ":enroll";

        room.set("is_published = false");
        assertThat(call("POST", enroll, student, "{}").status()).as("unpublished is invisible").isEqualTo(404);
        room.set("is_published = true, lifecycle_status = 'completed'");
        assertThat(call("POST", enroll, student, "{}").code()).isEqualTo("COHORT_NOT_OPEN");
        room.set("lifecycle_status = 'active', allow_self_enrollment = false");
        assertThat(call("POST", enroll, student, "{}").code()).isEqualTo("SELF_ENROLLMENT_CLOSED");
        room.set("allow_self_enrollment = true, enrollment_opens_at = now() + interval '1 day'");
        assertThat(call("POST", enroll, student, "{}").code()).isEqualTo("ENROLLMENT_CLOSED");
        room.set("enrollment_opens_at = NULL, is_paid = true");
        assertThat(call("POST", enroll, student, "{}").code()).isEqualTo("PAYMENT_REQUIRED");
        room.set("is_paid = false, seat_count = 1");
        room.student(true);
        assertThat(call("POST", enroll, student, "{}").code()).isEqualTo("COHORT_FULL");

        room.set("seat_count = 3");
        Reply enrolled = call("POST", enroll, student, "{}");
        assertThat(enrolled.status()).as(enrolled.text()).isEqualTo(200);
        assertThat(enrolled.body().path("userId").asString()).isEqualTo(learner.toString());
        assertThat(enrolled.body().path("isSelfEnrolled").asBoolean()).isTrue();
        assertThat(call("POST", enroll, student, "{}").code()).isEqualTo("ALREADY_ENROLLED");

        // a student who left comes back on their own row
        room.sql("UPDATE lms.cohort_enrollment SET is_active = false, removed_at = now() "
                + "WHERE tenant_id = :tenantId AND user_id = :a", learner);
        Reply back = call("POST", enroll, student, "{}");
        assertThat(back.status()).isEqualTo(200);
        assertThat(back.body().path("id").asString()).isEqualTo(enrolled.body().path("id").asString());
    }

    @Test
    void twoLastSeatEnrolmentsCannotBothPass() throws Exception {
        Classroom room = new Classroom();
        room.set("seat_count = 1");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch go = new CountDownLatch(1);
            Callable<Integer> race = () -> {
                String token = room.studentToken(UUID.randomUUID(), "lms.cohort.enroll_in_cohort");
                go.await();
                return call("POST", COHORTS + room.cohort + ":enroll", token, "{}").status();
            };
            Future<Integer> first = pool.submit(race);
            Future<Integer> second = pool.submit(race);
            go.countDown();
            List<Integer> statuses = List.of(first.get(), second.get());
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
            assertThat(room.count("lms.cohort_enrollment WHERE is_active")).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    // ---- batch enrolment and members

    @Test
    void batchEnrolmentPreviewsThenEnrolsAllOrNothing() throws Exception {
        Classroom room = new Classroom();
        UUID here = room.student(true);
        UUID fresh = UUID.randomUUID();
        String batch = COHORTS + room.cohort + ":batchEnroll";

        Reply mismatch = call("POST", batch, room.admin, "{\"userIds\":[\"" + fresh + "\"],\"paymentIds\":[\"" + UUID
                .randomUUID() + "\",\"" + UUID.randomUUID() + "\"]}", "Idempotency-Key", UUID.randomUUID().toString());
        assertThat(mismatch.status()).isEqualTo(422);

        Reply preview = call("POST", batch, room.admin, "{\"userIds\":[\"" + here + "\",\"" + fresh
                + "\"],\"validateOnly\":true}", "Idempotency-Key", UUID.randomUUID().toString());
        assertThat(preview.status()).as(preview.text()).isEqualTo(200);
        assertThat(preview.body().path("items")).hasSize(2);
        assertThat(preview.body().path("items").path(0).path("alreadyEnrolled").asBoolean()).isTrue();
        assertThat(preview.body().path("items").path(1).path("alreadyEnrolled").asBoolean()).isFalse();
        assertThat(room.count("lms.cohort_enrollment")).as("a preview writes nothing").isEqualTo(1);

        room.set("seat_count = 1");
        assertThat(call("POST", batch, room.admin, "{\"userIds\":[\"" + fresh + "\",\"" + UUID.randomUUID() + "\"]}",
                "Idempotency-Key", UUID.randomUUID().toString()).code()).isEqualTo("COHORT_FULL");
        assertThat(room.count("lms.cohort_enrollment")).as("all or nothing").isEqualTo(1);

        room.set("seat_count = 5");
        Reply done = call("POST", batch, room.admin, "{\"userIds\":[\"" + fresh + "\"]}", "Idempotency-Key",
                UUID.randomUUID().toString());
        assertThat(done.status()).as(done.text()).isEqualTo(200);
        assertThat(room.count("lms.cohort_enrollment WHERE is_active")).isEqualTo(2);
    }

    @Test
    void settingMembersWritesButNamesNeedIam() throws Exception {
        Classroom room = new Classroom();
        String set = COHORTS + room.cohort + ":setMembers";
        UUID student = UUID.randomUUID();

        Reply duplicate = call("POST", set, room.admin, "{\"members\":[{\"userId\":\"" + student + "\"},{\"userId\":\""
                + student + "\"}]}");
        assertThat(duplicate.status()).isEqualTo(422);

        Reply named = call("POST", set, room.admin, "{\"members\":[{\"userId\":\"" + student + "\"},"
                + "{\"userId\":\"" + UUID.randomUUID() + "\",\"roleLabel\":\"teacher\"}]}");
        assertThat(named.status()).as("the roster names need iam, which is not running here").isEqualTo(503);
        assertThat(room.count("lms.cohort_enrollment WHERE is_active")).isEqualTo(1);
        assertThat(room.count("lms.cohort_teacher WHERE is_active")).isEqualTo(2);
    }

    @Test
    void rosterIsTheTeachersAndNeedsIamForNames() throws Exception {
        Classroom room = new Classroom();
        room.student(true);
        String roster = COHORTS + room.cohort + ":roster";
        String student = room.studentToken(room.student(true), "lms.cohort.get_cohort_roster", "lms.cohort.read");
        assertThat(call("GET", roster, student, null).status()).as("students may not read the roster").isEqualTo(403);
        assertThat(call("GET", COHORTS + room.cohort + ":roster?roleCode=observer", room.teacher, null).status())
                .isEqualTo(400);
        assertThat(call("GET", roster, room.teacher, null).status()).as("names come from iam").isEqualTo(503);
    }

    // ---- access window and announcements

    @Test
    void studentAccessWindowIsOrderedAndArchivedCohortsAreReadOnly() throws Exception {
        Classroom room = new Classroom();
        String access = COHORTS + room.cohort + ":setStudentAccess";
        Reply backwards = call("POST", access, room.admin,
                "{\"studentAccessFrom\":\"2026-02-01T00:00:00Z\",\"studentAccessUntil\":\"2026-01-01T00:00:00Z\"}");
        assertThat(backwards.status()).isEqualTo(422);
        Reply window = call("POST", access, room.admin,
                "{\"studentAccessFrom\":\"2026-01-01T00:00:00Z\",\"studentAccessUntil\":\"2026-02-01T00:00:00Z\"}");
        assertThat(window.status()).as(window.text()).isEqualTo(200);
        assertThat(window.body().path("studentAccessFrom").asString()).startsWith("2026-01-01");
        room.set("lifecycle_status = 'archived'");
        assertThat(call("POST", access, room.admin, "{}").status()).isEqualTo(409);
    }

    @Test
    void announcementsQueueOneDeliveryForEveryActiveStudent() throws Exception {
        Classroom room = new Classroom();
        room.student(true);
        room.student(true);
        room.student(false);
        String announce = COHORTS + room.cohort + ":sendAnnouncement";
        String body = "{\"subject\":\"Exam week\",\"content\":\"The hall opens at nine.\"}";

        String officer = token(room.tenant, UUID.randomUUID(), List.of(room.institution.toString()), List.of(),
                "lms.cohort.send_cohort_announcement", "lms.cohort.read");
        assertThat(call("POST", announce, officer, body).status()).as("only the cohort's teachers").isEqualTo(403);
        Reply sent = call("POST", announce, room.teacher, body);
        assertThat(sent.status()).as(sent.text()).isEqualTo(200);
        assertThat(sent.body().path("queued").asInt()).isEqualTo(2);
        assertThat(sent.body().path("skipped").asInt()).isEqualTo(0);
        assertThat(room.count("lms.outbox_event WHERE event_type = 'lms.cohort.announcement_requested'"))
                .isEqualTo(1);
        room.set("lifecycle_status = 'archived'");
        assertThat(call("POST", announce, room.teacher, body).status()).isEqualTo(409);
    }

    // ---- files: import, export, syllabus

    @Test
    void importChecksEverythingLocalThenNeedsPlatform() throws Exception {
        Classroom room = new Classroom();
        String importMembers = COHORTS + room.cohort + ":importMembers";
        String body = "{\"fileId\":\"" + UUID.randomUUID() + "\",\"memberKind\":\"student\"}";
        String powerless = room.studentToken(UUID.randomUUID(), "nothing.at.all");
        assertThat(call("POST", importMembers, powerless, body).status()).isEqualTo(403);
        assertThat(call("POST", COHORTS + UUID.randomUUID() + ":importMembers", room.admin, body).status())
                .isEqualTo(404);
        room.set("lifecycle_status = 'archived'");
        assertThat(call("POST", importMembers, room.admin, body).status()).isEqualTo(409);
        room.set("lifecycle_status = 'active'");
        assertThat(call("POST", importMembers, room.admin, body).status())
                .as("the file store is platform's, which is not running here").isEqualTo(503);
    }

    @Test
    void gradeExportIsCsvForTeachingStaffOnly() throws Exception {
        Classroom room = new Classroom();
        String export = COHORTS + room.cohort + ":exportGrades";
        assertThat(call("POST", export, room.teacher, "{\"format\":\"xlsx\"}").status())
                .as("only csv is made").isEqualTo(422);
        String student = room.studentToken(room.student(true), "lms.cohort.export_grades", "lms.cohort.read");
        assertThat(call("POST", export, student, "{\"format\":\"csv\"}").status()).isEqualTo(403);
        assertThat(call("POST", export, room.teacher, "{\"format\":\"csv\",\"contentItemId\":\"" + UUID.randomUUID()
                + "\"}").status()).as("not a graded item of the cohort").isEqualTo(422);
        UUID quiz = UUID.randomUUID();
        room.sql("INSERT INTO lms.quiz (id, tenant_id, title, passing_percentage, total_marks, row_version) "
                + "VALUES (:a, :tenantId, 'Quiz 1', 40, 10, 1)", quiz);
        room.sql("INSERT INTO lms.content_item (id, tenant_id, cohort_id, item_kind, quiz_id, title, is_graded, "
                + "points, row_version) VALUES (:a, :tenantId, :b, 'quiz', :c, 'Quiz 1', true, 10, 1)",
                UUID.randomUUID(), room.cohort, quiz);
        assertThat(call("POST", export, room.teacher, "{\"format\":\"csv\"}").status())
                .as("student names come from iam, which is not running here").isEqualTo(503);
    }

    @Test
    void syllabusIsAPdfForMembers() throws Exception {
        Classroom room = new Classroom();
        String syllabus = COHORTS + room.cohort + ":syllabus";
        assertThat(call("GET", syllabus + "?format=docx", room.teacher, null).status()).isEqualTo(422);
        room.set("is_published = false");
        String outsider = room.studentToken(UUID.randomUUID(), "lms.cohort.get_cohort_syllabus");
        assertThat(call("GET", syllabus, outsider, null).status()).isEqualTo(404);
        room.set("is_published = true");
        assertThat(call("GET", syllabus, room.teacher, null).status())
                .as("the file store is platform's, which is not running here").isEqualTo(503);
    }

    // ---- live-class invites

    @Test
    void calendarInvitesAreQueuedAgainForTheHost() throws Exception {
        Classroom room = new Classroom();
        UUID account = UUID.randomUUID();
        UUID liveClass = UUID.randomUUID();
        room.sql("INSERT INTO lms.conferencing_account (id, tenant_id, provider, account_name, owner_user_id, "
                + "is_enabled, row_version) VALUES (:a, :tenantId, 'zoom', 'School Zoom', :b, true, 1)", account,
                room.teacherUser);
        room.sql("INSERT INTO lms.live_class (id, tenant_id, cohort_id, title, class_date, start_time, "
                + "duration_minutes, timezone, host_user_id, conferencing_provider, conferencing_account_id, "
                + "meeting_id, invite_status, invite_attempts, row_version) VALUES (:a, :tenantId, :b, 'Live 1', "
                + "DATE '2026-02-01', TIME '10:00', 45, 'Asia/Kolkata', :c, 'zoom', :d, 'm-1', 'sent', 0, 1)",
                liveClass, room.cohort, room.teacherUser, account);
        String retry = "/v1/lms/live-classes/" + liveClass + ":retryInvites";

        String student = room.studentToken(room.student(true), "lms.live_class.retry_live_class_invites",
                "lms.live_class.read", "lms.cohort.read");
        assertThat(call("POST", retry, student, "{}").status()).as("only the host or the teachers").isEqualTo(403);

        Reply queued = call("POST", retry, room.teacher, "{}");
        assertThat(queued.status()).as(queued.text()).isEqualTo(200);
        assertThat(queued.body().path("inviteStatus").asString()).isEqualTo("retrying");
        assertThat(queued.body().path("inviteAttempts").asInt()).isEqualTo(1);
        assertThat(room.count("lms.outbox_event WHERE event_type = 'lms.live_class.invites_requested'")).isEqualTo(1);

        assertThat(call("POST", retry, room.teacher, "{}").body().path("inviteAttempts").asInt()).isEqualTo(2);

        room.sql("UPDATE lms.live_class SET ended_at = now() WHERE tenant_id = :tenantId AND id = :a", liveClass);
        assertThat(call("POST", retry, room.teacher, "{}").status()).as("an ended class sends no invites")
                .isEqualTo(409);
    }

    @Test
    void liveClassesNeedTheirProviderConfiguredAndJoinIsLocal() throws Exception {
        Classroom room = new Classroom();
        UUID account = UUID.randomUUID();
        UUID liveClass = UUID.randomUUID();
        room.sql("INSERT INTO lms.conferencing_account (id, tenant_id, provider, account_name, owner_user_id, "
                + "is_enabled, row_version) VALUES (:a, :tenantId, 'zoom', 'School Zoom', :b, true, 1)", account,
                room.teacherUser);
        Reply unknown = call("POST", "/v1/lms/live-classes:schedule", room.teacher, "{\"cohortId\":\"" + room.cohort
                + "\",\"conferencingAccountId\":\"" + UUID.randomUUID() + "\",\"title\":\"Live 1\",\"classDate\":"
                + "\"2026-02-01\",\"startTime\":\"10:00\",\"durationMinutes\":45,\"timezone\":\"Asia/Kolkata\"}");
        assertThat(unknown.status()).as(unknown.text()).isEqualTo(422);
        Reply unconfigured = call("POST", "/v1/lms/live-classes:schedule", room.teacher, "{\"cohortId\":\""
                + room.cohort + "\",\"conferencingAccountId\":\"" + account + "\",\"title\":\"Live 1\",\"classDate\":"
                + "\"2026-02-01\",\"startTime\":\"10:00\",\"durationMinutes\":45,\"timezone\":\"Asia/Kolkata\"}");
        assertThat(unconfigured.status()).as(unconfigured.text()).isEqualTo(503);
        assertThat(unconfigured.code()).isEqualTo("CONFERENCING_NOT_CONFIGURED");

        room.sql("INSERT INTO lms.live_class (id, tenant_id, cohort_id, title, class_date, start_time, "
                + "duration_minutes, timezone, host_user_id, conferencing_provider, conferencing_account_id, "
                + "meeting_id, join_url, row_version) VALUES (:a, :tenantId, :b, 'Live 1', DATE '2026-02-01', "
                + "TIME '10:00', 45, 'Asia/Kolkata', :c, 'zoom', :d, 'm-1', 'https://meet.example/j', 1)",
                liveClass, room.cohort, room.teacherUser, account);
        String join = "/v1/lms/live-classes/" + liveClass + ":join";
        String member = room.studentToken(room.student(true), "lms.live_class.join_live_class",
                "lms.live_class.read", "lms.cohort.read");
        String outsider = room.studentToken(UUID.randomUUID(), "lms.live_class.join_live_class",
                "lms.live_class.read", "lms.cohort.read");
        assertThat(call("POST", join, outsider, "{}").status()).as("members only").isEqualTo(403);
        Reply joined = call("POST", join, member, "{}");
        assertThat(joined.status()).as(joined.text()).isEqualTo(200);
        assertThat(joined.body().path("url").asString()).isEqualTo("https://meet.example/j");
        assertThat(room.count("lms.live_class_participant")).isEqualTo(1);
        assertThat(call("POST", join, member, "{}").status()).as("joining twice keeps one row").isEqualTo(200);
        assertThat(room.count("lms.live_class_participant")).isEqualTo(1);

        assertThat(call("POST", "/v1/lms/live-classes/" + liveClass + ":syncAttendance", room.teacher, "{}")
                .status()).as("attendance is read once the class has ended").isEqualTo(409);
        room.sql("UPDATE lms.live_class SET ended_at = now() WHERE tenant_id = :tenantId AND id = :a", liveClass);
        assertThat(call("POST", join, member, "{}").status()).as("an ended class is not joined").isEqualTo(409);
    }

    // ---- the reader's own forum marks

    @Test
    void readMarksAndFlagsAreTheReadersOwn() throws Exception {
        Classroom room = new Classroom();
        UUID topic = UUID.randomUUID();
        UUID post = UUID.randomUUID();
        room.sql("INSERT INTO lms.forum_topic (id, tenant_id, title, topic_type, row_version) "
                + "VALUES (:a, :tenantId, 'Week 1', 'discussion', 1)", topic);
        room.sql("INSERT INTO lms.forum_post (id, tenant_id, forum_topic_id, author_user_id, posted_at, "
                + "row_version) VALUES (:a, :tenantId, :b, :c, now(), 1)", post, topic, UUID.randomUUID());
        UUID reader = room.student(true);
        String student = room.studentToken(reader, "lms.forum_topic.mark_topic_read",
                "lms.forum_post.flag_forum_post", "lms.forum_topic.read", "lms.forum_post.read");

        Reply stray = call("POST", "/v1/lms/forum-topics/" + topic + ":markRead", student,
                "{\"forumPostIds\":[\"" + UUID.randomUUID() + "\"]}");
        assertThat(stray.status()).as("every post must belong to the topic").isEqualTo(422);
        Reply read = call("POST", "/v1/lms/forum-topics/" + topic + ":markRead", student,
                "{\"forumPostIds\":[\"" + post + "\"]}");
        assertThat(read.status()).as(read.text()).isEqualTo(204);
        assertThat(call("POST", "/v1/lms/forum-topics/" + topic + ":markRead", student, "{}").status())
                .as("marking read is repeatable").isEqualTo(204);
        assertThat(room.count("lms.forum_read_state WHERE user_id = '" + reader + "' AND read_at IS NOT NULL"))
                .isEqualTo(2);

        String flag = "/v1/lms/forum-topics/" + topic + "/posts/" + post + ":flag";
        assertThat(call("POST", flag, student, "{\"flagged\":true}").status()).isEqualTo(204);
        assertThat(room.count("lms.forum_read_state WHERE user_id = '" + reader + "' AND is_flagged")).isEqualTo(1);
        assertThat(call("POST", flag, student, "{\"flagged\":false}").status()).isEqualTo(204);
        assertThat(room.count("lms.forum_read_state WHERE user_id = '" + reader + "' AND is_flagged")).isEqualTo(0);
    }

    // ---- member profile, cohort reads, settings

    @Test
    void memberProfilesNeedASignInAndIamForTheCard() throws Exception {
        Classroom room = new Classroom();
        UUID member = UUID.randomUUID();
        HttpResponse<String> anonymous = HTTP.send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/v1/lms/members/" + member + ":profile")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(anonymous.statusCode()).isEqualTo(401);
        assertThat(call("GET", "/v1/lms/members/" + member + ":profile", room.admin, null).status())
                .as("the member card is named through iam, which is not running here").isEqualTo(503);
    }

    @Test
    void unpublishedCohortsAreVisibleToMembersAndManagersOnly() throws Exception {
        Classroom room = new Classroom();
        room.set("is_published = false");
        UUID enrolled = room.student(true);
        String member = room.studentToken(enrolled, "lms.cohort.read");
        String outsider = room.studentToken(UUID.randomUUID(), "lms.cohort.read");

        assertThat(call("GET", COHORTS + room.cohort, member, null).status()).isEqualTo(200);
        assertThat(call("GET", COHORTS + room.cohort, outsider, null).status()).isEqualTo(404);
        assertThat(call("GET", COHORTS + room.cohort, room.admin, null).status()).isEqualTo(200);

        Reply list = call("GET", "/v1/lms/cohorts", outsider, null);
        assertThat(list.status()).isEqualTo(200);
        assertThat(list.body().path("data")).as("an outsider's list leaves the unpublished cohort out").isEmpty();
        assertThat(call("GET", "/v1/lms/cohorts", member, null).body().path("data")).hasSize(1);
    }

    @Test
    void lmsSettingsAreTheTenantAdministrators() throws Exception {
        Classroom room = new Classroom();
        String scoped = token(room.tenant, UUID.randomUUID(), List.of(room.institution.toString()), List.of(), "*");
        assertThat(call("GET", "/v1/lms/settings", scoped, null).status())
                .as("tenant-level settings need a tenant-wide grant").isEqualTo(403);

        Reply defaults = call("GET", "/v1/lms/settings", room.admin, null);
        assertThat(defaults.status()).as(defaults.text()).isEqualTo(200);
        assertThat(defaults.body().path("etag").asString()).isEqualTo("\"0\"");

        Reply saved = call("PATCH", "/v1/lms/settings", room.admin,
                "{\"allowGuestAccess\":true,\"lessonDwellSeconds\":30}", "If-Match", "\"0\"");
        assertThat(saved.status()).as(saved.text()).isEqualTo(200);
        assertThat(saved.body().path("allowGuestAccess").asBoolean()).isTrue();
        assertThat(call("PATCH", "/v1/lms/settings", room.admin, "{\"lessonDwellSeconds\":-1}", "If-Match",
                saved.body().path("etag").asString()).status()).isEqualTo(422);
        assertThat(call("PATCH", "/v1/lms/settings", room.admin, "{\"allowGuestAccess\":false}", "If-Match",
                "\"9\"").status()).isEqualTo(412);
    }

    // ---- helpers

    private Reply call(String method, String path, String token, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        request.header("Authorization", "Bearer " + token);
        for (int at = 0; at < headers.length; at += 2) {
            request.header(headers[at], headers[at + 1]);
        }
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
