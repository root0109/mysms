package com.mysms.learning.lms;

import com.mysms.learning.LearningApplication;
import static com.mysms.learning.lms.LmsFixtures.expect;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.learning.lms.LmsFixtures.Reply;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.time.OffsetDateTime;
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
 * Three lms workflows over HTTP against real PostgreSQL: the access log read only by tenant administrators within a
 * dated range, ShareDriveItem replacing who sees the caller's own item, and SubmitExercise judging a student's
 * outputs against the exercise's test cases with hidden cases kept hidden.
 */
@SpringBootTest(classes = LearningApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LearningOpsComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(LmsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private LmsFixtures fx;
    private UUID tenant;
    private UUID institution;
    private String admin;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "lms");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @BeforeEach
    void school() {
        fx = new LmsFixtures(TOKENS, database, port);
        tenant = UUID.randomUUID();
        institution = UUID.randomUUID();
        admin = fx.admin(tenant);
    }

    // ---- access logs

    @Test
    void accessLogsAreReadByTenantAdministratorsWithinADatedRange() throws Exception {
        UUID log = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.access_log (id, tenant_id, user_id, accessed_at, operation, url) "
                        + "VALUES (:id, :tenantId, :user, :at, 'lms.GetLesson', '/v1/lms/lessons/1')",
                "id", log, "user", UUID.randomUUID(), "at", OffsetDateTime.parse("2026-11-10T08:00:00Z"));
        String range = "accessedAt >= \"2026-11-01T00:00:00Z\" AND accessedAt < \"2026-12-01T00:00:00Z\"";
        String query = "?filter=" + java.net.URLEncoder.encode(range, java.nio.charset.StandardCharsets.UTF_8);
        Reply list = expect(fx.call("GET", "/v1/lms/access-logs" + query, admin, null), 200);
        assertThat(list.body().path("data")).hasSize(1);
        assertThat(list.body().path("data").get(0).path("operation").asString()).isEqualTo("lms.GetLesson");
        expect(fx.call("GET", "/v1/lms/access-logs", admin, null), 400);
        expect(fx.call("GET", "/v1/lms/access-logs" + query,
                fx.member(tenant, institution, "lms.access_log.read"), null), 403);
        Reply one = expect(fx.call("GET", "/v1/lms/access-logs/" + log, admin, null), 200);
        assertThat(one.body().path("url").asString()).isEqualTo("/v1/lms/lessons/1");
        expect(fx.call("GET", "/v1/lms/access-logs/" + log, fx.admin(UUID.randomUUID()), null), 404);
    }

    // ---- drive share

    @Test
    void shareDriveItemReplacesTheListAndOnlyTheOwnerShares() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String owner = fx.user(ownerId, tenant, institution, "lms.drive_item.share_drive_item");
        UUID item = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.drive_item (id, tenant_id, owner_user_id, title, link_url) "
                + "VALUES (:id, :tenantId, :owner, 'Notes', 'https://notes')", "id", item, "owner", ownerId);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        String path = "/v1/lms/drive-items/" + item + ":share";
        Reply shared = expect(fx.call("POST", path, owner,
                "{\"userIds\":[\"" + alice + "\",\"" + bob + "\"]}"), 200);
        assertThat(shared.body().path("items")).hasSize(2);
        Reply replaced = expect(fx.call("POST", path, owner, "{\"userIds\":[\"" + bob + "\"]}"), 200);
        assertThat(replaced.body().path("items")).hasSize(1);
        assertThat(replaced.body().path("items").get(0).path("userId").asString()).isEqualTo(bob.toString());
        Reply unshared = expect(fx.call("POST", path, owner, "{\"userIds\":[]}"), 200);
        assertThat(unshared.body().path("items")).isEmpty();
        assertThat(fx.value(tenant, "SELECT is_shared FROM lms.drive_item WHERE id = :id", Boolean.class,
                "id", item)).isFalse();
        expect(fx.call("POST", path, owner, "{\"userIds\":[\"" + ownerId + "\"]}"), 422);
        expect(fx.call("POST", path, admin, "{\"userIds\":[\"" + alice + "\"]}"), 404);
        expect(fx.call("POST", "/v1/lms/drive-items/" + UUID.randomUUID() + ":share", owner,
                "{\"userIds\":[]}"), 404);
    }

    // ---- exercises

    @Test
    void submitExerciseJudgesTheOutputsAndHidesHiddenCases() throws Exception {
        UUID exercise = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.programming_exercise (id, tenant_id, title, problem_statement, language) "
                + "VALUES (:id, :tenantId, 'FizzBuzz', 'Write it.', 'python')", "id", exercise);
        UUID visible = UUID.randomUUID();
        UUID hidden = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.exercise_test_case (id, tenant_id, exercise_id, sort_order, input, "
                        + "expected_output, is_hidden) VALUES (:v, :tenantId, :exercise, 1, '3', 'Fizz', false), "
                        + "(:h, :tenantId, :exercise, 2, '5', 'Buzz', true)",
                "v", visible, "h", hidden, "exercise", exercise);
        String student = fx.member(tenant, institution, "lms.programming_exercise.submit_exercise",
                "lms.exercise_submission.read", "lms.exercise_test_result.read");
        String path = "/v1/lms/programming-exercises/" + exercise + ":submit";
        // 'Fizz\n' equals 'Fizz' once trailing white space is set aside; 'Fuzz' fails the hidden case
        Reply failed = expect(fx.call("POST", path, student, "{\"code\":\"print(x)\",\"testOutputs\":["
                + "{\"testCaseId\":\"" + visible + "\",\"actualOutput\":\"Fizz\\n\"},"
                + "{\"testCaseId\":\"" + hidden + "\",\"actualOutput\":\"Fuzz\"}]}"), 200);
        assertThat(failed.body().path("status").asString()).isEqualTo("failed");
        Reply passed = expect(fx.call("POST", path, student, "{\"code\":\"print(y)\",\"testOutputs\":["
                + "{\"testCaseId\":\"" + visible + "\",\"actualOutput\":\"Fizz\"},"
                + "{\"testCaseId\":\"" + hidden + "\",\"actualOutput\":\"Buzz\"}]}"), 200);
        assertThat(passed.body().path("status").asString()).isEqualTo("passed");
        Reply results = expect(fx.call("GET", "/v1/lms/exercise-submissions/"
                + failed.body().path("id").asString() + "/test-results", student, null), 200);
        assertThat(results.body().path("data")).hasSize(2);
        results.body().path("data").forEach(result -> {
            if (result.path("testCaseId").asString().equals(hidden.toString())) {
                assertThat(result.path("expectedOutput").isNull()
                        || result.path("expectedOutput").asString("").isEmpty())
                        .as("a hidden case's expectation stays hidden").isTrue();
            }
        });
        expect(fx.call("POST", "/v1/lms/programming-exercises/" + UUID.randomUUID() + ":submit", student,
                "{\"code\":\"x\",\"testOutputs\":[]}"), 404);
        expect(fx.call("POST", path, student, "{\"code\":\"x\",\"testOutputs\":[{\"testCaseId\":\""
                + UUID.randomUUID() + "\",\"actualOutput\":\"y\"}]}"), 422);
    }
}
