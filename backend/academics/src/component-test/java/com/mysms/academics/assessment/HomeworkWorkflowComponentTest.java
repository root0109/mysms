package com.mysms.academics.assessment;

import com.mysms.academics.AcademicsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.academics.assessment.config.AssessmentDatabaseConfig;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
 * The homework lifecycle over HTTP against real PostgreSQL as the application role: publishing with and without review,
 * the reviewer's approval and rejection, checking a submission and the student's reply. Each test works in a tenant of
 * its own.
 */
@SpringBootTest(classes = AcademicsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HomeworkWorkflowComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String HOMEWORKS = "/v1/assessment/homeworks";
    private static final String SUBMISSIONS = "/v1/assessment/homework-submissions";
    private static final String PUBLISH = "assessment.homework.publish_homework";
    private static final String APPROVE = "assessment.homework.approve_homework";
    private static final String REJECT = "assessment.homework.reject_homework";
    private static final String CHECK = "assessment.homework_submission.check_homework_submission";
    private static final String REPLY = "assessment.homework_submission.reply_to_homework_feedback";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(AssessmentDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "assessment", "academics", "timetable", "attendance");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {
    }

    /** One school of one tenant with its administrator, a teacher who needs review and a reviewer. */
    private final class School {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final String admin = token(UUID.randomUUID(), tenant, List.of("*"), List.of("*"), "*");
        final String teacher = token(UUID.randomUUID(), tenant, List.of(institution.toString()), List.of(),
                "assessment.homework.read", "assessment.homework.create", PUBLISH, CHECK);
        final UUID reviewerId = UUID.randomUUID();
        final String reviewer = token(reviewerId, tenant, List.of(institution.toString()), List.of(),
                "assessment.homework.read", APPROVE, REJECT);

        String homework(String extra) throws Exception {
            return created(call("POST", HOMEWORKS, admin, "{\"institutionId\":\"" + institution
                    + "\",\"academicYearId\":\"" + UUID.randomUUID() + "\",\"classSectionId\":\"" + UUID.randomUUID()
                    + "\",\"description\":\"Read chapter 4\"" + extra + "}"));
        }

        Reply get(String homework) throws Exception {
            return call("GET", HOMEWORKS + "/" + homework, admin, null);
        }

        JsonNode approvals(String homework) throws Exception {
            Reply list = call("GET", HOMEWORKS + "/" + homework + "/approvals?sort=createTime", admin, null);
            assertThat(list.status()).as(list.text()).isEqualTo(200);
            return list.body().path("data");
        }
    }

    private static String created(Reply reply) {
        assertThat(reply.status()).as(reply.text()).isEqualTo(201);
        return reply.body().path("id").asString();
    }

    private Reply call(String method, String path, String token, String body) throws Exception {
        return call(method, path, token, body, null);
    }

    private Reply call(String method, String path, String token, String body, String ifMatch) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (ifMatch != null) {
            request.header("If-Match", ifMatch);
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    /** The submission as the student would have made it (sis, which confirms the student's own, is not running). */
    private void handedInBy(UUID tenant, String submission, UUID studentUser) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(tenant, null);
        CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(() -> database.jdbc()
                .sql("UPDATE assessment.homework_submission SET created_by = :user WHERE tenant_id = :tenantId "
                        + "AND id = :id").param("user", studentUser).param("id", UUID.fromString(submission)).update()));
    }

    private Reply step(String homework, String step, String token, String body) throws Exception {
        return call("POST", HOMEWORKS + "/" + homework + ":" + step, token, body);
    }

    private static String token(UUID user, UUID tenant, List<String> institutions, List<String> tenantWide,
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

    private List<String> events(UUID tenant, String aggregate) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).read(() -> database.jdbc().sql("SELECT event_type FROM "
                        + "assessment.outbox_event WHERE tenant_id = :tenantId AND aggregate_id = :aggregate "
                        + "AND event_type <> 'audit.recorded' ORDER BY id")
                        .param("aggregate", UUID.fromString(aggregate)).query(String.class).list()));
    }

    @Test
    void aTeacherWhoMayNotApproveSendsHomeworkForReviewAndTheReviewerPublishesIt() throws Exception {
        School school = new School();
        String homework = school.homework("");
        String etag = school.get(homework).body().path("etag").asString();

        Reply stale = step(homework, "publish", school.teacher, "{\"etag\":\"\\\"99\\\"\"}");
        assertThat(stale.status()).as(stale.text()).isEqualTo(412);
        Reply sent = step(homework, "publish", school.teacher, "{\"etag\":" + JSON.writeValueAsString(etag) + "}");
        assertThat(sent.status()).as(sent.text()).isEqualTo(200);
        assertThat(sent.body().path("status").asString()).isEqualTo("pending");
        assertThat(sent.body().has("assignedOn")).as("not set until it is published").isFalse();
        assertThat(school.approvals(homework)).hasSize(1);
        assertThat(school.approvals(homework).get(0).path("decision").asString()).isEqualTo("pending");

        Reply again = step(homework, "publish", school.teacher, "{}");
        assertThat(again.status()).as("already waiting for review").isEqualTo(409);
        assertThat(again.body().path("code").asString()).isEqualTo("INVALID_STATE_TRANSITION");
        assertThat(step(homework, "approve", school.teacher, "{}").status()).as("a teacher cannot approve")
                .isEqualTo(403);

        Reply approved = step(homework, "approve", school.reviewer, "{\"reason\":\"  Good to go  \"}");
        assertThat(approved.status()).as(approved.text()).isEqualTo(200);
        assertThat(approved.body().path("status").asString()).isEqualTo("published");
        assertThat(approved.body().path("assignedOn").asString()).isNotEmpty();
        JsonNode decision = school.approvals(homework).get(0);
        assertThat(decision.path("decision").asString()).isEqualTo("approved");
        assertThat(decision.path("comments").asString()).isEqualTo("Good to go");
        assertThat(decision.path("decidedByUserId").asString()).isEqualTo(school.reviewerId.toString());
        assertThat(decision.path("decidedAt").asString()).isNotEmpty();

        assertThat(step(homework, "approve", school.reviewer, "{}").status()).as("nothing left to review")
                .isEqualTo(409);
        assertThat(step(homework, "reject", school.reviewer, "{}").status()).isEqualTo(409);
        assertThat(events(school.tenant, homework)).containsExactly("assessment.homework.created",
                "assessment.homework.sendForApproval", "assessment.homework.approve");
    }

    @Test
    void aRejectedHomeworkIsADraftAgainAndASecondReviewStartsAfresh() throws Exception {
        School school = new School();
        String homework = school.homework("");
        assertThat(step(homework, "publish", school.teacher, "{}").status()).isEqualTo(200);
        assertThat(step(homework, "reject", school.reviewer, "{\"reason\":\"" + "x".repeat(4001) + "\"}").status())
                .as("a reason longer than the limit").isEqualTo(422);

        Reply rejected = step(homework, "reject", school.reviewer, "{\"reason\":\"Due date is a holiday\"}");
        assertThat(rejected.status()).as(rejected.text()).isEqualTo(200);
        assertThat(rejected.body().path("status").asString()).isEqualTo("draft");
        assertThat(rejected.body().has("assignedOn")).isFalse();
        assertThat(school.approvals(homework).get(0).path("decision").asString()).isEqualTo("rejected");
        assertThat(school.approvals(homework).get(0).path("comments").asString()).isEqualTo("Due date is a holiday");

        assertThat(step(homework, "publish", school.teacher, "{}").body().path("status").asString())
                .isEqualTo("pending");
        JsonNode reviews = school.approvals(homework);
        assertThat(reviews).hasSize(2);
        assertThat(reviews.get(0).path("decision").asString()).isEqualTo("rejected");
        assertThat(reviews.get(1).path("decision").asString()).isEqualTo("pending");
    }

    @Test
    void whoMayApprovePublishesAtOnceAndTheDayTheTeacherGaveIsKept() throws Exception {
        School school = new School();
        String head = token(UUID.randomUUID(), school.tenant, List.of(school.institution.toString()), List.of(),
                PUBLISH, APPROVE);
        String homework = school.homework("");
        Reply published = step(homework, "publish", head, "{}");
        assertThat(published.status()).as(published.text()).isEqualTo(200);
        assertThat(published.body().path("status").asString()).isEqualTo("published");
        assertThat(published.body().path("assignedOn").asString()).isNotEmpty();
        assertThat(school.approvals(homework)).as("no review was needed").isEmpty();
        assertThat(events(school.tenant, homework)).containsExactly("assessment.homework.created",
                "assessment.homework.publish");

        String dated = school.homework(",\"assignedOn\":\"2026-03-02T08:00:00Z\"");
        assertThat(OffsetDateTime.parse(step(dated, "publish", head, "{}").body().path("assignedOn").asString())
                .toInstant()).as("the day the teacher set is kept").isEqualTo(Instant.parse("2026-03-02T08:00:00Z"));
    }

    @Test
    void theStepsNeedTheirPermissionAndTheHomeworkInsideTheCallersInstitutionsAndTenant() throws Exception {
        School school = new School();
        String homework = school.homework("");
        String readOnly = token(UUID.randomUUID(), school.tenant, List.of("*"), List.of("*"),
                "assessment.homework.read");
        String elsewhere = token(UUID.randomUUID(), school.tenant, List.of(UUID.randomUUID().toString()), List.of(),
                "*");
        String otherTenant = token(UUID.randomUUID(), UUID.randomUUID(), List.of("*"), List.of("*"), "*");
        for (String action : List.of("publish", "approve", "reject")) {
            assertThat(step(homework, action, readOnly, "{}").status()).as(action + " without permission")
                    .isEqualTo(403);
            assertThat(step(homework, action, null, "{}").status()).as(action + " without a token").isEqualTo(401);
            assertThat(step(homework, action, elsewhere, "{}").status()).as(action + " from another institution")
                    .isEqualTo(404);
            assertThat(step(homework, action, otherTenant, "{}").status()).as(action + " from another tenant")
                    .isEqualTo(404);
            assertThat(step(UUID.randomUUID().toString(), action, school.admin, "{}").status()).isEqualTo(404);
        }
        assertThat(school.get(homework).body().path("status").asString()).isEqualTo("draft");
        assertThat(step(homework, "approve", school.reviewer, "{}").status()).as("a draft is not under review")
                .isEqualTo(409);
    }

    @Test
    void aTeacherChecksASubmissionAndOnlyTheStudentWhoHandedItInReplies() throws Exception {
        School school = new School();
        String homework = school.homework("");
        assertThat(step(homework, "publish", school.admin, "{}").status()).isEqualTo(200);
        UUID studentUser = UUID.randomUUID();
        String student = token(studentUser, school.tenant, List.of(school.institution.toString()), List.of(),
                "assessment.homework_submission.read", "assessment.homework_submission.create", REPLY);
        String classmate = token(UUID.randomUUID(), school.tenant, List.of(school.institution.toString()),
                List.of(), "assessment.homework_submission.read", REPLY);
        String handIn = "{\"homeworkId\":\"" + homework + "\",\"studentId\":\"" + UUID.randomUUID() + "\"}";
        assertThat(call("POST", SUBMISSIONS, student, handIn).status())
                .as("a student hands in their own work only, which sis confirms (not running here)").isEqualTo(503);
        String submission = created(call("POST", SUBMISSIONS, school.admin, handIn));
        handedInBy(school.tenant, submission, studentUser);
        String check = SUBMISSIONS + "/" + submission + ":check";
        String reply = SUBMISSIONS + "/" + submission + ":reply";

        Reply early = call("POST", reply, student, "{\"studentReply\":\"Thanks\"}");
        assertThat(early.status()).as("nothing to reply to yet: " + early.text()).isEqualTo(409);
        assertThat(call("POST", check, student, "{}").status()).as("a student cannot check").isEqualTo(403);
        assertThat(call("POST", check, school.teacher, "{\"teacherComments\":\"" + "x".repeat(4001) + "\"}")
                .status()).isEqualTo(422);

        Reply checked = call("POST", check, school.teacher, "{\"teacherComments\":\" Show your working \"}");
        assertThat(checked.status()).as(checked.text()).isEqualTo(200);
        assertThat(checked.body().path("status").asString()).isEqualTo("checked");
        assertThat(checked.body().path("teacherComments").asString()).isEqualTo("Show your working");
        assertThat(checked.body().path("checkedByUserId").asString()).isNotEmpty();
        assertThat(checked.body().path("checkedOn").asString()).isEqualTo(LocalDate.now(ZoneOffset.UTC).toString());
        String etag = checked.body().path("etag").asString();

        Reply stale = call("POST", check, school.teacher, "{\"etag\":\"\\\"1\\\"\"}");
        assertThat(stale.status()).as(stale.text()).isEqualTo(412);
        Reply again = call("POST", check, school.teacher, "{\"etag\":" + JSON.writeValueAsString(etag) + "}");
        assertThat(again.status()).as("checking again is allowed: " + again.text()).isEqualTo(200);
        assertThat(again.body().path("teacherComments").asString()).as("comments are kept when none are sent")
                .isEqualTo("Show your working");

        Reply notMine = call("POST", reply, classmate, "{\"studentReply\":\"I will\"}");
        assertThat(notMine.status()).as("another account: " + notMine.text()).isEqualTo(403);
        assertThat(call("POST", reply, student, "{\"studentReply\":\"   \"}").status()).isEqualTo(422);
        assertThat(call("POST", reply, student, "{}").status()).isEqualTo(422);
        Reply replied = call("POST", reply, student, "{\"studentReply\":\" I will redo it \"}");
        assertThat(replied.status()).as(replied.text()).isEqualTo(200);
        assertThat(replied.body().path("studentReply").asString()).isEqualTo("I will redo it");
        assertThat(replied.body().path("status").asString()).isEqualTo("checked");

        String elsewhere = token(UUID.randomUUID(), school.tenant, List.of(UUID.randomUUID().toString()), List.of(),
                "*");
        assertThat(call("POST", check, elsewhere, "{}").status()).as("another institution").isEqualTo(404);
        assertThat(call("POST", reply, elsewhere, "{\"studentReply\":\"x\"}").status()).isEqualTo(404);
        assertThat(call("POST", check, token(UUID.randomUUID(), UUID.randomUUID(), List.of("*"), List.of("*"), "*"),
                "{}").status()).as("another tenant").isEqualTo(404);
        assertThat(events(school.tenant, submission)).containsExactly("assessment.homework_submission.created",
                "assessment.homework_submission.check", "assessment.homework_submission.check",
                "assessment.homework_submission.reply");
    }

    @Test
    void whatWasReviewedIsNotReplacedAndTheRecordsOfTheWorkflowAreNotWrittenByHand() throws Exception {
        School school = new School();
        String author = token(UUID.randomUUID(), school.tenant, List.of(school.institution.toString()), List.of(),
                "assessment.homework.read", "assessment.homework.update", PUBLISH);
        String homework = school.homework("");
        String item = HOMEWORKS + "/" + homework;
        String changed = "{\"description\":\"Read chapter 5\"}";

        Reply draft = call("PATCH", item, author, changed, school.get(homework).body().path("etag").asString());
        assertThat(draft.status()).as("a draft is the teacher's to change: " + draft.text()).isEqualTo(200);
        assertThat(step(homework, "publish", author, "{}").status()).isEqualTo(200);
        Reply waiting = call("PATCH", item, author, changed, school.get(homework).body().path("etag").asString());
        assertThat(waiting.status()).as("under review: " + waiting.text()).isEqualTo(409);
        assertThat(call("PATCH", item, school.admin, changed, school.get(homework).body().path("etag").asString())
                .status()).as("under review nobody changes it").isEqualTo(409);

        assertThat(step(homework, "approve", school.reviewer, "{}").status()).isEqualTo(200);
        String published = school.get(homework).body().path("etag").asString();
        assertThat(call("PATCH", item, author, changed, published).status())
                .as("an approved text is not replaced by an unreviewed one").isEqualTo(409);
        Reply byReviewer = call("PATCH", item, school.admin, "{\"description\":\"Read chapters 5 and 6\"}", published);
        assertThat(byReviewer.status()).as("who may approve may correct it: " + byReviewer.text()).isEqualTo(200);
        assertThat(byReviewer.body().path("status").asString()).isEqualTo("published");

        String approvals = item + "/approvals";
        JsonNode approval = school.approvals(homework).get(0);
        String one = approvals + "/" + approval.path("id").asString();
        String etag = approval.path("etag").asString();
        assertThat(call("POST", approvals, school.admin, "{\"homeworkId\":\"" + homework + "\"}").status())
                .as("the contract has no way to write an approval by hand").isIn(404, 405);
        assertThat(call("PATCH", one, school.admin, "{\"decision\":\"rejected\"}", etag).status()).isIn(404, 405);
        assertThat(call("DELETE", one, school.admin, null, etag).status()).isIn(404, 405);
        assertThat(school.approvals(homework).get(0).path("decision").asString()).isEqualTo("approved");

        Reply handedIn = call("POST", SUBMISSIONS, school.admin, "{\"homeworkId\":\"" + homework
                + "\",\"studentId\":\"" + UUID.randomUUID() + "\",\"teacherComments\":\"Full marks\","
                + "\"checkedByUserId\":\"" + UUID.randomUUID() + "\",\"checkedOn\":\"2026-01-05\","
                + "\"studentReply\":\"Thanks\"}");
        String submission = created(handedIn);
        assertThat(handedIn.body().has("teacherComments")).as("a check is not written by hand").isFalse();
        assertThat(handedIn.body().has("checkedByUserId")).isFalse();
        assertThat(handedIn.body().has("checkedOn")).isFalse();
        assertThat(handedIn.body().has("studentReply")).isFalse();
        assertThat(handedIn.body().path("status").asString()).isEqualTo("unchecked");
        Reply forged = call("PATCH", SUBMISSIONS + "/" + submission, school.admin,
                "{\"teacherComments\":\"Full marks\"}", handedIn.body().path("etag").asString());
        assertThat(forged.body().has("teacherComments")).as("not writable here: ignored " + forged.text()).isFalse();
    }
}
