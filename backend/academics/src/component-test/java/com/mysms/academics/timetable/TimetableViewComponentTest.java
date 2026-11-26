package com.mysms.academics.timetable;

import com.mysms.academics.AcademicsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.academics.timetable.config.TimetableDatabaseConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
 * timetable.GetTimetable over HTTP against real PostgreSQL as the application role: whose timetable is answered, in
 * which order and over which days, what is left out, and who may read it. Each test works in a tenant of its own.
 */
@SpringBootTest(classes = AcademicsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TimetableViewComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String VIEW = "/v1/timetable/sessions:timetable";
    private static final String PERMISSION = "timetable.session.get_timetable";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(TimetableDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "timetable", "academics", "attendance", "assessment");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {

        List<String> titles() {
            List<String> titles = new ArrayList<>();
            body.path("data").forEach(item -> titles.add(item.path("session").path("title").asString()));
            return titles;
        }

        /** The cursor of the next link; empty on the last page. */
        String cursor() {
            String next = body.path("paging").path("next").asString("");
            return next.isEmpty() ? "" : next.substring(next.indexOf("cursor=") + "cursor=".length());
        }
    }

    /** One school of one tenant with a teacher, a class and a room, and someone who may read its timetable. */
    private final class School {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final UUID teacher = UUID.randomUUID();
        final UUID classSection = UUID.randomUUID();
        final UUID room = UUID.randomUUID();
        final String reader = token(tenant, List.of(institution.toString()), PERMISSION);
        final String office = token(tenant, List.of(institution.toString()), PERMISSION,
                "timetable.session_student.read");

        /** A session of the school's class with its teacher in its room; answers its id. */
        UUID session(String title, String startAt, String endAt, String status) {
            return session(title, startAt, endAt, status, teacher, classSection, room, institution);
        }

        UUID session(String title, String startAt, String endAt, String status, UUID staff, UUID ofClass, UUID inRoom,
                UUID ofInstitution) {
            UUID id = UUID.randomUUID();
            sql("INSERT INTO timetable.session (id, tenant_id, institution_id, start_at, end_at, class_section_id,"
                    + " staff_id, room_id, title, status) VALUES ('" + id + "', :tenantId, '" + ofInstitution + "', '"
                    + startAt + "', '" + endAt + "', '" + ofClass + "', '" + staff + "', '" + inRoom + "', '" + title
                    + "', '" + status + "')");
            return id;
        }

        void sql(String statement) {
            CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> database.jdbc().sql(statement).update()));
        }
    }

    private Reply get(String query, String token) throws Exception {
        HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + VIEW + query)).GET()
                .header("Authorization", "Bearer " + token).build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    private static String token(UUID tenant, List<String> institutions, String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", institutions.contains("*") ? List.of("*") : List.of());
        claims.put("ins", institutions);
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    @Test
    void aTeachersWeekIsAnsweredInStartOrderWithoutDraftsPageByPage() throws Exception {
        School school = new School();
        school.session("Wed cancelled", "2026-03-04T08:00:00Z", "2026-03-04T08:45:00Z", "cancelled");
        school.session("Mon first", "2026-03-02T08:00:00Z", "2026-03-02T08:45:00Z", "confirmed");
        school.session("Mon second", "2026-03-02T09:00:00Z", "2026-03-02T09:45:00Z", "done");
        school.session("Thu draft", "2026-03-05T08:00:00Z", "2026-03-05T08:45:00Z", "draft");
        school.session("Another teacher", "2026-03-03T08:00:00Z", "2026-03-03T08:45:00Z", "confirmed",
                UUID.randomUUID(), school.classSection, school.room, school.institution);
        String week = "?fromDate=2026-03-02&toDate=2026-03-08&staffId=" + school.teacher;

        Reply all = get(week, school.reader);
        assertThat(all.status()).as(all.text()).isEqualTo(200);
        assertThat(all.titles()).containsExactly("Mon first", "Mon second", "Wed cancelled");
        assertThat(all.body().path("data").get(2).path("session").path("status").asString()).isEqualTo("cancelled");
        assertThat(all.body().path("data").get(0).path("session").path("staffId").asString())
                .isEqualTo(school.teacher.toString());
        assertThat(all.body().path("paging").has("next")).isFalse();

        Reply first = get(week + "&size=2", school.reader);
        assertThat(first.titles()).containsExactly("Mon first", "Mon second");
        String next = first.cursor();
        assertThat(next).isNotEmpty();
        assertThat(get(week + "&page=2&size=2&cursor=" + next, school.reader).titles()).containsExactly("Wed cancelled");
        assertThat(get(week + "&page=2&size=2", school.reader).titles()).as("the same page by offset")
                .containsExactly("Wed cancelled");
        Reply otherView = get("?fromDate=2026-03-02&toDate=2026-03-08&roomId=" + school.room + "&page=2&size=2&cursor="
                + next, school.reader);
        assertThat(otherView.status()).as("a cursor belongs to the view it was issued for").isEqualTo(400);
    }

    @Test
    void sessionsThatStartTogetherAreNotLostOrRepeatedBetweenPages() throws Exception {
        School school = new School();
        for (int i = 0; i < 5; i++) {
            school.session("Parallel " + i, "2026-03-02T08:00:00Z", "2026-03-02T08:45:00Z", "confirmed");
        }
        String view = "?fromDate=2026-03-02&toDate=2026-03-02&size=2&classSectionId=" + school.classSection;
        List<String> seen = new ArrayList<>();
        String cursor = "";
        for (int page = 1; page <= 4; page++) {
            Reply reply = get(view + (cursor.isEmpty() ? "" : "&page=" + page + "&cursor=" + cursor), school.reader);
            assertThat(reply.status()).as(reply.text()).isEqualTo(200);
            seen.addAll(reply.titles());
            cursor = reply.cursor();
            if (cursor.isEmpty()) {
                break;
            }
        }
        assertThat(cursor).as("three pages end the view").isEmpty();
        assertThat(seen).containsExactlyInAnyOrder("Parallel 0", "Parallel 1", "Parallel 2", "Parallel 3",
                "Parallel 4");
        Reply onTheStandardList = get(view, school.reader);
        String issued = onTheStandardList.cursor();
        HttpResponse<String> replayed = HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                        + "/v1/timetable/sessions?page=2&size=2&cursor=" + issued)).GET().header("Authorization", "Bearer "
                        + token(school.tenant, List.of(school.institution.toString()), "timetable.session.read"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(replayed.statusCode()).as("a timetable cursor does not page the standard list").isEqualTo(400);
    }

    @Test
    void theDaysAreWholeUtcDaysAndEachSubjectNarrowsTheView() throws Exception {
        School school = new School();
        school.session("First minute", "2026-03-02T00:00:00Z", "2026-03-02T00:45:00Z", "confirmed");
        school.session("Last minute", "2026-03-03T23:59:00Z", "2026-03-04T00:30:00Z", "confirmed");
        school.session("Day before", "2026-03-01T23:59:59Z", "2026-03-02T00:30:00Z", "confirmed");
        school.session("Day after", "2026-03-04T00:00:00Z", "2026-03-04T00:45:00Z", "confirmed");
        UUID otherClass = UUID.randomUUID();
        UUID otherRoom = UUID.randomUUID();
        UUID elective = school.session("Elective", "2026-03-02T10:00:00Z", "2026-03-02T10:45:00Z", "confirmed",
                UUID.randomUUID(), otherClass, otherRoom, school.institution);
        UUID student = UUID.randomUUID();
        school.sql("INSERT INTO timetable.session_student (tenant_id, session_id, student_id) VALUES (:tenantId, '"
                + elective + "', '" + student + "')");
        String days = "?fromDate=2026-03-02&toDate=2026-03-03";

        assertThat(get(days + "&classSectionId=" + school.classSection, school.reader).titles())
                .containsExactly("First minute", "Last minute");
        assertThat(get(days + "&roomId=" + otherRoom, school.reader).titles()).containsExactly("Elective");
        assertThat(get(days + "&studentId=" + student, school.office).titles())
                .as("a student is found on a session's own list of students").containsExactly("Elective");
        assertThat(get(days + "&studentId=" + student + "&classSectionId=" + school.classSection, school.office)
                .titles()).as("every given subject must match").isEmpty();
        Reply withoutStudentRead = get(days + "&studentId=" + student, school.reader);
        assertThat(withoutStudentRead.status()).as("which sessions list a student is the student's own: "
                + withoutStudentRead.text()).isEqualTo(403);
        school.sql("UPDATE timetable.session_student SET deleted_at = now() WHERE student_id = '" + student + "'");
        assertThat(get(days + "&studentId=" + student, school.office).titles()).as("taken off the list").isEmpty();

        UUID batch = UUID.randomUUID();
        school.sql("UPDATE timetable.session SET batch_id = '" + batch + "' WHERE id = '" + elective + "'");
        assertThat(get(days + "&batchId=" + batch, school.reader).titles()).containsExactly("Elective");
        assertThat(get(days + "&batchId=" + UUID.randomUUID(), school.reader).titles()).isEmpty();
        school.sql("UPDATE timetable.session SET deleted_at = now() WHERE id = '" + elective + "'");
        assertThat(get(days + "&roomId=" + otherRoom, school.reader).titles()).as("a deleted session").isEmpty();
    }

    @Test
    void aViewNamesItsSubjectAndAReasonableStretchOfDays() throws Exception {
        School school = new School();
        String staff = "&staffId=" + school.teacher;

        Reply nobody = get("?fromDate=2026-03-02&toDate=2026-03-08", school.reader);
        assertThat(nobody.status()).as(nobody.text()).isEqualTo(422);
        assertThat(nobody.body().path("errors").get(0).path("field").asString()).isEqualTo("classSectionId");
        Reply backwards = get("?fromDate=2026-03-08&toDate=2026-03-02" + staff, school.reader);
        assertThat(backwards.status()).isEqualTo(422);
        assertThat(backwards.body().path("errors").get(0).path("field").asString()).isEqualTo("toDate");
        assertThat(get("?fromDate=2026-03-01&toDate=2026-05-01" + staff, school.reader).status())
                .as("62 days at most").isEqualTo(200);
        assertThat(get("?fromDate=2026-03-01&toDate=2026-05-02" + staff, school.reader).status()).isEqualTo(422);
        assertThat(get("?toDate=2026-03-02" + staff, school.reader).status()).as("fromDate is required").isEqualTo(400);
        assertThat(get("?fromDate=tomorrow&toDate=2026-03-02" + staff, school.reader).status()).isEqualTo(400);
        assertThat(get("?fromDate=1899-12-31&toDate=1900-01-01" + staff, school.reader).status())
                .as("a year no timetable has").isEqualTo(422);
        assertThat(get("?fromDate=9999-12-31&toDate=%2B10000-01-01" + staff, school.reader).status()).isIn(400, 422);
        assertThat(get("?fromDate=9999-12-31&toDate=9999-12-31" + staff, school.reader).status())
                .as("the last day there is").isEqualTo(200);
        assertThat(get("?fromDate=2026-03-02&toDate=2026-03-08&size=0" + staff, school.reader).status())
                .isEqualTo(400);
    }

    @Test
    void aTimetableIsReadWithItsPermissionInsideTheCallersInstitutionsAndTenant() throws Exception {
        School school = new School();
        school.session("Ours", "2026-03-02T08:00:00Z", "2026-03-02T08:45:00Z", "confirmed");
        UUID elsewhere = UUID.randomUUID();
        school.session("Other school, same teacher", "2026-03-02T11:00:00Z", "2026-03-02T11:45:00Z", "confirmed",
                school.teacher, UUID.randomUUID(), UUID.randomUUID(), elsewhere);
        String week = "?fromDate=2026-03-02&toDate=2026-03-08&staffId=" + school.teacher;

        assertThat(get(week, token(school.tenant, List.of(school.institution.toString()), "timetable.session.read"))
                .status()).as("the permission of the view is required").isEqualTo(403);
        assertThat(get(week, school.reader).titles()).as("only the caller's institutions").containsExactly("Ours");
        assertThat(get(week, token(school.tenant, List.of(elsewhere.toString()), PERMISSION)).titles())
                .containsExactly("Other school, same teacher");
        assertThat(get(week, token(school.tenant, List.of("*"), PERMISSION)).titles())
                .containsExactly("Ours", "Other school, same teacher");
        Reply otherTenant = get(week, token(UUID.randomUUID(), List.of("*"), PERMISSION));
        assertThat(otherTenant.status()).isEqualTo(200);
        assertThat(otherTenant.titles()).as("another tenant sees nothing").isEmpty();
    }
}
