package com.mysms.learning.lms;

import com.mysms.learning.LearningApplication;
import static com.mysms.learning.lms.LmsFixtures.expect;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.learning.lms.LmsFixtures.Reply;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
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
 * Gradebook grading and assignment-submission workflows over HTTP against real PostgreSQL: a teacher's GradeEntry
 * with its guards, BatchGradeEntries with per-entry outcomes, a student finalizing their own submission, the
 * teacher's evaluation, the draft-only deletes, and DeleteLmsAssignment keeping published or referenced assignments.
 */
@SpringBootTest(classes = LearningApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GradingWorkflowComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String GRADE = "/v1/lms/gradebook-entries:grade";
    private static final String BATCH = "/v1/lms/gradebook-entries:batchGrade";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(LmsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private LmsFixtures fx;
    private UUID tenant;
    private UUID institution;
    private String admin;
    private UUID cohort;
    private UUID item;
    private UUID studentId;

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
        cohort = fx.cohort(tenant, institution);
        item = fx.contentItem(tenant, cohort, "link", "link_url", "https://example.com", "20");
        studentId = UUID.randomUUID();
        enroll(studentId);
    }

    private void enroll(UUID user) {
        fx.sql(tenant, "INSERT INTO lms.cohort_enrollment (id, tenant_id, cohort_id, user_id) "
                + "VALUES (:id, :tenantId, :cohort, :user)", "id", UUID.randomUUID(), "cohort", cohort, "user", user);
    }

    private String gradeBody(UUID itemId, UUID user, String points, String etag) {
        return "{\"contentItemId\":\"" + itemId + "\",\"userId\":\"" + user + "\""
                + (etag == null ? "" : ",\"etag\":\"\\\"" + etag + "\\\"\"")
                + ",\"grade\":{\"points\":\"" + points + "\",\"feedback\":\"Good work\"}}";
    }

    // ---- GradeEntry

    @Test
    void gradeEntryWritesTheEntryItsHistoryAndGuardsTheVersion() throws Exception {
        Reply graded = expect(fx.call("POST", GRADE, admin, gradeBody(item, studentId, "15.5", null)), 200);
        assertThat(graded.body().path("points").asString()).isEqualTo("15.50");
        assertThat(graded.body().path("manualPoints").asString()).isEqualTo("15.50");
        assertThat(graded.body().path("status").asString()).isEqualTo("graded");
        assertThat(fx.value(tenant, "SELECT count(*) FROM lms.grade_change_log", Integer.class)).isEqualTo(1);
        String etag = graded.body().path("etag").asString().replace("\"", "");
        expect(fx.call("POST", GRADE, admin, gradeBody(item, studentId, "16", "99")), 412);
        Reply regraded = expect(fx.call("POST", GRADE, admin, gradeBody(item, studentId, "16", etag)), 200);
        assertThat(regraded.body().path("points").asString()).isEqualTo("16.00");
        assertThat(fx.value(tenant, "SELECT count(*) FROM lms.grade_change_log", Integer.class)).isEqualTo(2);
    }

    @Test
    void gradeEntryRefusesWhatItMust() throws Exception {
        expect(fx.call("POST", GRADE, admin, gradeBody(item, UUID.randomUUID(), "10", null)), 422);
        expect(fx.call("POST", GRADE, admin, gradeBody(item, studentId, "20.01", null)), 422);
        expect(fx.call("POST", GRADE, admin, gradeBody(UUID.randomUUID(), studentId, "10", null)), 404);
        expect(fx.call("POST", GRADE, fx.member(tenant, institution, "lms.gradebook_entry.read"),
                gradeBody(item, studentId, "10", null)), 403);
        expect(fx.call("POST", GRADE, fx.admin(UUID.randomUUID()), gradeBody(item, studentId, "10", null)), 404);
        UUID ungraded = fx.contentItem(tenant, cohort, "link", "link_url", "https://example.com", null);
        fx.sql(tenant, "UPDATE lms.content_item SET is_graded = false WHERE id = :id", "id", ungraded);
        expect(fx.call("POST", GRADE, admin, gradeBody(ungraded, studentId, "10", null)), 422);
    }

    @Test
    void batchGradeEntriesAnswersEachEntrysOwnOutcome() throws Exception {
        UUID stranger = UUID.randomUUID();
        Reply batch = expect(fx.call("POST", BATCH, admin, "{\"entries\":["
                + "{\"contentItemId\":\"" + item + "\",\"userId\":\"" + studentId
                + "\",\"grade\":{\"points\":\"12\"}},"
                + "{\"contentItemId\":\"" + item + "\",\"userId\":\"" + stranger
                + "\",\"grade\":{\"points\":\"12\"}}]}"), 200);
        assertThat(batch.body().path("items")).hasSize(2);
        assertThat(batch.body().path("items").get(0).path("entry").path("points").asString()).isEqualTo("12.00");
        assertThat(batch.body().path("items").get(1).path("error").asString()).isEqualTo("VALIDATION_FAILED");
        expect(fx.call("POST", BATCH, admin, "{\"entries\":[]}"), 422);
    }

    // ---- submissions and deletes

    private UUID assignment(boolean published) {
        UUID assignment = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.assignment (id, tenant_id, institution_id, title, question, submission_type"
                        + (published ? ", published_at" : "") + ") VALUES (:id, :tenantId, :institution, 'Essay', "
                        + "'Write.', 'text'" + (published ? ", now()" : "") + ")",
                "id", assignment, "institution", institution);
        return assignment;
    }

    /** A submission to the assignment, placed in the cohort as a content item (so the cohort's teachers reach it). */
    private UUID submission(UUID assignment, UUID user, boolean isFinal) {
        UUID placement = fx.value(tenant, "SELECT id FROM lms.content_item WHERE assignment_id = :assignment",
                UUID.class, "assignment", assignment);
        if (placement == null) {
            placement = fx.contentItem(tenant, cohort, "assignment", "assignment_id", assignment, "20");
        }
        UUID submission = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.assignment_submission (id, tenant_id, assignment_id, user_id, answer, "
                        + "content_item_id, is_final" + (isFinal ? ", finalized_at" : "") + ") VALUES (:id, :tenantId, "
                        + ":assignment, :user, 'My answer', :item, :final" + (isFinal ? ", now()" : "") + ")",
                "id", submission, "assignment", assignment, "user", user, "item", placement, "final", isFinal);
        return submission;
    }

    @Test
    void aStudentFinalizesTheirOwnDraftAndTheTeacherEvaluatesIt() throws Exception {
        UUID assignment = assignment(false);
        UUID draft = submission(assignment, studentId, false);
        String student = fx.user(studentId, tenant, institution,
                "lms.assignment_submission.finalize_submission", "lms.assignment_submission.delete");
        String path = "/v1/lms/assignment-submissions/" + draft;
        expect(fx.call("POST", path + ":finalize", fx.user(UUID.randomUUID(), tenant, institution,
                "lms.assignment_submission.finalize_submission"), "{}"), 404);
        Reply finalized = expect(fx.call("POST", path + ":finalize", student, "{}"), 200);
        assertThat(finalized.body().path("isFinal").asBoolean()).isTrue();
        assertThat(finalized.body().path("finalizedAt").asString()).isNotEmpty();
        expect(fx.call("POST", path + ":finalize", student, "{}"), 409);

        expect(fx.call("POST", path + ":grade", student, "{\"status\":\"pass\"}"), 403);
        Reply gradedDraft = fx.call("POST", "/v1/lms/assignment-submissions/"
                + submission(assignment, UUID.randomUUID(), false) + ":grade", admin, "{\"status\":\"pass\"}");
        assertThat(gradedDraft.status()).as(gradedDraft.text()).isEqualTo(409);
        Reply graded = expect(fx.call("POST", path + ":grade", admin,
                "{\"status\":\"pass\",\"evaluatorComments\":\"Solid\"}"), 200);
        assertThat(graded.body().path("status").asString()).isEqualTo("pass");

        // final or evaluated submissions are evidence: the student cannot delete them
        expect(fx.call("DELETE", path, student, null, "If-Match", "\"2\""), 409);
        UUID other = submission(assignment, studentId, false);
        expect(fx.call("DELETE", "/v1/lms/assignment-submissions/" + other, student, null, "If-Match", "\"9\""), 412);
        expect(fx.call("DELETE", "/v1/lms/assignment-submissions/" + other, student, null, "If-Match", "\"0\""), 204);
    }

    @Test
    void anAssignmentIsDeletedOnlyWhileDraftAndUnreferenced() throws Exception {
        UUID draft = assignment(false);
        fx.sql(tenant, "UPDATE lms.assignment SET row_version = 1 WHERE id = :id", "id", draft);
        UUID referenced = assignment(false);
        fx.sql(tenant, "UPDATE lms.assignment SET row_version = 1 WHERE id = :id", "id", referenced);
        submission(referenced, studentId, false);
        UUID published = assignment(true);
        fx.sql(tenant, "UPDATE lms.assignment SET row_version = 1 WHERE id = :id", "id", published);
        expect(fx.call("DELETE", "/v1/lms/assignments/" + published, admin, null, "If-Match", "\"1\""), 409);
        expect(fx.call("DELETE", "/v1/lms/assignments/" + referenced, admin, null, "If-Match", "\"1\""), 409);
        expect(fx.call("DELETE", "/v1/lms/assignments/" + draft, admin, null, "If-Match", "\"9\""), 412);
        expect(fx.call("DELETE", "/v1/lms/assignments/" + draft, admin, null, "If-Match", "\"1\""), 204);
        expect(fx.call("DELETE", "/v1/lms/assignments/" + draft, admin, null, "If-Match", "\"1\""), 404);
    }
}
