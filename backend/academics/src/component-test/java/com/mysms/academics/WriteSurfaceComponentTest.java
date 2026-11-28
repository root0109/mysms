package com.mysms.academics;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.academics.assessment.config.AssessmentDatabaseConfig;
import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.events.AcademicsReferenceCleanup;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.InboxProcessor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * What a client may write, over HTTP against real PostgreSQL as the application role: fields the server owns are not
 * part of any update body and are ignored on create (sending one changes nothing), and a teacher named by a teaching
 * assignment or a teacher subject must be a staff record of the caller's own tenant. Each test works in tenants of its
 * own.
 */
@SpringBootTest(classes = AcademicsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WriteSurfaceComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String FIRST_VERSION = "\"1\"";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(AcademicsDatabaseConfig.DATABASE)
    private ModuleDatabase academics;

    @Autowired
    @Qualifier(AssessmentDatabaseConfig.DATABASE)
    private ModuleDatabase assessment;

    @Autowired
    private AcademicsReferenceCleanup cleanup;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "academics", "timetable", "attendance", "assessment");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {

        List<String> invalidFields() {
            List<String> fields = new ArrayList<>();
            body.path("errors").forEach(error -> fields.add(error.path("field").asString()));
            return fields;
        }
    }

    /** One school of one tenant: a class subject, a subject, and a staff record hr announced. */
    private final class School {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final UUID subject = UUID.randomUUID();
        final UUID classSubject = UUID.randomUUID();
        final UUID staff = UUID.randomUUID();
        final UUID adminId = UUID.randomUUID();
        final String admin = token(tenant, adminId);

        School() {
            UUID grade = UUID.randomUUID();
            UUID section = UUID.randomUUID();
            UUID classSection = UUID.randomUUID();
            String start = "(:tenantId, '" + institution + "', ";
            sql(academics, "INSERT INTO academics.grade_level (tenant_id, institution_id, name, slug, id) VALUES "
                            + start + "'Grade 7', 'grade-7', '" + grade + "')",
                    "INSERT INTO academics.section (tenant_id, institution_id, name, code, id) VALUES " + start
                            + "'A', 'A', '" + section + "')",
                    "INSERT INTO academics.class_section (tenant_id, institution_id, academic_year_id, grade_level_id,"
                            + " section_id, id) VALUES " + start + "'" + UUID.randomUUID() + "', '" + grade + "', '"
                            + section + "', '" + classSection + "')",
                    "INSERT INTO academics.subject (tenant_id, institution_id, name, id) VALUES " + start
                            + "'Science', '" + subject + "')",
                    "INSERT INTO academics.class_subject (tenant_id, class_section_id, subject_id, id) VALUES "
                            + "(:tenantId, '" + classSection + "', '" + subject + "', '" + classSubject + "')");
            announce(staff);
        }

        /** A staff record of this tenant, as hr's staff events leave it in academics. */
        void announce(UUID staffId) {
            sql(academics, "INSERT INTO academics.staff_record (tenant_id, staff_id, person_id, source_version, "
                    + "synced_at) VALUES (:tenantId, '" + staffId + "', '" + UUID.randomUUID() + "', 1, now())");
        }

        void sql(ModuleDatabase database, String... statements) {
            CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> {
                        for (String statement : statements) {
                            database.jdbc().sql(statement).update();
                        }
                        return null;
                    }));
        }

        boolean holds(ModuleDatabase database, String condition) {
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).read(() -> database.jdbc().sql(condition)
                            .query(Boolean.class).single()));
        }
    }

    // ---- staff references

    @Test
    void aTeacherSubjectNamesAStaffRecordOfTheCallersTenant() throws Exception {
        School school = new School();
        School other = new School();
        String path = "/v1/academics/teacher-subjects";

        Reply unknown = call("POST", path, school.admin, teacherSubject(UUID.randomUUID(), school.subject), null);
        assertThat(unknown.status()).as(unknown.text()).isEqualTo(422);
        assertThat(unknown.invalidFields()).containsExactly("staffId");
        Reply foreign = call("POST", path, school.admin, teacherSubject(other.staff, school.subject), null);
        assertThat(foreign.status()).as("another tenant's staff is no staff here").isEqualTo(422);
        assertThat(foreign.invalidFields()).containsExactly("staffId");

        Reply created = call("POST", path, school.admin, teacherSubject(school.staff, school.subject), null);
        assertThat(created.status()).as(created.text()).isEqualTo(201);
        assertThat(created.body().path("staffId").asString()).isEqualTo(school.staff.toString());
    }

    @Test
    void aTeachingAssignmentNamesAStaffRecordOfTheCallersTenantOnCreateAndUpdate() throws Exception {
        School school = new School();
        School other = new School();
        String path = "/v1/academics/teaching-assignments";

        Reply unknown = call("POST", path, school.admin, teaching(UUID.randomUUID(), school.classSubject), null);
        assertThat(unknown.status()).as(unknown.text()).isEqualTo(422);
        assertThat(unknown.invalidFields()).containsExactly("staffId");
        Reply foreign = call("POST", path, school.admin, teaching(other.staff, school.classSubject), null);
        assertThat(foreign.status()).as("another tenant's staff is no staff here").isEqualTo(422);
        assertThat(foreign.invalidFields()).containsExactly("staffId");

        Reply created = call("POST", path, school.admin, teaching(school.staff, school.classSubject), null);
        assertThat(created.status()).as(created.text()).isEqualTo(201);
        String item = path + "/" + created.body().path("id").asString();
        String etag = created.body().path("etag").asString();

        Reply moved = call("PATCH", item, school.admin, "{\"staffId\":\"" + other.staff + "\"}", etag);
        assertThat(moved.status()).as(moved.text()).isEqualTo(422);
        assertThat(moved.invalidFields()).containsExactly("staffId");
        UUID colleague = UUID.randomUUID();
        school.announce(colleague);
        Reply reassigned = call("PATCH", item, school.admin, "{\"staffId\":\"" + colleague + "\"}", etag);
        assertThat(reassigned.status()).as(reassigned.text()).isEqualTo(200);
        assertThat(reassigned.body().path("staffId").asString()).isEqualTo(colleague.toString());
    }

    @Test
    void aStaffRecordHrDeletedNoLongerTeaches() throws Exception {
        School school = new School();
        UUID person = UUID.randomUUID();
        UUID leaver = UUID.randomUUID();
        school.sql(academics, "INSERT INTO academics.staff_record (tenant_id, staff_id, person_id, source_version, "
                + "synced_at) VALUES (:tenantId, '" + leaver + "', '" + person + "', 1, now())");
        String path = "/v1/academics/teaching-assignments";
        Reply before = call("POST", path, school.admin, teaching(leaver, school.classSubject), null);
        assertThat(before.status()).as(before.text()).isEqualTo(201);

        EventEnvelope deleted = new EventEnvelope(UUID.randomUUID(), "hr", "hr.staff.deleted",
                Optional.of(school.tenant), "hr.staff", leaver, Instant.now(), "{\"id\":\"" + leaver
                        + "\",\"personId\":\"" + person + "\",\"rowVersion\":2}", Optional.of(TRACE));
        InboxProcessor inbox = new InboxProcessor(academics, Clock.systemUTC());
        assertThat(inbox.deliver(deleted, cleanup.handlers().get("hr.staff.deleted")))
                .isEqualTo(InboxProcessor.Outcome.PROCESSED);

        Reply after = call("POST", path, school.admin, teaching(leaver, school.classSubject), null);
        assertThat(after.status()).as(after.text()).isEqualTo(422);
        assertThat(after.invalidFields()).containsExactly("staffId");
        assertThat(school.holds(academics, "SELECT is_deleted AND person_id = '" + person + "' FROM "
                + "academics.staff_record WHERE tenant_id = :tenantId AND staff_id = '" + leaver + "'"))
                .as("the entry stays, flagged, for privacy's tasks").isTrue();
    }

    // ---- fields the server owns

    @Test
    void anUpdateCannotWriteWhatResultsGenerationOwns() throws Exception {
        School school = new School();
        UUID examType = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        UUID template = UUID.randomUUID();
        UUID exam = UUID.randomUUID();
        UUID marksheet = UUID.randomUUID();
        UUID generatedBy = UUID.randomUUID();
        school.sql(assessment, "INSERT INTO assessment.exam_type (id, tenant_id, code, name) VALUES ('" + examType
                        + "', :tenantId, 'TERM', 'Term')",
                "INSERT INTO assessment.exam_session (id, tenant_id, institution_id, exam_type_id, code, name, "
                        + "grade_level_id, start_date, end_date) VALUES ('" + session + "', :tenantId, '"
                        + school.institution + "', '" + examType + "', 'T1', 'Term one', '" + UUID.randomUUID()
                        + "', '2026-01-10', '2026-01-20')",
                "INSERT INTO assessment.result_template (id, tenant_id, exam_session_id, name, result_date, "
                        + "row_version) VALUES ('" + template + "', :tenantId, '" + session + "', 'Term one', "
                        + "'2026-01-25', 1)",
                "INSERT INTO assessment.exam (id, tenant_id, exam_session_id, subject_id, code, name, start_at, "
                        + "end_at, total_marks, pass_marks, row_version) VALUES ('" + exam + "', :tenantId, '"
                        + session + "', '" + school.subject + "', 'SCI', 'Science', '2026-01-12T09:00:00Z', "
                        + "'2026-01-12T10:00:00Z', 100, 40, 1)",
                "INSERT INTO assessment.marksheet (id, tenant_id, exam_session_id, result_template_id, name, "
                        + "generated_on, generated_by_user_id, total_passed, total_failed, row_version) VALUES ('"
                        + marksheet + "', :tenantId, '" + session + "', '" + template + "', 'Term one', "
                        + "'2026-01-25', '" + generatedBy + "', 3, 1, 1)");

        JsonNode templateRow = unchanged(school, "/v1/assessment/result-templates/" + template,
                "{\"status\":\"result_generated\"}");
        assertThat(templateRow.path("status").asString()).isEqualTo("draft");
        JsonNode examRow = unchanged(school, "/v1/assessment/exams/" + exam, "{\"resultsEntered\":true}");
        assertThat(examRow.path("resultsEntered").asBoolean()).isFalse();
        JsonNode sheet = unchanged(school, "/v1/assessment/marksheets/" + marksheet, "{\"generatedByUserId\":\""
                + UUID.randomUUID() + "\",\"totalPassed\":40,\"totalFailed\":0}");
        assertThat(sheet.path("generatedByUserId").asString()).isEqualTo(generatedBy.toString());
        assertThat(sheet.path("totalPassed").asInt()).isEqualTo(3);
        assertThat(sheet.path("totalFailed").asInt()).isEqualTo(1);
    }

    @Test
    void aCreateCannotWriteWhatResultsGenerationOwns() throws Exception {
        School school = new School();
        UUID examType = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        school.sql(assessment, "INSERT INTO assessment.exam_type (id, tenant_id, code, name) VALUES ('" + examType
                        + "', :tenantId, 'TERM', 'Term')",
                "INSERT INTO assessment.exam_session (id, tenant_id, institution_id, exam_type_id, code, name, "
                        + "grade_level_id, start_date, end_date) VALUES ('" + session + "', :tenantId, '"
                        + school.institution + "', '" + examType + "', 'T1', 'Term one', '" + UUID.randomUUID()
                        + "', '2026-01-10', '2026-01-20')");

        Reply template = call("POST", "/v1/assessment/result-templates", school.admin, "{\"examSessionId\":\""
                + session + "\",\"name\":\"Term one\",\"resultDate\":\"2026-01-25\",\"status\":\"result_generated\"}",
                null);
        assertThat(template.status()).as(template.text()).isEqualTo(201);
        assertThat(template.body().path("status").asString()).as("starts as a draft").isEqualTo("draft");

        Reply exam = call("POST", "/v1/assessment/exams", school.admin, "{\"examSessionId\":\"" + session
                + "\",\"subjectId\":\"" + school.subject + "\",\"code\":\"SCI\",\"name\":\"Science\",\"startAt\":"
                + "\"2026-01-12T09:00:00Z\",\"endAt\":\"2026-01-12T10:00:00Z\",\"totalMarks\":\"100\","
                + "\"passMarks\":\"40\",\"resultsEntered\":true}", null);
        assertThat(exam.status()).as(exam.text()).isEqualTo(201);
        assertThat(exam.body().path("resultsEntered").asBoolean()).as("no marks are entered yet").isFalse();

        Reply sheet = call("POST", "/v1/assessment/marksheets", school.admin, "{\"examSessionId\":\"" + session
                + "\",\"resultTemplateId\":\"" + template.body().path("id").asString() + "\",\"name\":\"Term one\","
                + "\"generatedOn\":\"2026-01-25\",\"generatedByUserId\":\"" + UUID.randomUUID() + "\","
                + "\"totalPassed\":40,\"totalFailed\":0}", null);
        assertThat(sheet.status()).as(sheet.text()).isEqualTo(201);
        assertThat(sheet.body().path("generatedByUserId").asString()).as("made in the caller's name")
                .isEqualTo(school.adminId.toString());
        assertThat(sheet.body().has("totalPassed")).as("filled by results generation only").isFalse();
        assertThat(sheet.body().has("totalFailed")).isFalse();
    }

    @Test
    void anUpdateCannotWriteTheRemainingPeriodsOfATeachingAssignment() throws Exception {
        School school = new School();
        UUID teaching = UUID.randomUUID();
        school.sql(academics, "INSERT INTO academics.teaching_assignment (tenant_id, class_subject_id, staff_id, "
                + "periods_per_week, periods_remaining, row_version, id) VALUES (:tenantId, '" + school.classSubject
                + "', '" + school.staff + "', 6, 4, 1, '" + teaching + "')");

        JsonNode row = unchanged(school, "/v1/academics/teaching-assignments/" + teaching,
                "{\"periodsRemaining\":0}");
        assertThat(row.path("periodsRemaining").asInt()).isEqualTo(4);
    }

    /**
     * PATCHes a body that carries only fields the update body does not have: like an empty body, the row comes back
     * as it was, at the version it had.
     */
    private JsonNode unchanged(School school, String item, String body) throws Exception {
        Reply patched = call("PATCH", item, school.admin, body, FIRST_VERSION);
        assertThat(patched.status()).as(item + ": " + patched.text()).isEqualTo(200);
        assertThat(patched.body().path("etag").asString()).as(item + " keeps its version").isEqualTo(FIRST_VERSION);
        Reply read = call("GET", item, school.admin, null, null);
        assertThat(read.body().path("etag").asString()).isEqualTo(FIRST_VERSION);
        return read.body();
    }

    private static String teacherSubject(UUID staff, UUID subject) {
        return "{\"staffId\":\"" + staff + "\",\"subjectId\":\"" + subject + "\"}";
    }

    private static String teaching(UUID staff, UUID classSubject) {
        return "{\"staffId\":\"" + staff + "\",\"classSubjectId\":\"" + classSubject + "\"}";
    }

    private Reply call(String method, String path, String token, String body, String ifMatch) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", "Bearer " + token);
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (ifMatch != null) {
            request.header("If-Match", ifMatch);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    /** An administrator of every institution of the tenant. */
    private static String token(UUID tenant, UUID user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of("*"));
        claims.put("tperms", List.of("*"));
        claims.put("ins", List.of("*"));
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }
}
