package com.mysms.academics.attendance;

import com.mysms.academics.AcademicsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.academics.attendance.config.AttendanceDatabaseConfig;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
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
 * Taking attendance over HTTP against real PostgreSQL (row-level security on): RecordAttendance,
 * CompleteAttendanceSheet, reopening a completed sheet and SummarizeAttendance. Each test works in a tenant of its own.
 * TakeAttendanceForSession needs timetable, sis and org, which do not run here, so it answers 503.
 */
@SpringBootTest(classes = AcademicsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AttendanceTakingComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SHEETS = "/v1/attendance/attendance-sheets";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(AttendanceDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "attendance", "academics", "timetable", "assessment");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {
    }

    /** One school of one tenant with a class register, a configured mark and an absence reason. */
    private final class School {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final UUID classSection = UUID.randomUUID();
        final String admin = token(tenant, List.of("*"), List.of("*"), "*");
        final String register;
        final String lateType;
        final String reason;

        School() throws Exception {
            register = created(call("POST", "/v1/attendance/attendance-registers", admin, "{\"institutionId\":\""
                    + institution + "\",\"code\":\"7A\",\"name\":\"Class 7A\",\"registerScope\":\"class_section\","
                    + "\"classSectionId\":\"" + classSection + "\"}"));
            lateType = created(call("POST", "/v1/attendance/attendance-types", admin, "{\"institutionId\":\""
                    + institution + "\",\"code\":\"LATE\",\"name\":\"Late\",\"countsLate\":true}"));
            reason = created(call("POST", "/v1/attendance/absence-reasons", admin,
                    "{\"code\":\"SICK\",\"title\":\"Sick\"}"));
        }

        /** A sheet of the class register on the date, already in progress. */
        String sheetInProgress(String date) throws Exception {
            String sheet = draftSheet(date);
            assertThat(call("POST", SHEETS + "/" + sheet + ":start", admin, "{}").status()).isEqualTo(200);
            return sheet;
        }

        String draftSheet(String date) throws Exception {
            return created(call("POST", SHEETS, admin,
                    "{\"registerId\":\"" + register + "\",\"attendanceDate\":\"" + date + "\"}"));
        }

        /**
         * Students expected on the sheet: each gets a record marked present, as TakeAttendanceForSession pre-fills a
         * sheet.
         */
        UUID[] expected(String sheet, String date, int count) {
            UUID[] students = new UUID[count];
            for (int i = 0; i < count; i++) {
                students[i] = UUID.randomUUID();
            }
            expect(sheet, date, students);
            return students;
        }

        void expect(String sheet, String date, UUID... students) {
            CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> {
                        for (UUID student : students) {
                            database.jdbc().sql("INSERT INTO attendance.attendance_record (id, tenant_id, sheet_id, "
                                    + "student_id, status, attendance_date, row_version) VALUES (:id, :tenantId, "
                                    + ":sheet, :student, 'present', CAST(:date AS date), 1)")
                                    .param("id", UUID.randomUUID()).param("sheet", UUID.fromString(sheet))
                                    .param("student", student).param("date", date).update();
                        }
                        return null;
                    }));
        }

        Reply record(String sheet, String marks) throws Exception {
            return call("POST", SHEETS + "/" + sheet + ":recordAttendance", admin, "{\"marks\":[" + marks + "]}");
        }

        Reply sheet(String sheet) throws Exception {
            return call("GET", SHEETS + "/" + sheet, admin, null);
        }

        Reply summary(String query) throws Exception {
            return call("GET", "/v1/attendance/summaries?institutionId=" + institution + "&" + query, admin, null);
        }
    }

    private static String mark(UUID student, String status) {
        return "{\"studentId\":\"" + student + "\",\"status\":\"" + status + "\"}";
    }

    private static String created(Reply reply) {
        assertThat(reply.status()).as(reply.text()).isEqualTo(201);
        return reply.body().path("id").asString();
    }

    private Reply call(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    private static String token(UUID tenant, List<String> institutions, List<String> tenantWide,
            String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", tenantWide);
        claims.put("ins", institutions);
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    private <T> T inTenant(UUID tenant, TestTransactions.Work<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).read(work));
    }

    private int marksStored(UUID tenant, String sheet) {
        return inTenant(tenant, () -> database.jdbc().sql("SELECT count(*) FROM attendance.attendance_record "
                + "WHERE tenant_id = :tenantId AND sheet_id = :sheet AND deleted_at IS NULL")
                .param("sheet", UUID.fromString(sheet)).query(Integer.class).single());
    }

    /** How many marks of the sheet were ever changed after they were first stored. */
    private int marksCorrected(UUID tenant, String sheet) {
        return inTenant(tenant, () -> database.jdbc().sql("SELECT count(*) FROM attendance.attendance_record "
                + "WHERE tenant_id = :tenantId AND sheet_id = :sheet AND row_version > 1")
                .param("sheet", UUID.fromString(sheet)).query(Integer.class).single());
    }

    private List<String> events(UUID tenant, String sheet, String type) {
        return inTenant(tenant, () -> database.jdbc().sql("SELECT payload FROM attendance.outbox_event "
                + "WHERE tenant_id = :tenantId AND aggregate_id = :sheet AND event_type = :type ORDER BY id")
                .param("sheet", UUID.fromString(sheet)).param("type", type).query(String.class).list());
    }

    @Test
    void recordsOneMarkPerStudentAndCorrectsItInPlace() throws Exception {
        School school = new School();
        String sheet = school.sheetInProgress("2026-03-02");
        String etag = school.sheet(sheet).body().path("etag").asString();
        UUID[] students = school.expected(sheet, "2026-03-02", 3);
        UUID present = students[0];
        UUID absent = students[1];
        UUID late = students[2];

        Reply recorded = school.record(sheet, mark(present, "present") + ",{\"studentId\":\"" + absent
                + "\",\"status\":\"absent\",\"absenceReasonId\":\"" + school.reason + "\",\"remark\":\"  fever  \"},"
                + "{\"studentId\":\"" + late + "\",\"status\":\"late\",\"attendanceTypeId\":\"" + school.lateType
                + "\"}");

        assertThat(recorded.status()).as(recorded.text()).isEqualTo(200);
        assertThat(recorded.body().path("items")).hasSize(3);
        JsonNode absentRecord = recorded.body().path("items").get(1);
        assertThat(absentRecord.path("studentId").asString()).isEqualTo(absent.toString());
        assertThat(absentRecord.path("status").asString()).isEqualTo("absent");
        assertThat(absentRecord.path("absenceReasonId").asString()).isEqualTo(school.reason);
        assertThat(absentRecord.path("remark").asString()).isEqualTo("fever");
        assertThat(absentRecord.path("attendanceDate").asString()).isEqualTo("2026-03-02");
        assertThat(absentRecord.path("sheetId").asString()).isEqualTo(sheet);
        assertThat(absentRecord.path("recordedByUserId").asString()).isNotEmpty();
        assertThat(absentRecord.path("etag").asString()).isEqualTo("\"2\"");
        assertThat(recorded.body().path("items").get(0).path("etag").asString())
                .as("a mark that changes nothing is not written").isEqualTo("\"1\"");
        String recordedEtag = school.sheet(sheet).body().path("etag").asString();
        assertThat(recordedEtag).as("recording is a new sheet version").isNotEqualTo(etag);
        assertThat(school.record(sheet, mark(present, "present")).status()).isEqualTo(200);
        assertThat(school.sheet(sheet).body().path("etag").asString())
                .as("a call that changes nothing leaves the sheet's version alone").isEqualTo(recordedEtag);

        // a second device saving over marks it has not seen is told to reload
        Reply stale = call("POST", SHEETS + "/" + sheet + ":recordAttendance", school.admin,
                "{\"marks\":[" + mark(present, "late") + "],\"etag\":" + JSON.writeValueAsString(etag) + "}");
        assertThat(stale.status()).as(stale.text()).isEqualTo(412);
        Reply fresh = call("POST", SHEETS + "/" + sheet + ":recordAttendance", school.admin, "{\"marks\":["
                + mark(present, "present") + "],\"etag\":" + JSON.writeValueAsString(recordedEtag) + "}");
        assertThat(fresh.status()).as(fresh.text()).isEqualTo(200);

        // the correction changes the same record; the reason goes when the student turns out to be present
        Reply corrected = school.record(sheet, mark(absent, "present"));
        assertThat(corrected.status()).as(corrected.text()).isEqualTo(200);
        JsonNode same = corrected.body().path("items").get(0);
        assertThat(same.path("id").asString()).isEqualTo(absentRecord.path("id").asString());
        assertThat(same.path("status").asString()).isEqualTo("present");
        assertThat(same.has("absenceReasonId")).isFalse();
        assertThat(same.has("remark")).isFalse();
        assertThat(same.path("etag").asString()).isEqualTo("\"3\"");
        assertThat(marksStored(school.tenant, sheet)).isEqualTo(3);
        // every stored mark has its own audit record, naming what changed
        List<String> audits = inTenant(school.tenant, () -> database.jdbc().sql("SELECT payload FROM "
                + "attendance.outbox_event WHERE tenant_id = :tenantId AND event_type = 'audit.recorded' "
                + "AND payload LIKE :record ORDER BY id")
                .param("record", "%" + absentRecord.path("id").asString() + "%").query(String.class).list());
        assertThat(audits).hasSize(2);
        assertThat(audits.get(0)).contains("status", "absenceReasonId", "remark");

        // the record list is partitioned by date: it is read with the sheet's day as its range
        Reply listed = call("GET", SHEETS + "/" + sheet + "/records?filter="
                + q("attendanceDate >= \"2026-03-02\" AND attendanceDate < \"2026-03-03\""), school.admin, null);
        assertThat(listed.status()).as(listed.text()).isEqualTo(200);
        assertThat(listed.body().path("data")).hasSize(3);
    }

    @Test
    void refusesEveryMarkWhenOneIsInvalidAndNamesEachProblem() throws Exception {
        School school = new School();
        School other = new School();
        String sheet = school.sheetInProgress("2026-03-03");
        UUID[] known = school.expected(sheet, "2026-03-03", 5);
        UUID student = known[0];
        String presentType = created(call("POST", "/v1/attendance/attendance-types", school.admin,
                "{\"institutionId\":\"" + school.institution + "\",\"code\":\"P\",\"name\":\"Present\","
                        + "\"countsPresent\":true}"));
        String elsewhereType = created(call("POST", "/v1/attendance/attendance-types", school.admin,
                "{\"institutionId\":\"" + UUID.randomUUID() + "\",\"code\":\"L2\",\"name\":\"Late\","
                        + "\"countsLate\":true}"));

        Reply refused = school.record(sheet, mark(UUID.randomUUID(), "present") + ","
                + mark(student, "present") + ","
                + mark(student, "absent") + ","
                + "{\"studentId\":\"" + UUID.randomUUID() + "\",\"status\":\"present\",\"absenceReasonId\":\""
                + school.reason + "\"},"
                + "{\"studentId\":\"" + UUID.randomUUID() + "\",\"status\":\"absent\",\"remark\":\""
                + "x".repeat(256) + "\"}");
        assertThat(refused.status()).as(refused.text()).isEqualTo(422);
        assertThat(fieldErrors(refused)).containsExactlyInAnyOrder("marks[2].studentId DUPLICATE",
                "marks[3].absenceReasonId INVALID", "marks[4].remark Size");

        Reply references = school.record(sheet,
                "{\"studentId\":\"" + known[1] + "\",\"status\":\"late\",\"attendanceTypeId\":\""
                        + presentType + "\"},"
                        + "{\"studentId\":\"" + known[2] + "\",\"status\":\"late\",\"attendanceTypeId\":\""
                        + elsewhereType + "\"},"
                        + "{\"studentId\":\"" + known[3] + "\",\"status\":\"late\",\"attendanceTypeId\":\""
                        + other.lateType + "\"},"
                        + "{\"studentId\":\"" + known[4] + "\",\"status\":\"absent\",\"absenceReasonId\":\""
                        + UUID.randomUUID() + "\"}");
        assertThat(references.status()).as(references.text()).isEqualTo(422);
        assertThat(fieldErrors(references)).containsExactlyInAnyOrder("marks[0].attendanceTypeId INVALID",
                "marks[1].attendanceTypeId NOT_FOUND", "marks[2].attendanceTypeId NOT_FOUND",
                "marks[3].absenceReasonId NOT_FOUND");

        assertThat(school.record(sheet, "").status()).as("no marks").isEqualTo(422);
        assertThat(call("POST", SHEETS + "/" + sheet + ":recordAttendance", school.admin, "{}").status())
                .as("marks is required").isEqualTo(422);
        assertThat(school.record(sheet, "{\"studentId\":\"" + UUID.randomUUID() + "\",\"status\":\"asleep\"}")
                .status()).as("an unknown status").isEqualTo(400);
        assertThat(call("POST", SHEETS + "/" + sheet + ":recordAttendance", school.admin, "{\"marks\":[null]}")
                .status()).as("a mark that is null").isIn(400, 422);
        assertThat(marksCorrected(school.tenant, sheet)).as("a refused call stores nothing").isZero();

        // a student the sheet does not know is a reference into sis: without sis nothing is taken on trust
        Reply unknown = school.record(sheet, mark(known[0], "absent") + "," + mark(UUID.randomUUID(), "absent"));
        assertThat(unknown.status()).as(unknown.text()).isEqualTo(503);
        assertThat(unknown.body().path("code").asString()).isEqualTo("UNAVAILABLE");
        assertThat(marksStored(school.tenant, sheet)).isEqualTo(5);
        assertThat(marksCorrected(school.tenant, sheet)).isZero();

        String draft = school.draftSheet("2026-03-04");
        Reply notStarted = school.record(draft, mark(UUID.randomUUID(), "present"));
        // the state is judged before sis is asked about the student
        assertThat(notStarted.status()).as(notStarted.text()).isEqualTo(409);
        assertThat(notStarted.body().path("code").asString()).isEqualTo("INVALID_STATE_TRANSITION");
    }

    private static List<String> fieldErrors(Reply reply) {
        List<String> errors = new ArrayList<>();
        reply.body().path("errors").forEach(error ->
                errors.add(error.path("field").asString() + " " + error.path("code").asString()));
        return errors;
    }

    @Test
    void completesASheetWithMarksAnnouncesItsAbsenteesAndLocksIt() throws Exception {
        School school = new School();
        String sheet = school.sheetInProgress("2026-03-05");
        String complete = SHEETS + "/" + sheet + ":complete";

        Reply empty = call("POST", complete, school.admin, "{}");
        assertThat(empty.status()).as("a sheet without marks: " + empty.text()).isEqualTo(409);
        String started = school.sheet(sheet).body().path("etag").asString();

        UUID[] students = school.expected(sheet, "2026-03-05", 2);
        UUID absent = students[1];
        assertThat(school.record(sheet, mark(students[0], "present") + "," + mark(absent, "absent")).status())
                .isEqualTo(200);
        String etag = school.sheet(sheet).body().path("etag").asString();

        Reply stale = call("POST", complete, school.admin, "{\"etag\":" + JSON.writeValueAsString(started) + "}");
        assertThat(stale.status()).as("an etag from before the marks: " + stale.text()).isEqualTo(412);

        Reply done = call("POST", complete, school.admin, "{\"etag\":" + JSON.writeValueAsString(etag) + "}");
        assertThat(done.status()).as(done.text()).isEqualTo(200);
        assertThat(done.body().path("status").asString()).isEqualTo("done");
        List<String> announced = events(school.tenant, sheet, "attendance.attendance_sheet.complete");
        assertThat(announced).hasSize(1);
        JsonNode payload = JSON.readTree(announced.get(0));
        assertThat(payload.path("id").asString()).isEqualTo(sheet);
        assertThat(payload.path("attendanceDate").asString()).isEqualTo("2026-03-05");
        assertThat(payload.path("absentStudentIds")).hasSize(1);
        assertThat(payload.path("absentStudentIds").get(0).asString()).isEqualTo(absent.toString());

        Reply again = call("POST", complete, school.admin, "{}");
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.body().path("code").asString()).isEqualTo("INVALID_STATE_TRANSITION");
        assertThat(school.record(sheet, mark(absent, "present")).status()).as("a completed sheet is locked")
                .isEqualTo(409);
    }

    @Test
    void recordsAndCompletesInOneCallAndReopensACompletedSheetForCorrection() throws Exception {
        School school = new School();
        String sheet = school.sheetInProgress("2026-03-06");
        UUID student = school.expected(sheet, "2026-03-06", 1)[0];

        Reply both = call("POST", SHEETS + "/" + sheet + ":recordAttendance", school.admin,
                "{\"marks\":[" + mark(student, "absent") + "],\"complete\":true}");
        assertThat(both.status()).as(both.text()).isEqualTo(200);
        assertThat(school.sheet(sheet).body().path("status").asString()).isEqualTo("done");

        // reopening keeps the marks: reset to draft, start, correct, complete again
        assertThat(call("POST", SHEETS + "/" + sheet + ":resetToDraft", school.admin, "{}").status()).isEqualTo(200);
        assertThat(call("POST", SHEETS + "/" + sheet + ":start", school.admin, "{}").status()).isEqualTo(200);
        assertThat(marksStored(school.tenant, sheet)).isEqualTo(1);
        assertThat(school.record(sheet, mark(student, "late")).status()).isEqualTo(200);
        assertThat(call("POST", SHEETS + "/" + sheet + ":complete", school.admin, "{}").status()).isEqualTo(200);
        List<String> announced = events(school.tenant, sheet, "attendance.attendance_sheet.complete");
        assertThat(announced).hasSize(2);
        assertThat(JSON.readTree(announced.get(1)).path("absentStudentIds")).isEmpty();
    }

    @Test
    void aSheetOutsideTheCallersInstitutionsOrTenantDoesNotExistAndThePermissionIsRequired() throws Exception {
        School school = new School();
        String sheet = school.sheetInProgress("2026-03-09");
        UUID student = school.expected(sheet, "2026-03-09", 1)[0];
        String marks = "{\"marks\":[" + mark(student, "absent") + "]}";
        String record = SHEETS + "/" + sheet + ":recordAttendance";
        String complete = SHEETS + "/" + sheet + ":complete";

        String noPermission = token(school.tenant, List.of("*"), List.of("*"), "attendance.attendance_sheet.read");
        assertThat(call("POST", record, noPermission, marks).status()).isEqualTo(403);
        assertThat(call("POST", complete, noPermission, "{}").status()).isEqualTo(403);
        assertThat(call("GET", "/v1/attendance/summaries?institutionId=" + school.institution
                + "&fromDate=2026-03-01&toDate=2026-03-31&groupBy=student", noPermission, null).status())
                .isEqualTo(403);
        assertThat(call("POST", record, null, marks).status()).isEqualTo(401);

        String elsewhere = token(school.tenant, List.of(UUID.randomUUID().toString()), List.of(), "*");
        assertThat(call("POST", record, elsewhere, marks).status()).as("another institution").isEqualTo(404);
        assertThat(call("POST", complete, elsewhere, "{}").status()).isEqualTo(404);

        String otherTenant = token(UUID.randomUUID(), List.of("*"), List.of("*"), "*");
        assertThat(call("POST", record, otherTenant, marks).status()).as("another tenant").isEqualTo(404);
        assertThat(call("POST", complete, otherTenant, "{}").status()).isEqualTo(404);

        String recordOnly = token(school.tenant, List.of(school.institution.toString()), List.of(),
                "attendance.attendance_sheet.record_attendance");
        Reply sneaked = call("POST", record, recordOnly,
                "{\"marks\":[" + mark(student, "absent") + "],\"complete\":true}");
        assertThat(sneaked.status()).as("completing through recordAttendance needs the completing permission")
                .isEqualTo(403);
        assertThat(marksCorrected(school.tenant, sheet)).as("a refused call stores nothing").isZero();
        assertThat(school.sheet(sheet).body().path("status").asString()).isEqualTo("in_progress");

        String here = token(school.tenant, List.of(school.institution.toString()), List.of(),
                "attendance.attendance_sheet.record_attendance", "attendance.attendance_sheet.complete_attendance_sheet");
        assertThat(call("POST", record, here, marks).status()).as("a teacher of this institution").isEqualTo(200);
        assertThat(call("POST", complete, here, "{}").status()).isEqualTo(200);
        assertThat(marksCorrected(school.tenant, sheet)).isEqualTo(1);
    }

    @Test
    void summarizesCompletedSheetsPerStudentPerDayAndPerClass() throws Exception {
        School school = new School();
        UUID alwaysThere = UUID.randomUUID();
        UUID sometimes = UUID.randomUUID();
        String monday = school.sheetInProgress("2026-03-09");
        school.expect(monday, "2026-03-09", alwaysThere, sometimes);
        school.record(monday, mark(alwaysThere, "present") + "," + mark(sometimes, "absent"));
        call("POST", SHEETS + "/" + monday + ":complete", school.admin, "{}");
        String tuesday = school.sheetInProgress("2026-03-10");
        school.expect(tuesday, "2026-03-10", alwaysThere, sometimes);
        school.record(tuesday, mark(alwaysThere, "late") + "," + mark(sometimes, "excused"));
        call("POST", SHEETS + "/" + tuesday + ":complete", school.admin, "{}");
        String wednesday = school.sheetInProgress("2026-03-11");
        school.expect(wednesday, "2026-03-11", alwaysThere, sometimes);
        school.record(wednesday, mark(alwaysThere, "present") + "," + mark(sometimes, "present"));
        call("POST", SHEETS + "/" + wednesday + ":complete", school.admin, "{}");
        // still being taken: its marks do not count yet
        String thursday = school.sheetInProgress("2026-03-12");
        school.expect(thursday, "2026-03-12", alwaysThere);
        school.record(thursday, mark(alwaysThere, "absent"));
        String range = "fromDate=2026-03-01&toDate=2026-03-31";

        Reply perStudent = school.summary(range + "&groupBy=student");
        assertThat(perStudent.status()).as(perStudent.text()).isEqualTo(200);
        assertThat(perStudent.body().path("data")).hasSize(2);
        JsonNode there = find(perStudent, "studentId", alwaysThere.toString());
        assertThat(counts(there)).isEqualTo("2/0/1/0");
        assertThat(there.path("attendancePercent").asString()).isEqualTo("100.00");
        JsonNode other = find(perStudent, "studentId", sometimes.toString());
        assertThat(counts(other)).isEqualTo("1/1/0/1");
        assertThat(other.path("attendancePercent").asString()).isEqualTo("33.33");

        Reply oneStudent = school.summary(range + "&groupBy=date&studentId=" + sometimes);
        assertThat(oneStudent.body().path("data")).hasSize(3);
        assertThat(oneStudent.body().path("data").get(0).path("date").asString()).isEqualTo("2026-03-09");
        assertThat(counts(oneStudent.body().path("data").get(0))).isEqualTo("0/1/0/0");
        assertThat(oneStudent.body().path("data").get(0).path("attendancePercent").asString()).isEqualTo("0.00");

        Reply perDay = school.summary("fromDate=2026-03-10&toDate=2026-03-11&groupBy=date");
        assertThat(perDay.body().path("data")).hasSize(2);
        assertThat(counts(perDay.body().path("data").get(0))).isEqualTo("0/0/1/1");
        assertThat(perDay.body().path("data").get(0).path("attendancePercent").asString()).isEqualTo("50.00");

        Reply perClass = school.summary(range + "&groupBy=class_section");
        assertThat(perClass.body().path("data")).hasSize(1);
        JsonNode classRow = perClass.body().path("data").get(0);
        assertThat(classRow.path("classSectionId").asString()).isEqualTo(school.classSection.toString());
        assertThat(counts(classRow)).isEqualTo("3/1/1/1");
        assertThat(classRow.path("attendancePercent").asString()).isEqualTo("66.67");
        assertThat(school.summary(range + "&groupBy=student&classSectionId=" + UUID.randomUUID()).body()
                .path("data")).isEmpty();

        // paged in key order, one group at a time; a cursor continues only the summary it was issued for
        Reply first = school.summary(range + "&groupBy=date&size=1");
        assertThat(first.body().path("data")).hasSize(1);
        String next = first.body().path("paging").path("next").asString();
        assertThat(next).contains("groupBy=date").contains("page=2").contains("cursor=");
        Reply second = call("GET", next, school.admin, null);
        assertThat(second.body().path("data").get(0).path("date").asString()).isEqualTo("2026-03-10");
        Reply third = call("GET", second.body().path("paging").path("next").asString(), school.admin, null);
        assertThat(third.body().path("data").get(0).path("date").asString()).isEqualTo("2026-03-11");
        assertThat(third.body().path("paging").has("next")).isFalse();
        assertThat(third.body().path("paging").path("previous").asString()).contains("page=2")
                .doesNotContain("cursor=");
        Reply byOffset = call("GET", third.body().path("paging").path("previous").asString(), school.admin, null);
        assertThat(byOffset.body().path("data").get(0).path("date").asString()).isEqualTo("2026-03-10");
        String cursor = next.substring(next.indexOf("cursor=") + "cursor=".length());
        assertThat(school.summary(range + "&groupBy=student&page=2&size=1&cursor=" + cursor).status())
                .as("a cursor of another grouping").isEqualTo(400);
        Reply studentPage = school.summary(range + "&groupBy=student&size=1");
        Reply studentNext = call("GET", studentPage.body().path("paging").path("next").asString(), school.admin, null);
        assertThat(studentNext.body().path("data")).hasSize(1);
        assertThat(studentNext.body().path("data").get(0).path("studentId").asString())
                .isNotEqualTo(studentPage.body().path("data").get(0).path("studentId").asString());
        assertThat(studentNext.body().path("paging").has("next")).isFalse();

        assertThat(school.summary("fromDate=2026-03-10&toDate=2026-03-09&groupBy=date").status()).isEqualTo(400);
        assertThat(school.summary("fromDate=2025-01-01&toDate=2026-03-09&groupBy=date").status()).isEqualTo(400);
        assertThat(school.summary(range + "&groupBy=weekday").status()).isEqualTo(400);
        assertThat(school.summary("fromDate=2026-03-01&groupBy=date").status()).as("toDate is required")
                .isEqualTo(400);

        String elsewhere = token(school.tenant, List.of(UUID.randomUUID().toString()), List.of(), "*");
        Reply outOfScope = call("GET", "/v1/attendance/summaries?institutionId=" + school.institution + "&" + range
                + "&groupBy=student", elsewhere, null);
        assertThat(outOfScope.status()).isEqualTo(200);
        assertThat(outOfScope.body().path("data")).as("an institution outside the caller's scope").isEmpty();
        Reply otherTenant = call("GET", "/v1/attendance/summaries?institutionId=" + school.institution + "&" + range
                + "&groupBy=student", token(UUID.randomUUID(), List.of("*"), List.of("*"), "*"), null);
        assertThat(otherTenant.body().path("data")).as("another tenant").isEmpty();
    }

    private static JsonNode find(Reply reply, String field, String value) {
        for (JsonNode item : reply.body().path("data")) {
            if (value.equals(item.path(field).asString())) {
                return item;
            }
        }
        throw new AssertionError("no item with " + field + " " + value + " in " + reply.text());
    }

    private static String counts(JsonNode summary) {
        return summary.path("present").asInt() + "/" + summary.path("absent").asInt() + "/"
                + summary.path("late").asInt() + "/" + summary.path("excused").asInt();
    }

    private static String q(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Test
    void attendanceForASessionIsUnavailableWhileTheOtherServicesAreNotConfigured() throws Exception {
        School school = new School();
        Reply unavailable = call("POST", SHEETS + ":forSession", school.admin,
                "{\"timetableSessionId\":\"" + UUID.randomUUID() + "\"}");
        assertThat(unavailable.status()).as(unavailable.text()).isEqualTo(503);
        assertThat(unavailable.body().path("code").asString()).isEqualTo("UNAVAILABLE");
        assertThat(call("POST", SHEETS + ":forSession", school.admin, "{}").status()).as("the session is required")
                .isEqualTo(422);
    }
}
