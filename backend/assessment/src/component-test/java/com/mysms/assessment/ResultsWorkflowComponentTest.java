package com.mysms.assessment;

import static com.mysms.assessment.AssessmentFixtures.band;
import static com.mysms.assessment.AssessmentFixtures.expect;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.assessment.AssessmentFixtures.Reply;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
 * Results over HTTP against real PostgreSQL as the application role: generating a marksheet from finalized papers
 * (numbers worked by hand below), what keeps a generation from running twice or on changed grading, validating and
 * publishing, cancelling to correct marks and generating again, and who reads a student's results.
 *
 * <p>The session of {@link #generationGradesAndDecidesPassFromThePapers} has two finalized papers and a cancelled one.
 * Bands A 90.00–100.00 (10 points), B 75.00–89.99 (8), C 40.00–74.99 (5) and F 0.00–39.99 (0, failing):
 * <pre>
 *   paper 1 (out of 200, 4 credits)  paper 2 (out of 50, 2 credits)   total        line
 *   S1: 179.99 → 89.995 → 90.00  A   40.00 → 80.00 B                 219.99/250   88.00 B pass  points 56, SGPA 9.333
 *   S2: 190.00 → 95.00 A             absent                          190.00/250   76.00 B FAIL  (absent paper)
 *   S3: 100.00 → 50.00 C             exempted (no maximum, no credit) 100.00/200   50.00 C pass  points 20, SGPA 5.000
 *   S4: only on the cancelled paper: no line
 * </pre>
 * 89.995 rounds half-up to 90.00 at the bands' scale; kept at four places it would fall between B and A.
 */
@SpringBootTest(classes = AssessmentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResultsWorkflowComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TEMPLATES = "/v1/assessment/result-templates/";
    private static final String MARKSHEETS = "/v1/assessment/marksheets/";
    private static final String STUDENTS = "/v1/assessment/students/";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(AssessmentDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private AssessmentFixtures fx;
    private UUID tenant;
    private UUID institution;
    private String admin;

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
    }

    /** The session of the class comment: three papers, four students, the bands and a draft template. */
    private final class Term {
        final UUID session = fx.session(tenant, institution);
        final UUID scheme = fx.scheme(tenant, institution, "percentage_band",
                band("A", "90.00", "100.00", "10", true), band("B", "75.00", "89.99", "8", true),
                band("C", "40.00", "74.99", "5", true), band("F", "0.00", "39.99", "0", false));
        final UUID paper1 = fx.exam(tenant, session, "result_updated", "200", "80");
        final UUID paper2 = fx.exam(tenant, session, "done", "50", "20");
        final UUID cancelled = fx.exam(tenant, session, "cancelled", "100", "40");
        final UUID s1 = UUID.randomUUID();
        final UUID s2 = UUID.randomUUID();
        final UUID s3 = UUID.randomUUID();
        final UUID s4 = UUID.randomUUID();
        final UUID s1Paper1 = fx.result(tenant, paper1, s1, "present", "179.99", "200", "pass", "4");
        final UUID template = fx.template(tenant, session, scheme);

        Term() {
            fx.result(tenant, paper2, s1, "present", "40.00", "50", "pass", "2");
            fx.result(tenant, paper1, s2, "present", "190.00", "200", "pass", "4");
            fx.result(tenant, paper2, s2, "absent", null, "50", "absent", "2");
            fx.result(tenant, paper1, s3, "present", "100.00", "200", "pass", "4");
            fx.result(tenant, paper2, s3, "exempted", null, "50", null, "2");
            fx.result(tenant, cancelled, s4, "expected", null, "100", null, null);
        }

        UUID generate() throws Exception {
            Reply operation = expect(fx.call("POST", TEMPLATES + template + ":generateResults", admin, "{}"), 202);
            assertThat(operation.body().path("status").asString()).isEqualTo("succeeded");
            return UUID.fromString(operation.body().path("result").path("marksheetId").asString());
        }
    }

    @Test
    void generationGradesAndDecidesPassFromThePapers() throws Exception {
        Term term = new Term();
        UUID marksheet = term.generate();

        JsonNode sheet = expect(fx.call("GET", MARKSHEETS + marksheet, admin, null), 200).body();
        assertThat(sheet.path("totalPassed").asInt()).isEqualTo(2);
        assertThat(sheet.path("totalFailed").asInt()).isEqualTo(1);
        Map<String, Map<String, Object>> lines = lines(marksheet);
        assertThat(lines).containsOnlyKeys(term.s1.toString(), term.s2.toString(), term.s3.toString());
        assertLine(lines.get(term.s1.toString()), "219.99", "88.00", "B", "pass", "6", "6", "56", "9.333");
        assertLine(lines.get(term.s2.toString()), "190.00", "76.00", "B", "fail", "6", "4", null, null);
        assertLine(lines.get(term.s3.toString()), "100.00", "50.00", "C", "pass", "4", "4", "20", "5.000");

        Map<String, String> grades = grades(term.session);
        assertThat(grades).containsEntry(term.s1Paper1.toString(), "A|10.00");
        assertThat(grades.values()).containsExactlyInAnyOrder("A|10.00", "B|8.00", "A|10.00", "null|null", "C|5.00",
                "null|null", "unlinked");
        assertThat(fx.value(tenant, "SELECT status FROM assessment.result_template WHERE tenant_id = :tenantId "
                + "AND id = :id", String.class, "id", term.template)).isEqualTo("result_generated");
    }

    /**
     * A pass_fail scheme with a gap between its bands (Pass 40.00–74.00, Merit 75.00–100.00), one paper out of 200:
     * <pre>
     *   149/200 = 74.50  between the bands: no grade; paper passed  → line "Pass", pass
     *    79/200 = 39.50  in no band; paper failed                   → line "Fail", fail
     *   190/200 = 95.00  Merit (the paper's grade)                  → line "Pass", pass (the label is the decision)
     * </pre>
     */
    @Test
    void aPassFailSchemeLabelsTheDecisionAndAPercentageBetweenBandsHasNoGrade() throws Exception {
        UUID session = fx.session(tenant, institution);
        UUID scheme = fx.scheme(tenant, institution, "pass_fail", band("Pass", "40.00", "74.00", "0", true),
                band("Merit", "75.00", "100.00", "0", true));
        UUID paper = fx.exam(tenant, session, "done", "200", "80");
        UUID between = UUID.randomUUID();
        UUID failed = UUID.randomUUID();
        UUID merit = UUID.randomUUID();
        UUID betweenResult = fx.result(tenant, paper, between, "present", "149", "200", "pass", null);
        fx.result(tenant, paper, failed, "present", "79", "200", "fail", null);
        UUID meritResult = fx.result(tenant, paper, merit, "present", "190", "200", "pass", null);
        UUID template = fx.template(tenant, session, scheme);

        Reply operation = expect(fx.call("POST", TEMPLATES + template + ":generateResults", admin, "{}"), 202);
        Map<String, Map<String, Object>> lines =
                lines(UUID.fromString(operation.body().path("result").path("marksheetId").asString()));
        assertThat((BigDecimal) lines.get(between.toString()).get("percentage")).isEqualByComparingTo("74.50");
        assertThat(lines.get(between.toString())).containsEntry("grade_label", "Pass")
                .containsEntry("result_status", "pass");
        assertThat((BigDecimal) lines.get(failed.toString()).get("percentage")).isEqualByComparingTo("39.50");
        assertThat(lines.get(failed.toString())).containsEntry("grade_label", "Fail")
                .containsEntry("result_status", "fail");
        assertThat(lines.get(merit.toString())).containsEntry("grade_label", "Pass")
                .containsEntry("result_status", "pass");
        Map<String, String> grades = grades(session);
        assertThat(grades).as("a percentage between two bands gets no grade")
                .containsEntry(betweenResult.toString(), "null|null")
                .containsEntry(meritResult.toString(), "Merit|0.00");
    }

    @Test
    void aGeneratedSessionIsNotGeneratedAgainAndItsGradingDoesNotChange() throws Exception {
        Term term = new Term();
        term.generate();

        Reply again = expect(fx.call("POST", TEMPLATES + term.template + ":generateResults", admin, "{}"), 409);
        assertThat(again.body().path("code").asString()).isEqualTo("INVALID_STATE_TRANSITION");
        UUID second = fx.template(tenant, term.session, term.scheme);
        Reply secondTemplate = expect(fx.call("POST", TEMPLATES + second + ":generateResults", admin, "{}"), 409);
        assertThat(secondTemplate.body().path("detail").asString()).contains("already belong to a marksheet");

        JsonNode band = expect(fx.call("GET", "/v1/assessment/grading-bands?filter="
                + java.net.URLEncoder.encode("gradingSchemeId = \"" + term.scheme + "\" AND gradeLabel = \"A\"",
                java.nio.charset.StandardCharsets.UTF_8), admin, null), 200).body().path("data").get(0);
        expect(fx.call("PATCH", "/v1/assessment/grading-bands/" + band.path("id").asString() + "?updateMask=minPercent",
                admin, "{\"minPercent\":\"85\"}", band.path("etag").asString()), 409);
        expect(fx.call("POST", "/v1/assessment/grading-bands", admin, "{\"gradingSchemeId\":\"" + term.scheme
                + "\",\"minPercent\":\"0\",\"maxPercent\":\"1\",\"gradeLabel\":\"Z\"}"), 409);
        String schemeEtag = expect(fx.call("GET", "/v1/assessment/grading-schemes/" + term.scheme, admin, null), 200)
                .body().path("etag").asString();
        expect(fx.call("PATCH", "/v1/assessment/grading-schemes/" + term.scheme + "?updateMask=gradingMethod", admin,
                "{\"gradingMethod\":\"pass_fail\"}", schemeEtag), 409);

        Reply correction = fx.call("POST", "/v1/assessment/exams/" + term.paper1 + ":recordMarks", admin,
                "{\"entries\":[{\"studentId\":\"" + term.s1 + "\",\"attendanceStatus\":\"present\","
                        + "\"marksObtained\":\"150\"}]}");
        assertThat(correction.status()).as("on a marksheet: " + correction.text()).isEqualTo(409);
    }

    @Test
    void validatedPublishedThenCancelledToCorrectAndGeneratedAgain() throws Exception {
        Term term = new Term();
        UUID first = term.generate();

        String etag = expect(fx.call("GET", MARKSHEETS + first, admin, null), 200).body().path("etag").asString();
        Reply validated = expect(fx.call("POST", MARKSHEETS + first + ":validate", admin, "{}"), 200);
        assertThat(validated.body().path("status").asString()).isEqualTo("validated");
        expect(fx.call("POST", MARKSHEETS + first + ":publish", admin, "{\"visibleToGuardians\":true}", etag), 412);
        String current = validated.body().path("etag").asString();
        Reply published = expect(fx.call("POST", MARKSHEETS + first + ":publish", admin,
                "{\"visibleToGuardians\":true}", current), 200);
        assertThat(published.body().path("publishedAt").asString()).isNotEmpty();
        expect(fx.call("POST", MARKSHEETS + first + ":publish", admin, "{}",
                published.body().path("etag").asString()), 409);

        expect(fx.call("POST", MARKSHEETS + first + ":cancel", admin, "{\"reason\":\"Paper 1 mis-marked\"}"), 200);
        assertThat(grades(term.session).values()).as("every result released").containsOnly("unlinked");
        assertThat(fx.value(tenant, "SELECT status FROM assessment.result_template WHERE tenant_id = :tenantId "
                + "AND id = :id", String.class, "id", term.template)).isEqualTo("draft");
        Reply reset = expect(fx.call("POST", MARKSHEETS + first + ":resetToDraft", admin, "{}"), 409);
        assertThat(reset.body().path("detail").asString()).contains("generate the results again");

        expect(fx.call("POST", "/v1/assessment/exams/" + term.paper1 + ":recordMarks", admin,
                "{\"entries\":[{\"studentId\":\"" + term.s1 + "\",\"attendanceStatus\":\"present\","
                        + "\"marksObtained\":\"150\"}]}"), 200);
        expect(fx.call("POST", TEMPLATES + term.template + ":generateResults", admin, "{}"), 409);
        expect(fx.call("POST", "/v1/assessment/exams/" + term.paper1 + ":markResultsUpdated", admin, "{}"), 200);
        UUID second = term.generate();
        assertThat(second).isNotEqualTo(first);
        // 150/200 = 75.00 B (8 × 4) and 40/50 = 80.00 B (8 × 2): 190.00/250 = 76.00 B, points 48, SGPA 8.000
        assertLine(lines(second).get(term.s1.toString()), "190.00", "76.00", "B", "pass", "6", "6", "48", "8.000");
        assertThat(fx.value(tenant, "SELECT count(*) FROM assessment.outbox_event WHERE tenant_id = :tenantId "
                + "AND event_type = 'assessment.exam_result.resultReleased'", Long.class))
                .as("one release event per released result").isEqualTo(6L);
    }

    @Test
    void staffReadStudentResultsInTheirSchoolAndStudentsOnlyThroughSis() throws Exception {
        Term term = new Term();
        UUID marksheet = term.generate();
        expect(fx.call("POST", MARKSHEETS + marksheet + ":validate", admin, "{}"), 200);
        String results = STUDENTS + term.s1 + "/results";

        String teacher = fx.member(tenant, institution, "assessment.marksheet_line.get_student_results",
                "assessment.marksheet_line.read");
        JsonNode cards = expect(fx.call("GET", results, teacher, null), 200).body().path("data");
        assertThat(cards).as("staff read validated results before they are published").hasSize(1);
        JsonNode card = cards.get(0);
        assertThat(card.path("marksheetLine").path("gradeLabel").asString()).isEqualTo("B");
        assertThat(card.path("results")).hasSize(2);

        String elsewhere = fx.member(tenant, UUID.randomUUID(), "assessment.marksheet_line.get_student_results",
                "assessment.marksheet_line.read");
        expect(fx.call("GET", results, elsewhere, null), 404);
        Reply otherTenant = expect(fx.call("GET", results, fx.admin(UUID.randomUUID()), null), 200);
        assertThat(otherTenant.body().path("data")).as("another tenant has no results of the student").isEmpty();

        String student = fx.member(tenant, institution, "assessment.marksheet_line.get_student_results");
        Reply asked = fx.call("GET", results, student, null);
        assertThat(asked.status()).as("students are confirmed by sis, which is not running: " + asked.text())
                .isEqualTo(503);
    }

    @Test
    void twoGenerationsOfOneTemplateRunOneAfterTheOther() throws Exception {
        Term term = new Term();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> generate = () ->
                    fx.call("POST", TEMPLATES + term.template + ":generateResults", admin, "{}").status();
            List<Future<Integer>> runs = new ArrayList<>(List.of(pool.submit(generate), pool.submit(generate)));
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> run : runs) {
                statuses.add(run.get());
            }
            assertThat(statuses).containsExactlyInAnyOrder(202, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(fx.value(tenant, "SELECT count(*) FROM assessment.marksheet WHERE tenant_id = :tenantId",
                Long.class)).isEqualTo(1L);
        assertThat(fx.value(tenant, "SELECT count(*) FROM assessment.marksheet_line WHERE tenant_id = :tenantId",
                Long.class)).isEqualTo(3L);
    }

    @Test
    void generationWaitsForEveryPaperAndAttendee() throws Exception {
        Term term = new Term();
        UUID late = fx.exam(tenant, term.session, "held", "10", "5");
        fx.result(tenant, late, term.s1, "present", "5", "10", "pass", null);
        Reply early = expect(fx.call("POST", TEMPLATES + term.template + ":generateResults", admin, "{}"), 409);
        assertThat(early.body().path("detail").asString()).contains("finalized marks");
        fx.inTenant(tenant, () -> database.jdbc().sql("UPDATE assessment.exam SET status = 'result_updated' "
                + "WHERE tenant_id = :tenantId AND id = :id").param("id", late).update());
        fx.result(tenant, late, term.s2, "present", null, "10", "pending", null);
        Reply pending = expect(fx.call("POST", TEMPLATES + term.template + ":generateResults", admin, "{}"), 409);
        assertThat(pending.body().path("detail").asString()).contains("final attendance");
        expect(fx.call("POST", TEMPLATES + term.template + ":generateResults",
                fx.member(tenant, UUID.randomUUID(), "assessment.result_template.generate_results"), "{}"), 404);
    }

    private Map<String, Map<String, Object>> lines(UUID marksheet) {
        return fx.inTenant(tenant, () -> {
            Map<String, Map<String, Object>> rows = new java.util.HashMap<>();
            database.jdbc().sql("SELECT student_id, total_marks, percentage, grade_label, result_status, "
                            + "credits_attempted, credits_earned, grade_points, sgpa, cgpa FROM assessment.marksheet_line "
                            + "WHERE tenant_id = :tenantId AND marksheet_id = :marksheet AND deleted_at IS NULL")
                    .param("marksheet", marksheet).query().listOfRows()
                    .forEach(row -> rows.put(row.get("student_id").toString(), row));
            return rows;
        });
    }

    /** Each result of the session's papers: "label|point", or "unlinked" when it belongs to no marksheet line. */
    private Map<String, String> grades(UUID session) {
        return fx.inTenant(tenant, () -> {
            Map<String, String> rows = new java.util.HashMap<>();
            database.jdbc().sql("SELECT r.id, r.grade_label, r.grade_point, r.marksheet_line_id "
                            + "FROM assessment.exam_result r JOIN assessment.exam e ON e.tenant_id = :tenantId "
                            + "AND e.id = r.exam_id WHERE r.tenant_id = :tenantId AND e.exam_session_id = :session")
                    .param("session", session).query().listOfRows()
                    .forEach(row -> rows.put(row.get("id").toString(), row.get("marksheet_line_id") == null
                            ? "unlinked" : row.get("grade_label") + "|" + row.get("grade_point")));
            return rows;
        });
    }

    private static void assertLine(Map<String, Object> line, String total, String percentage, String grade,
            String status, String attempted, String earned, String points, String sgpa) {
        assertThat((BigDecimal) line.get("total_marks")).isEqualByComparingTo(total);
        assertThat((BigDecimal) line.get("percentage")).isEqualByComparingTo(percentage);
        assertThat(line.get("grade_label")).isEqualTo(grade);
        assertThat(line.get("result_status")).isEqualTo(status);
        assertThat((BigDecimal) line.get("credits_attempted")).isEqualByComparingTo(attempted);
        assertThat((BigDecimal) line.get("credits_earned")).isEqualByComparingTo(earned);
        assertDecimal(line.get("grade_points"), points);
        assertDecimal(line.get("sgpa"), sgpa);
        assertDecimal(line.get("cgpa"), sgpa);
    }

    private static void assertDecimal(Object actual, String expected) {
        if (expected == null) {
            assertThat(actual).isNull();
        } else {
            assertThat((BigDecimal) actual).isEqualByComparingTo(expected);
        }
    }
}
