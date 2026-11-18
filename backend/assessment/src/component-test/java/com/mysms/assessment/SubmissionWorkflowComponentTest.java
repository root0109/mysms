package com.mysms.assessment;

import static com.mysms.assessment.AssessmentFixtures.expect;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.assessment.AssessmentFixtures.Reply;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
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
 * Assignment and homework submissions and the assignment review over HTTP against real PostgreSQL as the
 * application role: grading is written only by the workflow, staff (who hold a reviewing permission in the school)
 * work on every submission there, and anyone else — a student, a guardian — reaches only their own students'
 * submissions, which sis confirms; sis is not running here, so those requests stop at 503 and nothing is shown or
 * written. Each test works in a tenant of its own.
 */
@SpringBootTest(classes = AssessmentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SubmissionWorkflowComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String ASSIGNMENTS = "/v1/assessment/assignments";
    private static final String SUBMISSIONS = "/v1/assessment/assignment-submissions";
    private static final String HOMEWORK_SUBMISSIONS = "/v1/assessment/homework-submissions";
    private static final String GRADE = "assessment.assignment_submission.grade_assignment_submission";
    private static final String[] STUDENT = {"assessment.assignment_submission.read",
        "assessment.assignment_submission.create", "assessment.assignment_submission.update",
        "assessment.assignment_submission.submit_assignment_submission",
        "assessment.assignment_submission.reset_assignment_submission", "assessment.homework_submission.read",
        "assessment.homework_submission.create", "assessment.homework_submission.update"};

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(AssessmentDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private AssessmentFixtures fx;
    private UUID tenant;
    private UUID institution;
    private String admin;
    private String teacher;
    private String student;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "assessment");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @BeforeEach
    void school() {
        fx = new AssessmentFixtures(TOKENS, database, port);
        tenant = UUID.randomUUID();
        institution = UUID.randomUUID();
        admin = fx.admin(tenant);
        teacher = fx.member(tenant, institution, "assessment.assignment_submission.read",
                "assessment.assignment_submission.update", GRADE,
                "assessment.assignment_submission.accept_assignment_submission",
                "assessment.assignment_submission.request_submission_changes",
                "assessment.assignment_submission.reset_assignment_submission",
                "assessment.assignment_submission.submit_assignment_submission");
        student = fx.member(tenant, institution, STUDENT);
    }

    @Test
    void gradingIsWrittenOnlyByTheWorkflow() throws Exception {
        String assignment = assignment("published", "20");
        OffsetDateTime before = OffsetDateTime.now().minusSeconds(5);
        Reply made = expect(fx.call("POST", SUBMISSIONS, admin, "{\"assignmentId\":\"" + assignment
                + "\",\"studentId\":\"" + UUID.randomUUID() + "\",\"content\":\"My essay\",\"marksObtained\":\"20\","
                + "\"gradedByUserId\":\"" + UUID.randomUUID() + "\",\"gradedOn\":\"2026-01-05\","
                + "\"feedback\":\"Excellent\",\"submittedAt\":\"2020-01-01T00:00:00Z\"}"), 201);
        JsonNode created = made.body();
        String submission = created.path("id").asString();
        assertThat(created.has("marksObtained")).as("grading is not written by hand").isFalse();
        assertThat(created.has("gradedByUserId")).isFalse();
        assertThat(created.has("feedback")).isFalse();
        assertThat(OffsetDateTime.parse(created.path("submittedAt").asString()))
                .as("the server records when the work was handed in").isAfter(before)
                .isBefore(OffsetDateTime.now().plus(5, ChronoUnit.SECONDS));
        assertThat(created.path("status").asString()).isEqualTo("submitted");

        String item = SUBMISSIONS + "/" + submission;
        String etag = created.path("etag").asString();
        for (String field : List.of("marksObtained", "gradedByUserId", "gradedOn", "feedback", "studentId",
                "submittedAt")) {
            Reply forged = fx.call("PATCH", item + "?updateMask=" + field, admin, "{\"" + field + "\":\"x\"}", etag);
            assertThat(forged.status()).as(field + " is not updatable: " + forged.text()).isEqualTo(400);
        }

        Reply returned = expect(fx.call("POST", item + ":requestChanges", teacher, "{\"reason\":\"Add sources\"}"),
                200);
        assertThat(returned.body().path("status").asString()).isEqualTo("change_requested");
        assertThat(returned.body().path("feedback").asString()).isEqualTo("Add sources");
        Reply draft = expect(fx.call("POST", item + ":resetToDraft", teacher, "{}"), 200);
        assertThat(draft.body().path("status").asString()).isEqualTo("draft");
        expect(fx.call("POST", item + ":resetToDraft", teacher, "{}"), 409);
        Reply edited = expect(fx.call("PATCH", item + "?updateMask=content", teacher, "{\"content\":\"With sources\"}",
                draft.body().path("etag").asString()), 200);
        Reply submitted = expect(fx.call("POST", item + ":submit", teacher,
                "{\"etag\":" + AssessmentFixtures.json(edited.body().path("etag").asString()) + "}"), 200);
        assertThat(submitted.body().path("status").asString()).isEqualTo("submitted");

        expect(fx.call("POST", item + ":grade", teacher, "{\"marks\":\"20.01\"}"), 422);
        Reply graded = expect(fx.call("POST", item + ":grade", teacher, "{\"marks\":\"18.5\",\"feedback\":\" Good \"}"),
                200);
        assertThat(graded.body().path("status").asString()).isEqualTo("graded");
        assertThat(graded.body().path("marksObtained").asString()).isEqualTo("18.50");
        assertThat(graded.body().path("feedback").asString()).isEqualTo("Good");
        assertThat(graded.body().path("gradedOn").asString()).isNotEmpty();
        expect(fx.call("POST", item + ":grade", teacher, "{\"marks\":\"1\"}"), 409);
    }

    @Test
    void studentsReachOnlyTheirOwnSubmissionsThroughSis() throws Exception {
        String assignment = assignment("published", "10");
        UUID classmate = UUID.randomUUID();
        String submission = expect(fx.call("POST", SUBMISSIONS, admin,
                "{\"assignmentId\":\"" + assignment + "\",\"studentId\":\"" + classmate + "\"}"), 201)
                .body().path("id").asString();
        String item = SUBMISSIONS + "/" + submission;
        String etag = expect(fx.call("GET", item, teacher, null), 200).body().path("etag").asString();

        JsonNode staffList = expect(fx.call("GET", SUBMISSIONS + "?filter=" + filter("assignmentId", assignment),
                teacher, null), 200).body().path("data");
        assertThat(staffList).extracting(row -> row.path("id").asString()).containsExactly(submission);

        for (Reply refused : List.of(fx.call("GET", item, student, null),
                fx.call("GET", SUBMISSIONS + "?filter=" + filter("assignmentId", assignment), student, null),
                fx.call("POST", SUBMISSIONS, student, "{\"assignmentId\":\"" + assignment + "\",\"studentId\":\""
                        + UUID.randomUUID() + "\"}"),
                fx.call("PATCH", item + "?updateMask=content", student, "{\"content\":\"mine now\"}", etag),
                fx.call("POST", item + ":submit", student, "{}"))) {
            assertThat(refused.status()).as("only sis can say whose student it is: " + refused.text()).isEqualTo(503);
        }
        assertThat(fx.value(tenant, "SELECT count(*) FROM assessment.assignment_submission WHERE tenant_id = :tenantId",
                Long.class)).isEqualTo(1L);
        assertThat(fx.value(tenant, "SELECT content FROM assessment.assignment_submission WHERE tenant_id = :tenantId",
                String.class)).isNull();

        String elsewhere = fx.member(tenant, UUID.randomUUID(), "assessment.assignment_submission.read", GRADE,
                "assessment.assignment_submission.create");
        expect(fx.call("GET", item, elsewhere, null), 404);
        assertThat(expect(fx.call("GET", SUBMISSIONS, elsewhere, null), 200).body().path("data")).isEmpty();
        expect(fx.call("POST", SUBMISSIONS, elsewhere, "{\"assignmentId\":\"" + assignment + "\",\"studentId\":\""
                + UUID.randomUUID() + "\"}"), 403);
        expect(fx.call("GET", item, fx.admin(UUID.randomUUID()), null), 404);
    }

    @Test
    void homeworkSubmissionsKeepTheirStudentAndAreOwnerSafe() throws Exception {
        String homework = expect(fx.call("POST", "/v1/assessment/homeworks", admin, "{\"institutionId\":\""
                + institution + "\",\"academicYearId\":\"" + UUID.randomUUID() + "\",\"classSectionId\":\""
                + UUID.randomUUID() + "\",\"description\":\"Read chapter 4\"}"), 201).body().path("id").asString();
        Reply made = expect(fx.call("POST", HOMEWORK_SUBMISSIONS, admin, "{\"homeworkId\":\"" + homework
                + "\",\"studentId\":\"" + UUID.randomUUID() + "\"}"), 201);
        String item = HOMEWORK_SUBMISSIONS + "/" + made.body().path("id").asString();
        String etag = made.body().path("etag").asString();

        expect(fx.call("PATCH", item + "?updateMask=studentId", admin, "{\"studentId\":\"" + UUID.randomUUID() + "\"}",
                etag), 400);
        expect(fx.call("PATCH", item + "?updateMask=submittedOn", admin, "{\"submittedOn\":\"2026-03-02\"}", etag),
                200);
        expect(fx.call("GET", item, student, null), 503);
        expect(fx.call("GET", HOMEWORK_SUBMISSIONS, student, null), 503);
        String checker = fx.member(tenant, institution, "assessment.homework_submission.read",
                "assessment.homework_submission.check_homework_submission");
        assertThat(expect(fx.call("GET", HOMEWORK_SUBMISSIONS, checker, null), 200).body().path("data")).hasSize(1);
    }

    @Test
    void anAssignmentIsApprovedOrRejectedThroughItsPendingReviewAndFinished() throws Exception {
        String assignment = assignment("draft", "10");
        String item = ASSIGNMENTS + "/" + assignment;
        expect(fx.call("POST", item + ":reject", admin, "{}"), 409);
        expect(fx.call("POST", item + "/approvals", admin, "{\"decision\":\"pending\"}"), 201);

        Reply rejected = expect(fx.call("POST", item + ":reject", admin, "{\"reason\":\"Add a rubric\"}"), 200);
        assertThat(rejected.body().path("status").asString()).isEqualTo("draft");
        JsonNode approval = expect(fx.call("GET", item + "/approvals", admin, null), 200).body().path("data").get(0);
        assertThat(approval.path("decision").asString()).isEqualTo("rejected");
        assertThat(approval.path("comments").asString()).isEqualTo("Add a rubric");
        assertThat(events(assignment)).as("the assignment itself did not change").isEmpty();

        expect(fx.call("POST", item + "/approvals", admin, "{\"decision\":\"pending\"}"), 201);
        Reply approved = expect(fx.call("POST", item + ":approve", admin, "{}"), 200);
        assertThat(approved.body().path("status").asString()).isEqualTo("published");
        expect(fx.call("POST", item + ":approve", admin, "{}"), 409);
        Reply finished = expect(fx.call("POST", item + ":finish", admin, "{}"), 200);
        assertThat(finished.body().path("status").asString()).isNotEqualTo("published");
        expect(fx.call("POST", item + ":finish", admin, "{}"), 409);
        assertThat(events(assignment)).contains("assessment.assignment.approve", "assessment.assignment.finish");
        expect(fx.call("POST", item + ":finish", fx.member(tenant, UUID.randomUUID(),
                "assessment.assignment.finish_assignment"), "{}"), 404);
    }

    private String assignment(String status, String maxMarks) throws Exception {
        UUID id = UUID.randomUUID();
        fx.inTenant(tenant, () -> database.jdbc().sql("INSERT INTO assessment.assignment (id, tenant_id, "
                        + "institution_id, title, class_section_id, max_marks, status) VALUES (:id, :tenantId, "
                        + ":institution, 'Essay', :section, :max, :status)")
                .param("id", id).param("institution", institution).param("section", UUID.randomUUID())
                .param("max", new java.math.BigDecimal(maxMarks)).param("status", status).update());
        return id.toString();
    }

    private List<String> events(String assignment) {
        return fx.inTenant(tenant, () -> database.jdbc().sql("SELECT event_type FROM assessment.outbox_event "
                        + "WHERE tenant_id = :tenantId AND aggregate_id = :id AND event_type LIKE 'assessment.assignment.%' "
                        + "ORDER BY id").param("id", UUID.fromString(assignment)).query(String.class).list());
    }

    private static String filter(String field, String value) {
        return java.net.URLEncoder.encode(field + " = \"" + value + "\"", java.nio.charset.StandardCharsets.UTF_8);
    }
}
