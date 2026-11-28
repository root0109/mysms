package com.mysms.hrms.hr;

import com.mysms.hrms.HrmsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.hrms.hr.config.HrDatabaseConfig;
import com.mysms.hrms.hr.events.HolidayRecordSync;
import com.mysms.messaging.events.EventEnvelope;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
 * The leave ledger over HTTP against real PostgreSQL as the application role: approving leave counts its days
 * against the staff member's balance, and a staff member's position shows entitled, taken, pending and available
 * days. Each test works in a tenant of its own.
 */
@SpringBootTest(classes = HrmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LeaveLedgerComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String APPLICATIONS = "/v1/hr/leave-applications";
    private static final String APPROVE = "hr.leave_application.approve_leave";
    private static final String BALANCE = "hr.staff.get_leave_balance";
    private static final String CANCEL = "hr.leave_application.cancel_leave";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(HrDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private HolidayRecordSync holidays;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "hr", "payroll", "time_attendance");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {
    }

    /** One school of one tenant: a teacher with ten days of casual leave a year, and a head who decides. */
    private final class School {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final UUID year = UUID.randomUUID();
        final UUID teacherUser = UUID.randomUUID();
        final UUID teacherStaff = UUID.randomUUID();
        final UUID casual = UUID.randomUUID();
        final String teacher = token(teacherUser, tenant, List.of(institution.toString()),
                "hr.leave_application.read", "hr.leave_application.create", "hr.leave_application.update", BALANCE,
                APPROVE, CANCEL);
        final UUID headUser = UUID.randomUUID();
        final String head = token(headUser, tenant, List.of(institution.toString()), "hr.leave_application.read",
                "hr.leave_application.update", "hr.leave_application.delete", APPROVE, BALANCE,
                "hr.leave_application.reject_leave", "hr.leave_balance.read");

        School() {
            sql("INSERT INTO hr.staff (id, tenant_id, institution_id, person_id, user_id, employee_code) VALUES ('"
                            + teacherStaff + "', :tenantId, '" + institution + "', '" + UUID.randomUUID() + "', '"
                            + teacherUser + "', 'T-1')",
                    "INSERT INTO hr.leave_type (id, tenant_id, institution_id, name, code, max_days_per_year) VALUES ('"
                            + casual + "', :tenantId, '" + institution + "', 'Casual leave', 'CL', 10)");
        }

        void sql(String... statements) {
            CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> {
                        for (String statement : statements) {
                            database.jdbc().sql(statement).update();
                        }
                        return null;
                    }));
        }

        /** The teacher applies for casual leave; answers the application id. */
        String applies(String fromAt, String toAt, String dayPart) throws Exception {
            return applies(fromAt, toAt, dayPart, casual);
        }

        String applies(String fromAt, String toAt, String dayPart, UUID leaveType) throws Exception {
            Reply created = call("POST", APPLICATIONS, teacher, "{\"institutionId\":\"" + institution
                    + "\",\"academicYearId\":\"" + year + "\",\"staffId\":\"" + teacherStaff + "\","
                    + (leaveType == null ? "" : "\"leaveTypeId\":\"" + leaveType + "\",") + "\"fromAt\":\"" + fromAt
                    + "\",\"toAt\":\"" + toAt + "\",\"dayPart\":\"" + dayPart + "\"}");
            assertThat(created.status()).as(created.text()).isEqualTo(201);
            return created.body().path("id").asString();
        }

        /**
         * org announces a holiday (created, updated or deleted) as it does from HolidayService; answers whether hr's
         * copy changed. A null institution is a holiday of the whole tenant.
         */
        boolean holiday(String verb, UUID id, UUID closes, String start, String end, int rowVersion) {
            String data = "{\"id\":\"" + id + "\"," + (closes == null ? "" : "\"institutionId\":\"" + closes + "\",")
                    + "\"title\":\"Closed\",\"startDate\":\"" + start + "\",\"endDate\":\"" + end
                    + "\",\"rowVersion\":" + rowVersion + "}";
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> holidays.apply(new EventEnvelope(
                            UUID.randomUUID(), "org", "org.holiday." + verb, Optional.of(tenant), "org.holiday", id,
                            Instant.now(), data, Optional.empty()))));
        }

        /**
         * The teacher's live attendance days as "date status leave replaced" (leave: the application id or -,
         * replaced: the status the leave overrode or -), waiting up to ten seconds for them to become what is expected:
         * time_attendance writes them from hr's decision events.
         */
        List<String> attendance(List<String> expected) throws InterruptedException {
            List<String> days = List.of();
            for (int attempt = 0; attempt < 100; attempt++) {
                days = CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                        () -> TestTransactions.of(database).read(() -> database.jdbc().sql(
                                "SELECT a.attendance_date || ' ' || a.status || ' '"
                                        + " || COALESCE(a.leave_application_id::text, '-') || ' '"
                                        + " || COALESCE(a.replaced_status, '-')"
                                        + " FROM time_attendance.staff_attendance a WHERE a.tenant_id = :tenantId"
                                        + " AND a.staff_id = '" + teacherStaff + "' AND a.deleted_at IS NULL"
                                        + " ORDER BY a.attendance_date").query(String.class).list()));
                if (days.equals(expected)) {
                    return days;
                }
                Thread.sleep(100);
            }
            return days;
        }

        /** The remark of the teacher's live attendance row of the day. */
        String remark(String day) {
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).read(() -> database.jdbc().sql(
                            "SELECT a.remark FROM time_attendance.staff_attendance a WHERE a.tenant_id = :tenantId"
                                    + " AND a.staff_id = '" + teacherStaff + "' AND a.attendance_date = '" + day
                                    + "' AND a.deleted_at IS NULL").query(String.class).single()));
        }

        /** The teacher's position for casual leave, as [entitled, taken, pending, available]. */
        List<String> position(String token) throws Exception {
            Reply reply = call("GET", "/v1/hr/staff/" + teacherStaff + ":leaveBalance?academicYearId=" + year, token,
                    null);
            assertThat(reply.status()).as(reply.text()).isEqualTo(200);
            for (JsonNode line : reply.body().path("data")) {
                if (casual.toString().equals(line.path("leaveTypeId").asString())) {
                    return List.of(line.path("entitledDays").asString(), line.path("takenDays").asString(),
                            line.path("pendingDays").asString(), line.path("availableDays").asString());
                }
            }
            return List.of();
        }
    }

    private Reply call(String method, String path, String token, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", "Bearer " + token);
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    private Reply approve(String application, String token, String body) throws Exception {
        return call("POST", APPLICATIONS + "/" + application + ":approve", token, body);
    }

    private static String token(UUID user, UUID tenant, List<String> institutions, String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", institutions.contains("*") ? List.of("*") : List.of());
        claims.put("ins", institutions);
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    @Test
    void approvedLeaveIsCountedAgainstTheBalanceAndNoMoreThanIsLeftCanBeApproved() throws Exception {
        School school = new School();
        assertThat(school.position(school.teacher)).as("the yearly allowance, nothing asked for yet")
                .containsExactly("10", "0", "0", "10");
        String threeDays = school.applies("2026-03-02T00:00:00Z", "2026-03-04T00:00:00Z", "full_day");
        String halfDay = school.applies("2026-03-09T03:00:00Z", "2026-03-09T07:00:00Z", "forenoon");
        assertThat(school.position(school.teacher)).as("asked for, not yet taken")
                .containsExactly("10", "0", "3.5", "6.5");

        Reply approved = approve(threeDays, school.head, "{\"reason\":\"  Enjoy  \"}");
        assertThat(approved.status()).as(approved.text()).isEqualTo(200);
        assertThat(approved.body().path("status").asString()).isEqualTo("approved");
        assertThat(approved.body().path("decidedByUserId").asString()).isEqualTo(school.headUser.toString());
        assertThat(approved.body().path("decidedOn").asString()).isNotEmpty();
        assertThat(approved.body().path("decisionComments").asString()).isEqualTo("Enjoy");
        assertThat(school.position(school.teacher)).containsExactly("10", "3", "0.5", "6.5");
        assertThat(approve(halfDay, school.head, "{}").status()).isEqualTo(200);
        assertThat(school.position(school.teacher)).containsExactly("10", "3.5", "0", "6.5");

        // Monday to the Tuesday after: seven working days, the weekend between them not counted
        String tooLong = school.applies("2026-04-06T00:00:00Z", "2026-04-14T00:00:00Z", "full_day");
        Reply refused = approve(tooLong, school.head, "{}");
        assertThat(refused.status()).as("seven days with six and a half left: " + refused.text()).isEqualTo(409);
        assertThat(refused.body().path("detail").asString()).contains("7 days").contains("6.5");
        assertThat(school.position(school.teacher)).as("a refused approval counts nothing")
                .containsExactly("10", "3.5", "7", "0");

        String untyped = school.applies("2026-05-04T00:00:00Z", "2026-05-29T00:00:00Z", "full_day", null);
        Reply uncounted = approve(untyped, school.head, "{}");
        assertThat(uncounted.status()).as("leave that names no type cannot be counted: " + uncounted.text())
                .isEqualTo(409);
        assertThat(school.position(school.teacher)).containsExactly("10", "3.5", "7", "0");

        // times of day do not change the days: Monday morning to Wednesday morning is three working days
        String byTheClock = school.applies("2026-06-01T09:00:00Z", "2026-06-03T08:00:00Z", "full_day");
        assertThat(school.position(school.teacher)).containsExactly("10", "3.5", "10", "0");
        Reply rejected = call("POST", APPLICATIONS + "/" + byTheClock + ":reject", school.head,
                "{\"reason\":\" Exams that week \"}");
        assertThat(rejected.status()).as(rejected.text()).isEqualTo(200);
        assertThat(rejected.body().path("decisionComments").asString()).as("the applicant reads why")
                .isEqualTo("Exams that week");
    }

    @Test
    void leaveTakesWorkingDaysOnlyWithoutWeekendsOrTheSchoolsAndTheTenantsHolidays() throws Exception {
        School school = new School();
        UUID ownHoliday = UUID.randomUUID();
        UUID tenantHoliday = UUID.randomUUID();
        assertThat(school.holiday("created", ownHoliday, school.institution, "2026-03-04", "2026-03-04", 1)).isTrue();
        assertThat(school.holiday("created", tenantHoliday, null, "2026-03-05", "2026-03-05", 1)).isTrue();
        assertThat(school.holiday("created", UUID.randomUUID(), UUID.randomUUID(), "2026-03-06", "2026-03-06", 1))
                .as("another school's holiday is kept too").isTrue();

        // Monday 2 to Sunday 8 March: seven calendar days, less the weekend (7, 8), the school's holiday (4) and the
        // tenant's (5); another school's holiday (6) closes nothing here
        String week = school.applies("2026-03-02T00:00:00Z", "2026-03-08T00:00:00Z", "full_day");
        assertThat(school.position(school.teacher)).containsExactly("10", "0", "3", "7");
        String onTheHoliday = school.applies("2026-03-04T00:00:00Z", "2026-03-04T00:00:00Z", "forenoon");
        String onSaturday = school.applies("2026-03-07T00:00:00Z", "2026-03-07T00:00:00Z", "afternoon");
        assertThat(school.position(school.teacher)).as("a half day on a holiday or a weekend takes nothing")
                .containsExactly("10", "0", "3", "7");
        String halfDay = school.applies("2026-03-09T00:00:00Z", "2026-03-09T00:00:00Z", "afternoon");
        assertThat(school.position(school.teacher)).containsExactly("10", "0", "3.5", "6.5");

        assertThat(school.holiday("deleted", tenantHoliday, null, "2026-03-05", "2026-03-05", 2)).isTrue();
        assertThat(school.holiday("updated", tenantHoliday, null, "2026-03-05", "2026-03-05", 1))
                .as("a late event cannot restore a deleted holiday").isFalse();
        assertThat(school.position(school.teacher)).as("the deleted holiday is a working day again")
                .containsExactly("10", "0", "4.5", "5.5");
        assertThat(school.holiday("updated", ownHoliday, school.institution, "2026-03-03", "2026-03-04", 2)).isTrue();
        assertThat(school.position(school.teacher)).as("the school's holiday now closes the 3rd and the 4th")
                .containsExactly("10", "0", "3.5", "6.5");

        assertThat(approve(week, school.head, "{}").status()).isEqualTo(200);
        assertThat(approve(onTheHoliday, school.head, "{}").status()).isEqualTo(200);
        assertThat(approve(onSaturday, school.head, "{}").status()).isEqualTo(200);
        assertThat(approve(halfDay, school.head, "{}").status()).isEqualTo(200);
        assertThat(school.position(school.teacher)).as("2, 5 and 6 March, and half of the 9th")
                .containsExactly("10", "3.5", "0", "6.5");
    }

    @Test
    void approvedLeaveIsRecordedInStaffAttendanceOnItsWorkingDaysAndCancellingItGivesTheDaysBack() throws Exception {
        School school = new School();
        // Friday 2 to Tuesday 6 January 2099: the weekend (3, 4) and the tenant's holiday (5) are not leave days
        assertThat(school.holiday("created", UUID.randomUUID(), null, "2099-01-05", "2099-01-05", 1)).isTrue();
        UUID recorded = UUID.randomUUID();
        school.sql("INSERT INTO time_attendance.staff_attendance (id, tenant_id, staff_id, attendance_date, status,"
                + " remark, row_version) VALUES ('" + recorded + "', :tenantId, '" + school.teacherStaff
                + "', '2099-01-06', 'present', 'entered ahead', 1)");
        String leave = school.applies("2099-01-02T00:00:00Z", "2099-01-06T00:00:00Z", "full_day");
        String halfDay = school.applies("2099-01-07T00:00:00Z", "2099-01-07T00:00:00Z", "forenoon");
        assertThat(school.attendance(List.of("2099-01-06 present - -"))).as("pending leave records nothing")
                .containsExactly("2099-01-06 present - -");

        assertThat(approve(leave, school.head, "{}").status()).isEqualTo(200);
        assertThat(approve(halfDay, school.head, "{}").status()).isEqualTo(200);
        List<String> onLeave = List.of("2099-01-02 on_leave " + leave + " -",
                "2099-01-06 on_leave " + leave + " present", "2099-01-07 half_day " + halfDay + " -");
        assertThat(school.attendance(onLeave)).as("leave wins over the recorded day, which keeps its status")
                .containsExactlyElementsOf(onLeave);
        assertThat(school.position(school.teacher)).containsExactly("10", "2.5", "0", "7.5");

        assertThat(call("POST", APPLICATIONS + "/" + leave + ":cancel", school.teacher, "{}").status())
                .isEqualTo(200);
        List<String> back = List.of("2099-01-06 present - -", "2099-01-07 half_day " + halfDay + " -");
        assertThat(school.attendance(back)).as("the day written for the leave goes, the recorded day is restored")
                .containsExactlyElementsOf(back);
    }

    @Test
    void cancellingGivesBackExactlyTheDaysTheApprovalCountedWhateverTheCalendarBecameSince() throws Exception {
        School school = new School();
        // Monday 5 and Tuesday 6 January 2099: two working days when approved
        String leave = school.applies("2099-01-05T00:00:00Z", "2099-01-06T00:00:00Z", "full_day");
        Reply approved = approve(leave, school.head, "{}");
        assertThat(approved.status()).as(approved.text()).isEqualTo(200);
        assertThat(approved.body().path("countedDays").asString()).isEqualTo("2.0");
        assertThat(school.position(school.teacher)).containsExactly("10", "2", "0", "8");

        // the 6th becomes a holiday afterwards: a recount would now give back one day only
        assertThat(school.holiday("created", UUID.randomUUID(), null, "2099-01-06", "2099-01-06", 1)).isTrue();
        assertThat(call("POST", APPLICATIONS + "/" + leave + ":cancel", school.teacher, "{}").status())
                .isEqualTo(200);
        assertThat(school.position(school.teacher)).as("the balance is back where it was before the approval")
                .containsExactly("10", "0", "0", "10");
    }

    @Test
    void cancellingLeaveKeepsAChangedDayAndGivesAnotherLeavesDayItsRow() throws Exception {
        School school = new School();
        // Monday 2 and Tuesday 3 February 2099, and the forenoon of the 3rd again in a second application
        String week = school.applies("2099-02-02T00:00:00Z", "2099-02-03T00:00:00Z", "full_day");
        String forenoon = school.applies("2099-02-03T00:00:00Z", "2099-02-03T00:00:00Z", "forenoon");
        assertThat(approve(week, school.head, "{}").status()).isEqualTo(200);
        assertThat(approve(forenoon, school.head, "{}").status()).isEqualTo(200);
        List<String> recorded = List.of("2099-02-02 on_leave " + week + " -", "2099-02-03 on_leave " + week + " -");
        assertThat(school.attendance(recorded)).as("a day another leave explains is left to it")
                .containsExactlyElementsOf(recorded);
        school.sql("UPDATE time_attendance.staff_attendance SET status = 'present', row_version = row_version + 1"
                + " WHERE tenant_id = :tenantId AND staff_id = '" + school.teacherStaff + "'"
                + " AND attendance_date = '2099-02-02'");

        assertThat(call("POST", APPLICATIONS + "/" + week + ":cancel", school.teacher, "{}").status())
                .isEqualTo(200);
        List<String> after = List.of("2099-02-02 present - -", "2099-02-03 half_day " + forenoon + " -");
        assertThat(school.attendance(after))
                .as("the changed day stays as it was left, without the leave; the shared day is the other leave's")
                .containsExactlyElementsOf(after);
        assertThat(school.remark("2099-02-03")).isEqualTo("Leave: Casual leave");
    }

    @Test
    void anApprovedLeaveIsCancelledBeforeItStartsAndItsDaysGoBack() throws Exception {
        School school = new School();
        String ahead = school.applies("2099-01-05T00:00:00Z", "2099-01-06T00:00:00Z", "full_day");
        String begun = school.applies("2026-03-02T00:00:00Z", "2026-03-02T00:00:00Z", "full_day");
        assertThat(approve(ahead, school.head, "{}").status()).isEqualTo(200);
        assertThat(approve(begun, school.head, "{}").status()).isEqualTo(200);
        assertThat(school.position(school.teacher)).containsExactly("10", "3", "0", "7");

        String colleague = token(UUID.randomUUID(), school.tenant, List.of(school.institution.toString()),
                "hr.leave_application.read", CANCEL);
        Reply notTheirs = call("POST", APPLICATIONS + "/" + ahead + ":cancel", colleague, "{}");
        assertThat(notTheirs.status()).as("neither the applicant nor a decider: " + notTheirs.text()).isEqualTo(403);
        Reply cancelled = call("POST", APPLICATIONS + "/" + ahead + ":cancel", school.teacher,
                "{\"etag\":\"\\\"2\\\"\",\"reason\":\"Plans changed\"}");
        assertThat(cancelled.status()).as(cancelled.text()).isEqualTo(200);
        assertThat(cancelled.body().path("status").asString()).isEqualTo("cancelled");
        assertThat(school.position(school.teacher)).as("the two days are back").containsExactly("10", "1", "0", "9");
        Reply again = call("POST", APPLICATIONS + "/" + ahead + ":cancel", school.teacher, "{}");
        assertThat(again.status()).as("a cancelled leave is not cancelled twice").isEqualTo(409);

        Reply started = call("POST", APPLICATIONS + "/" + begun + ":cancel", school.teacher, "{}");
        assertThat(started.status()).as("a leave that has started stays: " + started.text()).isEqualTo(409);
        assertThat(started.body().path("detail").asString()).contains("started");
        assertThat(school.position(school.teacher)).containsExactly("10", "1", "0", "9");
        Reply stale = call("POST", APPLICATIONS + "/" + school.applies("2099-02-02T00:00:00Z", "2099-02-02T00:00:00Z",
                "full_day") + ":cancel", school.teacher, "{\"etag\":\"\\\"9\\\"\"}");
        assertThat(stale.status()).isEqualTo(412);
    }

    @Test
    void thePositionIsReadPageByPageInTheOrderTheSchoolListsItsTypes() throws Exception {
        School school = new School();
        UUID sick = UUID.randomUUID();
        school.sql("INSERT INTO hr.leave_type (id, tenant_id, institution_id, name, code, max_days_per_year, sort_order)"
                + " VALUES ('" + sick + "', :tenantId, '" + school.institution + "', 'Sick leave', 'SL', 5, -1)");
        String own = "/v1/hr/staff/" + school.teacherStaff + ":leaveBalance?academicYearId=" + school.year;
        Reply first = call("GET", own + "&size=1", school.teacher, null);
        assertThat(first.status()).as(first.text()).isEqualTo(200);
        assertThat(first.body().path("data").get(0).path("leaveTypeId").asString()).isEqualTo(sick.toString());
        String next = first.body().path("paging").path("next").asString();
        assertThat(next).contains("page=2").contains("cursor=");
        Reply second = call("GET", next, school.teacher, null);
        assertThat(second.status()).as(second.text()).isEqualTo(200);
        assertThat(second.body().path("data").get(0).path("leaveTypeId").asString())
                .isEqualTo(school.casual.toString());
        assertThat(second.body().path("paging").has("next")).isFalse();
        Reply otherYear = call("GET", "/v1/hr/staff/" + school.teacherStaff + ":leaveBalance?academicYearId="
                + UUID.randomUUID() + "&page=2&size=1&" + next.substring(next.indexOf("cursor=")), school.teacher, null);
        assertThat(otherYear.status()).as("a cursor of another year").isEqualTo(400);
    }

    @Test
    void anApprovalNeedsItsPermissionAnotherPersonAndAPendingApplicationInTheCallersInstitutions() throws Exception {
        School school = new School();
        String application = school.applies("2026-03-02T00:00:00Z", "2026-03-02T23:59:00Z", "full_day");

        assertThat(approve(application, school.teacher, "{}").status()).as("nobody approves their own leave")
                .isEqualTo(403);
        assertThat(approve(application, token(UUID.randomUUID(), school.tenant,
                List.of(school.institution.toString()), "hr.leave_application.read"), "{}").status()).isEqualTo(403);
        assertThat(approve(application, token(UUID.randomUUID(), school.tenant,
                List.of(UUID.randomUUID().toString()), "*"), "{}").status()).as("another institution").isEqualTo(404);
        assertThat(approve(application, token(UUID.randomUUID(), UUID.randomUUID(), List.of("*"), "*"), "{}")
                .status()).as("another tenant").isEqualTo(404);
        assertThat(approve(application, school.head, "{\"etag\":\"\\\"99\\\"\"}").status()).isEqualTo(412);
        assertThat(approve(application, school.head, "{\"reason\":\"" + "x".repeat(4001) + "\"}").status())
                .isEqualTo(422);
        assertThat(school.position(school.teacher)).as("refused calls count nothing")
                .containsExactly("10", "0", "1", "9");

        Reply approved = approve(application, school.head, "{}");
        assertThat(approved.status()).as(approved.text()).isEqualTo(200);
        String etag = approved.body().path("etag").asString();
        assertThat(approve(application, school.head, "{}").status()).as("decided once").isEqualTo(409);
        assertThat(call("POST", APPLICATIONS + "/" + application + ":reject", token(UUID.randomUUID(),
                school.tenant, List.of(school.institution.toString()), "hr.leave_application.reject_leave"), "{}")
                .status()).isEqualTo(409);
        assertThat(call("PATCH", APPLICATIONS + "/" + application, school.head,
                "{\"toAt\":\"2026-03-20T00:00:00Z\"}", "If-Match", etag).status())
                .as("what was approved is what is counted").isEqualTo(409);
        assertThat(call("DELETE", APPLICATIONS + "/" + application, school.head, null, "If-Match", etag).status())
                .isEqualTo(409);
        assertThat(school.position(school.teacher)).containsExactly("10", "1", "0", "9");
    }

    @Test
    void leaveIsAppliedForInTheStaffMembersOwnSchoolAndTheDecisionIsTheDecidersToWrite() throws Exception {
        School school = new School();
        UUID elsewhere = UUID.randomUUID();
        UUID generous = UUID.randomUUID();
        UUID retired = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();
        school.sql("INSERT INTO hr.leave_type (id, tenant_id, institution_id, name, code, max_days_per_year) VALUES ('"
                        + generous + "', :tenantId, '" + elsewhere + "', 'Other school', 'GEN', 30)",
                "INSERT INTO hr.leave_type (id, tenant_id, institution_id, name, code, max_days_per_year, is_active)"
                        + " VALUES ('" + retired + "', :tenantId, '" + school.institution + "', 'Old', 'OLD', 5, false)",
                "INSERT INTO hr.staff (id, tenant_id, institution_id, person_id, employee_code) VALUES ('" + outsider
                        + "', :tenantId, '" + elsewhere + "', '" + UUID.randomUUID() + "', 'X-1')");
        String wide = token(school.teacherUser, school.tenant, List.of("*"), "*");
        String start = "{\"institutionId\":\"" + school.institution + "\",\"academicYearId\":\"" + school.year
                + "\",\"fromAt\":\"2026-03-02T00:00:00Z\",";
        String ownStaff = "\"staffId\":\"" + school.teacherStaff + "\",";

        Reply foreignType = call("POST", APPLICATIONS, wide, start + ownStaff + "\"toAt\":\"2026-03-02T00:00:00Z\","
                + "\"leaveTypeId\":\"" + generous + "\"}");
        assertThat(foreignType.status()).as("another school's leave type: " + foreignType.text()).isEqualTo(422);
        assertThat(foreignType.body().path("errors").get(0).path("field").asString()).isEqualTo("leaveTypeId");
        assertThat(call("POST", APPLICATIONS, wide, start + ownStaff + "\"toAt\":\"2026-03-02T00:00:00Z\","
                + "\"leaveTypeId\":\"" + retired + "\"}").status()).as("a type no longer offered").isEqualTo(422);
        Reply foreignStaff = call("POST", APPLICATIONS, wide, start + "\"staffId\":\"" + outsider
                + "\",\"toAt\":\"2026-03-02T00:00:00Z\"}");
        assertThat(foreignStaff.status()).as("another school's staff member: " + foreignStaff.text()).isEqualTo(422);
        assertThat(foreignStaff.body().path("errors").get(0).path("field").asString()).isEqualTo("staffId");
        assertThat(call("POST", APPLICATIONS, wide, start + ownStaff + "\"toAt\":\"2026-03-04T00:00:00Z\","
                + "\"dayPart\":\"forenoon\",\"leaveTypeId\":\"" + school.casual + "\"}").status())
                .as("a half day is one day").isEqualTo(422);

        String application = school.applies("2026-03-02T00:00:00Z", "2026-03-02T00:00:00Z", "full_day");
        String etag = call("GET", APPLICATIONS + "/" + application, school.teacher, null).body().path("etag")
                .asString();
        Reply ownWords = call("PATCH", APPLICATIONS + "/" + application,
                school.teacher, "{\"decisionComments\":\"Approved by the head\"}", "If-Match", etag);
        assertThat(ownWords.status()).as(ownWords.text()).isEqualTo(200);
        assertThat(ownWords.body().has("decisionComments"))
                .as("the applicant does not write the decision, it is ignored: " + ownWords.text()).isFalse();
        Reply approved = approve(application, school.head, "{}");
        assertThat(approved.status()).as(approved.text()).isEqualTo(200);
        assertThat(approved.body().has("decisionComments")).as("no comment was given").isFalse();
    }

    @Test
    void nobodyApprovesTheirOwnLeaveUnderAnotherStaffRecordOfTheirs() throws Exception {
        School school = new School();
        UUID person = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        school.sql("INSERT INTO hr.staff (id, tenant_id, institution_id, person_id, employee_code) VALUES ('" + second
                + "', :tenantId, '" + school.institution + "', '" + person + "', 'H-2')");
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", school.headUser.toString());
        claims.put("pid", person.toString());
        claims.put("tid", school.tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of("hr.leave_application.read", "hr.leave_application.create", APPROVE));
        claims.put("tperms", List.of());
        claims.put("ins", List.of(school.institution.toString()));
        String headAsPerson = TOKENS.custom(claims, Instant.now().plusSeconds(900));
        Reply filed = call("POST", APPLICATIONS, headAsPerson, "{\"institutionId\":\"" + school.institution
                + "\",\"academicYearId\":\"" + school.year + "\",\"staffId\":\"" + second + "\",\"leaveTypeId\":\""
                + school.casual + "\",\"fromAt\":\"2026-03-02T00:00:00Z\",\"toAt\":\"2026-03-02T00:00:00Z\"}");
        assertThat(filed.status()).as(filed.text()).isEqualTo(201);
        String application = filed.body().path("id").asString();
        assertThat(approve(application, headAsPerson, "{}").status())
                .as("the staff record has no login, but it is the same person").isEqualTo(403);
        assertThat(approve(application, token(UUID.randomUUID(), school.tenant,
                List.of(school.institution.toString()), APPROVE), "{}").status()).as("someone else may").isEqualTo(200);
    }

    @Test
    void twoApprovalsAtOnceDoNotSpendTheSameDays() throws Exception {
        School school = new School();
        // eight applications of three days each with ten days to spend: three can be approved, whatever the order
        List<CompletableFuture<Reply>> answers = new ArrayList<>();
        List<String> applications = new ArrayList<>();
        for (int week = 0; week < 8; week++) {
            String monday = LocalDate.of(2026, 3, 2).plusWeeks(week).toString();
            applications.add(school.applies(monday + "T00:00:00Z",
                    LocalDate.parse(monday).plusDays(2) + "T00:00:00Z", "full_day"));
        }
        for (String application : applications) {
            answers.add(CompletableFuture.supplyAsync(() -> approveUnchecked(application, school.head)));
        }
        // and the same application decided twice at once is decided once
        answers.add(CompletableFuture.supplyAsync(() -> approveUnchecked(applications.get(0), school.head)));
        List<Integer> statuses = answers.stream().map(answer -> answer.join().status()).toList();
        assertThat(statuses.stream().filter(status -> status == 200).count()).as(statuses.toString()).isEqualTo(3);
        assertThat(statuses).allMatch(status -> status == 200 || status == 409);
        assertThat(school.position(school.teacher)).containsExactly("10", "9", "15", "0");
    }

    private Reply approveUnchecked(String application, String token) {
        try {
            return approve(application, token, "{}");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void aPositionIsTheStaffMembersOwnAndCountsTheBalanceRowTheYearAndTheActiveTypes() throws Exception {
        School school = new School();
        UUID retired = UUID.randomUUID();
        UUID otherYear = UUID.randomUUID();
        school.sql("INSERT INTO hr.leave_type (id, tenant_id, institution_id, name, code, max_days_per_year, is_active)"
                        + " VALUES ('" + retired + "', :tenantId, '" + school.institution + "', 'Old', 'OLD', 5, false)",
                "INSERT INTO hr.leave_type (id, tenant_id, institution_id, academic_year_id, name, code,"
                        + " max_days_per_year) VALUES ('" + otherYear + "', :tenantId, '" + school.institution + "', '"
                        + UUID.randomUUID() + "', 'Last year', 'LY', 5)",
                "INSERT INTO hr.leave_balance (tenant_id, staff_id, leave_type_id, academic_year_id, entitled_days,"
                        + " used_days, carried_forward_days) VALUES (:tenantId, '" + school.teacherStaff + "', '"
                        + school.casual + "', '" + school.year + "', 12, 4, 2.5)");
        String own = "/v1/hr/staff/" + school.teacherStaff + ":leaveBalance";

        Reply mine = call("GET", own + "?academicYearId=" + school.year, school.teacher, null);
        assertThat(mine.status()).as(mine.text()).isEqualTo(200);
        assertThat(mine.body().path("data")).as("active types of this year only").hasSize(1);
        assertThat(mine.body().path("paging").has("next")).isFalse();
        assertThat(school.position(school.teacher)).as("the balance row with what was carried forward")
                .containsExactly("14.5", "4", "0", "10.5");
        assertThat(school.position(school.head)).as("the head holds the balance permission")
                .containsExactly("14.5", "4", "0", "10.5");

        String colleague = token(UUID.randomUUID(), school.tenant, List.of(school.institution.toString()), BALANCE);
        assertThat(call("GET", own + "?academicYearId=" + school.year, colleague, null).status())
                .as("a colleague's position is not theirs to read").isEqualTo(403);
        assertThat(call("GET", own + "?academicYearId=" + school.year, token(UUID.randomUUID(), school.tenant,
                List.of(school.institution.toString()), "hr.staff.read"), null).status()).isEqualTo(403);
        assertThat(call("GET", own, school.teacher, null).status()).as("the year is named").isEqualTo(400);
        assertThat(call("GET", "/v1/hr/staff/" + UUID.randomUUID() + ":leaveBalance?academicYearId=" + school.year,
                school.head, null).status()).isEqualTo(404);
        assertThat(call("GET", own + "?academicYearId=" + school.year, token(UUID.randomUUID(), school.tenant,
                List.of(UUID.randomUUID().toString()), "*"), null).status()).as("another institution").isEqualTo(404);
        assertThat(call("GET", own + "?academicYearId=" + school.year, token(UUID.randomUUID(), UUID.randomUUID(),
                List.of("*"), "*"), null).status()).as("another tenant").isEqualTo(404);
    }
}
