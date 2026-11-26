package com.mysms.learning.lms;

import com.mysms.learning.LearningApplication;
import static com.mysms.learning.lms.LmsFixtures.expect;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.learning.lms.LmsFixtures.Reply;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Plagiarism checks over HTTP against real PostgreSQL: SubmitForPlagiarismCheck's refusals (no account configured —
 * 503 — and an unknown account), the report-link guards, and Turnitin's signed callback moving a report to complete
 * with its similarity score. Turnitin itself is not running here, so only the callback's local effects are proven.
 */
@SpringBootTest(classes = LearningApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlagiarismComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String WEBHOOK = "/v1/lms/integrations/plagiarism/turnitin:webhook";
    private static final String SECRET = "turnitin-shared-secret";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(LmsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private FieldCipher cipher;

    private LmsFixtures fx;
    private UUID tenant;
    private UUID adminId;
    private String admin;

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
        adminId = UUID.randomUUID();
        admin = fx.token(adminId, tenant, List.of("*"), List.of("*"), "*");
    }

    private UUID account() {
        UUID account = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.plagiarism_account (id, tenant_id, provider, account_ref, api_url, "
                        + "shared_secret_enc, is_default) VALUES (:id, :tenantId, 'turnitin', 'acct-1', "
                        + "'https://tii.example.com', :enc, true)",
                "id", account, "enc", cipher.encrypt(SECRET, tenant, "lms.plagiarism_account.shared_secret_enc"));
        return account;
    }

    /** A report the administrator sent, of a file of no lms record. */
    private UUID report(UUID account, String ref) {
        UUID report = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.plagiarism_report (id, tenant_id, plagiarism_account_id, file_id, "
                        + "provider_submission_ref, status, created_by) VALUES (:id, :tenantId, :account, :file, :ref, "
                        + "'uploaded', :by)",
                "id", report, "account", account, "file", UUID.randomUUID(), "ref", ref, "by", adminId);
        return report;
    }

    @Test
    void theReportLinkReachesOnlyTheSubmissionsStudentAndTheCohortsTeachers() throws Exception {
        UUID institution = UUID.randomUUID();
        UUID cohort = fx.cohort(tenant, institution);
        UUID assignment = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.assignment (id, tenant_id, institution_id, title, question, submission_type) "
                + "VALUES (:id, :tenantId, :institution, 'Essay', 'Write.', 'document')", "id", assignment,
                "institution", institution);
        UUID item = fx.contentItem(tenant, cohort, "assignment", "assignment_id", assignment, "20");
        UUID studentId = UUID.randomUUID();
        UUID submission = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.assignment_submission (id, tenant_id, assignment_id, user_id, content_item_id) "
                + "VALUES (:id, :tenantId, :assignment, :user, :item)", "id", submission, "assignment", assignment,
                "user", studentId, "item", item);
        UUID report = report(account(), null);
        fx.sql(tenant, "UPDATE lms.plagiarism_report SET provider_submission_ref = NULL, created_by = :student, "
                + "entity_type = 'lms.assignment_submission', entity_id = :submission WHERE id = :id",
                "id", report, "student", studentId, "submission", submission);
        String link = "/v1/lms/plagiarism-reports/" + report + ":reportLink";
        String permission = "lms.plagiarism_report.get_plagiarism_report_link";
        // reached: the next refusal is the report's own state (409), not the caller's reach
        expect(fx.call("GET", link, fx.user(studentId, tenant, institution, permission), null), 409);
        UUID teacher = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.cohort_teacher (id, tenant_id, cohort_id, user_id) VALUES (:id, :tenantId, "
                + ":cohort, :user)", "id", UUID.randomUUID(), "cohort", cohort, "user", teacher);
        expect(fx.call("GET", link, fx.user(teacher, tenant, institution, permission), null), 409);
        // another student, even one holding the old tenant-wide teacher view, and another cohort's teacher: 404
        expect(fx.call("GET", link, fx.member(tenant, institution, permission, "lms.plagiarism_account.read"), null),
                404);
        UUID otherCohort = fx.cohort(tenant, institution);
        UUID otherTeacher = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.cohort_teacher (id, tenant_id, cohort_id, user_id) VALUES (:id, :tenantId, "
                + ":cohort, :user)", "id", UUID.randomUUID(), "cohort", otherCohort, "user", otherTeacher);
        expect(fx.call("GET", link, fx.user(otherTeacher, tenant, institution, permission), null), 404);
    }

    @Test
    void submittingWithoutAnAccountIs503AndAnUnknownAccountIs422() throws Exception {
        String body = "{\"entityAttachmentId\":\"" + UUID.randomUUID() + "\"}";
        Reply unconfigured = expect(fx.call("POST", "/v1/lms/plagiarism-reports:submit", admin, body), 503);
        assertThat(unconfigured.body().path("code").asString()).isEqualTo("PLAGIARISM_NOT_CONFIGURED");
        account();
        expect(fx.call("POST", "/v1/lms/plagiarism-reports:submit", admin,
                "{\"entityAttachmentId\":\"" + UUID.randomUUID() + "\",\"plagiarismAccountId\":\""
                        + UUID.randomUUID() + "\"}"), 422);
    }

    @Test
    void theReportLinkNeedsASubmissionAtTheProvider() throws Exception {
        UUID report = report(account(), null);
        fx.sql(tenant, "UPDATE lms.plagiarism_report SET provider_submission_ref = NULL WHERE id = :id",
                "id", report);
        expect(fx.call("GET", "/v1/lms/plagiarism-reports/" + report + ":reportLink", admin, null), 409);
        expect(fx.call("GET", "/v1/lms/plagiarism-reports/" + UUID.randomUUID() + ":reportLink", admin, null), 404);
        expect(fx.call("GET", "/v1/lms/plagiarism-reports/" + report + ":reportLink?view=nonsense", admin, null),
                422);
    }

    @Test
    void aSignedSimilarityCallbackCompletesTheReport() throws Exception {
        UUID account = account();
        UUID report = report(account, "sub-9");
        String body = """
                {"event_type":"SIMILARITY_COMPLETE","submission_id":"sub-9","status":"COMPLETE",
                 "overall_match_percentage":37,
                 "metadata":{"custom":{"mysms_tenant":"%s","mysms_report":"%s"}}}
                """.formatted(tenant, report);
        long now = System.currentTimeMillis() / 1000;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal((now + "." + body)
                .getBytes(StandardCharsets.UTF_8)));
        expect(fx.call("POST", WEBHOOK, null, body, "X-Signature", signature,
                "X-Timestamp", Long.toString(now)), 204);
        assertThat(fx.value(tenant, "SELECT status FROM lms.plagiarism_report WHERE id = :id", String.class,
                "id", report)).isEqualTo("complete");
        assertThat(fx.value(tenant, "SELECT similarity_score FROM lms.plagiarism_report WHERE id = :id",
                String.class, "id", report)).isEqualTo("37");
        expect(fx.call("POST", WEBHOOK, null, body, "X-Signature", "bad", "X-Timestamp", Long.toString(now)), 401);
        expect(fx.call("POST", "/v1/lms/integrations/plagiarism/acme:webhook", null, body,
                "X-Signature", signature, "X-Timestamp", Long.toString(now)), 404);
    }
}
