package com.mysms.assessment;

import static com.mysms.assessment.AssessmentFixtures.expect;
import static com.mysms.assessment.AssessmentFixtures.json;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.assessment.AssessmentFixtures.Reply;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
 * The exam workflow over HTTP against real PostgreSQL as the application role: registering attendees and allocating
 * rooms (sis and facility are not running here, so after every local check the answer is 503 — and nothing is
 * written), recording marks with the attendance rules and the per-entry etag, closing and reopening marks entry, and
 * the immutability of the attendee rows. Each test works in a tenant of its own.
 */
@SpringBootTest(classes = AssessmentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExamWorkflowComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String EXAMS = "/v1/assessment/exams/";
    private static final String RESULTS = "/v1/assessment/exam-results/";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(AssessmentDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private AssessmentFixtures fx;
    private UUID tenant;
    private UUID institution;
    private UUID session;
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
        session = fx.session(tenant, institution);
        admin = fx.admin(tenant);
    }

    @Test
    void attendeesComeFromSisOnlyAndNothingIsWrittenWhenItCannotAnswer() throws Exception {
        UUID draft = fx.exam(tenant, session, "draft", "100", "40");
        UUID scheduled = fx.exam(tenant, session, "scheduled", "100", "40");

        Reply early = fx.call("POST", EXAMS + draft + ":populateResults", admin, "{}");
        assertThat(early.status()).as("not scheduled yet: checked before sis is asked: " + early.text()).isEqualTo(409);
        String elsewhere = fx.member(tenant, UUID.randomUUID(), "assessment.exam.populate_exam_results");
        assertThat(fx.call("POST", EXAMS + scheduled + ":populateResults", elsewhere, "{}").status())
                .as("another school's exam").isEqualTo(404);
        assertThat(fx.call("POST", EXAMS + scheduled + ":populateResults", fx.admin(UUID.randomUUID()), "{}").status())
                .as("another tenant's exam").isEqualTo(404);

        Reply unavailable = fx.call("POST", EXAMS + scheduled + ":populateResults", admin, "{}");
        assertThat(unavailable.status()).as(unavailable.text()).isEqualTo(503);
        assertThat(unavailable.body().path("code").asString()).isEqualTo("UNAVAILABLE");
        assertThat(attendees(scheduled)).as("nothing registered without the roster").isZero();
    }

    @Test
    void roomsAreCheckedLocallyFirstAndThenOnlyWithFacility() throws Exception {
        UUID draft = fx.exam(tenant, session, "draft", "100", "40");
        UUID scheduled = fx.exam(tenant, session, "scheduled", "100", "40");
        String allocate = EXAMS + scheduled + ":allocateRooms";
        UUID room = UUID.randomUUID();

        expect(fx.call("POST", allocate, admin, "{\"roomIds\":[]}"), 422);
        Reply twice = expect(fx.call("POST", allocate, admin, "{\"roomIds\":[\"" + room + "\",\"" + room + "\"]}"), 422);
        assertThat(twice.body().path("errors").get(0).path("field").asString()).isEqualTo("roomIds[1]");
        expect(fx.call("POST", EXAMS + draft + ":allocateRooms", admin, "{\"roomIds\":[\"" + room + "\"]}"), 409);
        Reply empty = expect(fx.call("POST", allocate, admin, "{\"roomIds\":[\"" + room + "\"]}"), 409);
        assertThat(empty.body().path("detail").asString()).contains("Populate");

        UUID seated = fx.result(tenant, scheduled, UUID.randomUUID(), "expected", null, "100", null, null);
        Reply unavailable = fx.call("POST", allocate, admin, "{\"roomIds\":[\"" + room + "\"]}");
        assertThat(unavailable.status()).as(unavailable.text()).isEqualTo(503);
        assertThat(fx.value(tenant, "SELECT room_id FROM assessment.exam_result WHERE tenant_id = :tenantId AND id = :id",
                UUID.class, "id", seated)).as("no room written").isNull();
    }

    @Test
    void marksFollowTheAttendanceRulesAndAStaleEntryIsRefused() throws Exception {
        UUID exam = fx.exam(tenant, session, "held", "100", "40");
        UUID ann = UUID.randomUUID();
        UUID ben = UUID.randomUUID();
        UUID cara = UUID.randomUUID();
        UUID dev = UUID.randomUUID();
        for (UUID student : List.of(ann, ben, cara, dev)) {
            fx.result(tenant, exam, student, "expected", null, "100", null, null);
        }
        String record = EXAMS + exam + ":recordMarks";

        Reply invalid = expect(fx.call("POST", record, admin, entries(
                entry(ann, "present", null, null), entry(ben, "absent", "10", null),
                entry(cara, "exempted", "5", null), entry(dev, "present", "100.001", null))), 422);
        assertThat(invalid.body().path("errors")).extracting(error -> error.path("field").asString())
                .containsExactly("entries[0].marksObtained", "entries[1].marksObtained", "entries[2].marksObtained",
                        "entries[3].marksObtained");
        Reply unknown = expect(fx.call("POST", record, admin, entries(entry(UUID.randomUUID(), "present", "5", null))),
                422);
        assertThat(unknown.body().path("errors").get(0).path("code").asString()).isEqualTo("NOT_FOUND");

        Reply recorded = expect(fx.call("POST", record, admin, entries(entry(ann, "present", "72.50", null),
                entry(ben, "present", "39.99", null), entry(cara, "absent", null, null),
                entry(dev, "exempted", null, null))), 200);
        Map<UUID, JsonNode> rows = byStudent(recorded.body());
        assertThat(rows.get(ann).path("resultStatus").asString()).isEqualTo("pass");
        assertThat(rows.get(ben).path("resultStatus").asString()).as("below the pass marks").isEqualTo("fail");
        assertThat(rows.get(cara).path("resultStatus").asString()).isEqualTo("absent");
        assertThat(rows.get(dev).path("attendanceStatus").asString()).isEqualTo("exempted");
        assertThat(rows.get(dev).has("resultStatus")).as("an exempted attendee has no result").isFalse();

        String annEtag = rows.get(ann).path("etag").asString();
        String benEtag = rows.get(ben).path("etag").asString();
        expect(fx.call("POST", record, admin, entries(entry(ann, "present", "75", annEtag))), 200);
        Reply stale = expect(fx.call("POST", record, admin, entries(entry(ben, "present", "41", benEtag),
                entry(ann, "present", "80", annEtag))), 412);
        assertThat(stale.body().path("detail").asString()).contains("entries[1]");
        assertThat(marks(exam, ben)).as("all or nothing: the fresh entry is not written either")
                .isEqualByComparingTo("39.99");
        assertThat(marks(exam, ann)).isEqualByComparingTo("75");
        assertThat(fx.value(tenant, "SELECT count(*) FROM assessment.result_change_log WHERE tenant_id = :tenantId",
                Long.class)).as("every change is logged").isEqualTo(5L);

        String outside = fx.member(tenant, UUID.randomUUID(), "assessment.exam.record_exam_marks");
        expect(fx.call("POST", record, outside, entries(entry(ann, "present", "1", null))), 404);
    }

    @Test
    void marksEntryClosesOnlyWhenCompleteAndACorrectionReopensIt() throws Exception {
        UUID exam = fx.exam(tenant, session, "held", "50", "20");
        UUID student = UUID.randomUUID();
        fx.result(tenant, exam, student, "expected", null, "50", null, null);
        String close = EXAMS + exam + ":markResultsUpdated";

        Reply incomplete = expect(fx.call("POST", close, admin, "{}"), 409);
        assertThat(incomplete.body().path("code").asString()).isEqualTo("INVALID_STATE_TRANSITION");
        expect(fx.call("POST", EXAMS + exam + ":recordMarks", admin, entries(entry(student, "present", "30", null))),
                200);
        Reply closed = expect(fx.call("POST", close, admin, "{}"), 200);
        assertThat(closed.body().path("status").asString()).isEqualTo("result_updated");
        assertThat(closed.body().path("resultsEntered").asBoolean()).isTrue();

        expect(fx.call("POST", EXAMS + exam + ":recordMarks", admin, entries(entry(student, "present", "30", null))),
                200);
        assertThat(examStatus(exam)).as("an unchanged entry reopens nothing").isEqualTo("result_updated");
        expect(fx.call("POST", EXAMS + exam + ":recordMarks", admin, entries(entry(student, "present", "31", null))),
                200);
        assertThat(examStatus(exam)).as("a correction reopens marks entry").isEqualTo("held");
    }

    @Test
    void attendanceChangesOnlyWithTheMarksAndAnAggregatedRowIsImmutable() throws Exception {
        UUID exam = fx.exam(tenant, session, "held", "100", "40");
        UUID result = fx.result(tenant, exam, UUID.randomUUID(), "present", "50", "100", "pass", null);
        String etag = expect(fx.call("GET", RESULTS + result, admin, null), 200).body().path("etag").asString();

        Reply attendance = fx.call("PATCH", RESULTS + result + "?updateMask=attendanceStatus", admin,
                "{\"attendanceStatus\":\"absent\"}", etag);
        assertThat(attendance.status()).as(attendance.text()).isEqualTo(400);
        Reply note = expect(fx.call("PATCH", RESULTS + result + "?updateMask=note", admin, "{\"note\":\"Late\"}", etag),
                200);

        fx.inTenant(tenant, () -> database.jdbc().sql("UPDATE assessment.exam_result SET marksheet_line_id = :line "
                + "WHERE tenant_id = :tenantId AND id = :id").param("line", line(exam)).param("id", result).update());
        String linked = expect(fx.call("GET", RESULTS + result, admin, null), 200).body().path("etag").asString();
        Reply frozen = fx.call("PATCH", RESULTS + result + "?updateMask=note", admin, "{\"note\":\"Changed\"}", linked);
        assertThat(frozen.status()).as("on a marksheet: " + frozen.text()).isEqualTo(409);
        assertThat(note.body().path("note").asString()).isEqualTo("Late");
    }

    /** A marksheet line of the exam's session, for a result to be linked to. */
    private UUID line(UUID exam) {
        UUID template = fx.template(tenant, session, null);
        UUID marksheet = UUID.randomUUID();
        UUID line = UUID.randomUUID();
        fx.inTenant(tenant, () -> {
            database.jdbc().sql("INSERT INTO assessment.marksheet (id, tenant_id, exam_session_id, result_template_id, "
                            + "name, generated_on, generated_by_user_id) VALUES (:id, :tenantId, :session, :template, "
                            + "'Sheet', DATE '2026-03-20', :actor)")
                    .param("id", marksheet).param("session", session).param("template", template)
                    .param("actor", UUID.randomUUID()).update();
            database.jdbc().sql("INSERT INTO assessment.marksheet_line (id, tenant_id, marksheet_id, student_id, "
                            + "generated_on) VALUES (:id, :tenantId, :marksheet, :student, DATE '2026-03-20')")
                    .param("id", line).param("marksheet", marksheet).param("student", UUID.randomUUID()).update();
            return null;
        });
        return line;
    }

    private static String entries(String... entries) {
        return "{\"entries\":[" + String.join(",", entries) + "]}";
    }

    private static String entry(UUID student, String attendance, String marks, String etag) {
        return "{\"studentId\":\"" + student + "\",\"attendanceStatus\":\"" + attendance + "\""
                + (marks == null ? "" : ",\"marksObtained\":\"" + marks + "\"")
                + (etag == null ? "" : ",\"etag\":" + json(etag)) + "}";
    }

    private static Map<UUID, JsonNode> byStudent(JsonNode list) {
        Map<UUID, JsonNode> rows = new java.util.HashMap<>();
        for (JsonNode row : list.path("items")) {
            rows.put(UUID.fromString(row.path("studentId").asString()), row);
        }
        return rows;
    }

    private long attendees(UUID exam) {
        return fx.value(tenant, "SELECT count(*) FROM assessment.exam_result WHERE tenant_id = :tenantId "
                + "AND exam_id = :exam", Long.class, "exam", exam);
    }

    private BigDecimal marks(UUID exam, UUID student) {
        return fx.value(tenant, "SELECT marks FROM assessment.exam_result WHERE tenant_id = :tenantId "
                + "AND exam_id = :exam AND student_id = :student", BigDecimal.class, "exam", exam, "student", student);
    }

    private String examStatus(UUID exam) {
        return fx.value(tenant, "SELECT status FROM assessment.exam WHERE tenant_id = :tenantId AND id = :id",
                String.class, "id", exam);
    }
}
