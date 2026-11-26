package com.mysms.students.sis;

import com.mysms.students.StudentsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.students.sis.config.SisDatabaseConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
 * The hand-written sis operations over HTTP against real PostgreSQL as the application role (row-level security on):
 * the student's status, bonafide certificates, portal accounts, the student summary, a guardian's children, the
 * discipline steps, promotions and subject registrations. Each test works in a school of a tenant of its own.
 *
 * <p>This module's test runs sis alone: the parts that need another service (iam for portal accounts, academics for
 * proposing promotions, naming a target class and loading course subjects) must answer 503 here, after every check
 * sis makes itself; the parts that are optional (people, org) are left out. The flows across the services belong to
 * the app module's component test.
 */
@SpringBootTest(classes = StudentsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SisWorkflowComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String STUDENTS = "/v1/sis/students/";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(SisDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "sis", "admission", "careers");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {
    }

    /** One school of one tenant, its administrator, and helpers that put rows in place as the tenant. */
    private final class School {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final UUID year = UUID.randomUUID();
        final UUID nextYear = UUID.randomUUID();
        final UUID grade = UUID.randomUUID();
        final UUID classSection = UUID.randomUUID();
        final String admin = token(tenant, UUID.randomUUID(), null, List.of("*"), List.of("*"), "*");
        final UUID relationship = UUID.randomUUID();

        School() {
            sql("INSERT INTO sis.relationship_type (id, tenant_id, code, name, row_version) "
                    + "VALUES (:a, :tenantId, 'MOTHER', 'Mother', 1)", relationship);
        }

        /** An active student with the admission number, placed in the class of the year since the date. */
        UUID student(String admissionNumber, LocalDate placedFrom) {
            UUID student = UUID.randomUUID();
            sql("INSERT INTO sis.student (id, tenant_id, person_id, institution_id, admission_date, row_version) "
                    + "VALUES (:a, :tenantId, :b, :c, DATE '2020-06-01', 1)", student, UUID.randomUUID(), institution);
            if (admissionNumber != null) {
                sql("UPDATE sis.student SET admission_number = :t WHERE tenant_id = :tenantId AND id = :a", student,
                        admissionNumber);
            }
            sql("INSERT INTO sis.student_enrollment (id, tenant_id, student_id, academic_year_id, class_section_id, "
                    + "effective_from, enrolled_on, row_version) VALUES (:a, :tenantId, :b, :c, :d, CAST(:t AS date), "
                    + "CAST(:t AS date), 1)", UUID.randomUUID(), student, year, classSection, placedFrom.toString());
            return student;
        }

        /** A guardian (account and person given) with an active or inactive link to the student. */
        UUID guardian(UUID userId, UUID personId, UUID student, String linkStatus) {
            UUID guardian = UUID.randomUUID();
            sql("INSERT INTO sis.guardian (id, tenant_id, person_id, user_id, email, row_version) "
                    + "VALUES (:a, :tenantId, :b, :c, :t, 1)", guardian, personId, userId,
                    "g" + guardian.toString().substring(0, 8) + "@example.test");
            link(guardian, student, linkStatus);
            return guardian;
        }

        void link(UUID guardian, UUID student, String linkStatus) {
            sql("INSERT INTO sis.student_guardian (id, tenant_id, student_id, guardian_id, relationship_type_id, "
                    + "status, row_version) VALUES (:a, :tenantId, :b, :c, :d, :t, 1)", UUID.randomUUID(), student,
                    guardian, relationship, linkStatus);
        }

        UUID incident(UUID student, boolean shared) {
            UUID incident = UUID.randomUUID();
            sql("INSERT INTO sis.discipline_incident (id, tenant_id, institution_id, academic_year_id, student_id, "
                    + "incident_at, reported_by_user_id, details, notify_guardians, row_version) VALUES (:a, "
                    + ":tenantId, :b, :c, :d, now(), :b, 'Late again', " + shared + ", 1)", incident, institution,
                    year, student);
            return incident;
        }

        UUID promotion(UUID student, String status, UUID toGrade) {
            UUID promotion = UUID.randomUUID();
            sql("INSERT INTO sis.promotion (id, tenant_id, student_id, institution_id, from_academic_year_id, "
                    + "from_grade_level_id, to_academic_year_id, to_grade_level_id, status, row_version) "
                    + "VALUES (:a, :tenantId, :b, :c, :d, :e, :f, :g, :t, 1)", promotion, student, institution, year,
                    grade, nextYear, toGrade, status);
            return promotion;
        }

        /** A subject registration of the student in the state, with the given number of lines. */
        UUID registration(UUID student, String status, int lines) {
            UUID enrollment = UUID.randomUUID();
            UUID registration = UUID.randomUUID();
            sql("INSERT INTO sis.program_enrollment (id, tenant_id, student_id, course_id, row_version) "
                    + "VALUES (:a, :tenantId, :b, :c, 1)", enrollment, student, UUID.randomUUID());
            sql("INSERT INTO sis.subject_registration (id, tenant_id, student_id, course_id, academic_year_id, "
                    + "program_enrollment_id, status, row_version) VALUES (:a, :tenantId, :b, :c, :d, :e, :t, 1)",
                    registration, student, UUID.randomUUID(), year, enrollment, status);
            for (int i = 0; i < lines; i++) {
                sql("INSERT INTO sis.subject_registration_line (id, tenant_id, subject_registration_id, subject_id, "
                        + "course_subject_id, selection_type, row_version) VALUES (:a, :tenantId, :b, :c, :d, "
                        + "'compulsory', 1)", UUID.randomUUID(), registration, UUID.randomUUID(), UUID.randomUUID());
            }
            return registration;
        }

        /** Runs one statement as the tenant; :a … :g are the ids in order, :t the text. */
        void sql(String statement, Object... values) {
            CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> {
                        var spec = database.jdbc().sql(statement);
                        String[] names = {"a", "b", "c", "d", "e", "f", "g"};
                        int id = 0;
                        for (Object value : values) {
                            spec = value instanceof String ? spec.param("t", value) : spec.param(names[id++], value);
                        }
                        return spec.update();
                    }));
        }

        <T> T one(String query, Class<T> type, Object id) {
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).read(() -> database.jdbc().sql(query).param("id", id)
                            .query(type).single()));
        }

        List<String> events(UUID aggregate, String type) {
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).read(() -> database.jdbc().sql("SELECT payload FROM "
                            + "sis.outbox_event WHERE tenant_id = :tenantId AND aggregate_id = :id AND event_type = "
                            + ":type ORDER BY id").param("id", aggregate).param("type", type).query(String.class)
                            .list()));
        }
    }

    // ---- sis.ChangeStudentStatus ----

    @Test
    void aStudentWhoLeavesHasTheirPlacementEndedOnTheExitDate() throws Exception {
        School school = new School();
        UUID student = school.student("A-1", LocalDate.parse("2026-04-01"));
        String path = STUDENTS + student + ":changeStatus";

        assertThat(call("POST", path, school.admin, "{\"status\":\"withdrawn\"}").status())
                .as("an exit status needs the exit date").isEqualTo(422);
        assertThat(call("POST", path, school.admin, "{\"status\":\"inactive\",\"exitDate\":\"2026-09-01\"}").status())
                .as("only an exit status takes exit details").isEqualTo(422);
        assertThat(call("POST", path, school.admin, "{\"status\":\"withdrawn\",\"exitDate\":\"2019-01-01\"}")
                .status()).as("not before the admission").isEqualTo(422);
        assertThat(call("POST", path, school.admin, "{\"status\":\"withdrawn\",\"exitDate\":\"2026-09-01\","
                + "\"etag\":\"\\\"7\\\"\"}").status()).as("a stale etag").isEqualTo(412);
        assertThat(call("POST", path, school.admin, "{\"status\":\"active\"}").status())
                .as("the status it has").isEqualTo(409);
        String outsider = token(school.tenant, UUID.randomUUID(), null, List.of(UUID.randomUUID().toString()),
                List.of(), "sis.student.change_student_status");
        assertThat(call("POST", path, outsider, "{\"status\":\"withdrawn\",\"exitDate\":\"2026-09-01\"}").status())
                .as("outside the caller's institutions").isEqualTo(404);
        assertThat(call("POST", path, token(UUID.randomUUID(), UUID.randomUUID(), null, List.of("*"), List.of("*"),
                "*"), "{\"status\":\"withdrawn\",\"exitDate\":\"2026-09-01\"}").status()).as("another tenant")
                .isEqualTo(404);
        assertThat(call("POST", path, token(school.tenant, UUID.randomUUID(), null, List.of("*"), List.of("*"),
                "sis.student.update"), "{\"status\":\"withdrawn\",\"exitDate\":\"2026-09-01\"}").status())
                .as("the step has its own permission").isEqualTo(403);

        Reply left = call("POST", path, school.admin,
                "{\"status\":\"transferred\",\"exitDate\":\"2026-09-01\",\"exitReason\":\"Family moved\"}");

        assertThat(left.status()).as(left.text()).isEqualTo(200);
        assertThat(left.body().path("status").asString()).isEqualTo("transferred");
        assertThat(left.body().path("exitDate").asString()).isEqualTo("2026-09-01");
        assertThat(left.body().path("exitReason").asString()).isEqualTo("Family moved");
        assertThat(school.one("SELECT enrollment_status || ' ' || effective_to || ' ' || left_on FROM "
                + "sis.student_enrollment WHERE tenant_id = :tenantId AND student_id = :id", String.class, student))
                .isEqualTo("left 2026-09-01 2026-09-01");
        assertThat(school.events(student, "sis.student.changeStatus")).singleElement().asString()
                .contains("\"status\":\"transferred\"").contains("\"rowVersion\":2");

        Reply back = call("POST", path, school.admin, "{\"status\":\"active\"}");
        assertThat(back.status()).as(back.text()).isEqualTo(200);
        assertThat(back.body().has("exitDate")).as("re-admission clears the exit details").isFalse();
    }

    // ---- sis.IssueBonafideCertificate ----

    @Test
    void certificatesAreNumberedInTurnPerInstitutionAndYear() throws Exception {
        School school = new School();
        UUID first = school.student("B-1", LocalDate.parse("2026-04-01"));
        UUID second = school.student("B-2", LocalDate.parse("2026-04-01"));
        String year = String.valueOf(LocalDate.now(ZoneOffset.UTC).getYear());

        Reply issued = call("POST", STUDENTS + first + ":issueBonafideCertificate", school.admin,
                "{\"purpose\":\"Passport\"}");

        assertThat(issued.status()).as(issued.text()).isEqualTo(202);
        assertThat(issued.body().path("status").asString()).isEqualTo("succeeded");
        assertThat(issued.body().path("kind").asString()).isEqualTo("sis.IssueBonafideCertificate");
        assertThat(call("GET", "/v1/sis/operations/" + issued.body().path("operationId").asString(), school.admin,
                null).status()).isEqualTo(200);
        assertThat(call("POST", STUDENTS + second + ":issueBonafideCertificate", school.admin, "{}").status())
                .isEqualTo(202);
        assertThat(call("POST", STUDENTS + first + ":issueBonafideCertificate", school.admin, "{}").status())
                .isEqualTo(202);
        assertThat(call("GET", STUDENTS + first, school.admin, null).body().path("bonafideCertificateNumber")
                .asString()).isEqualTo("BC/" + year + "/000003");
        assertThat(call("GET", STUDENTS + second, school.admin, null).body().path("bonafideCertificateNumber")
                .asString()).isEqualTo("BC/" + year + "/000002");

        School other = new School();
        UUID elsewhere = other.student("B-1", LocalDate.parse("2026-04-01"));
        assertThat(call("POST", STUDENTS + elsewhere + ":issueBonafideCertificate", other.admin, "{}").status())
                .isEqualTo(202);
        assertThat(call("GET", STUDENTS + elsewhere, other.admin, null).body().path("bonafideCertificateNumber")
                .asString()).as("another tenant's series").isEqualTo("BC/" + year + "/000001");

        call("POST", STUDENTS + second + ":changeStatus", school.admin, "{\"status\":\"inactive\"}");
        assertThat(call("POST", STUDENTS + second + ":issueBonafideCertificate", school.admin, "{}").status())
                .as("only a current student").isEqualTo(409);
        assertThat(call("POST", STUDENTS + UUID.randomUUID() + ":issueBonafideCertificate", school.admin, "{}")
                .status()).isEqualTo(404);
    }

    // ---- sis.CreateStudentPortalAccount, sis.CreatePortalAccount ----

    @Test
    void portalAccountsAreCheckedInSisBeforeIamIsAsked() throws Exception {
        School school = new School();
        UUID student = school.student("C-1", LocalDate.parse("2026-04-01"));
        UUID linked = school.student("C-2", LocalDate.parse("2026-04-01"));
        school.sql("UPDATE sis.student SET user_id = :b WHERE tenant_id = :tenantId AND id = :a", linked,
                UUID.randomUUID());
        UUID unnumbered = school.student(null, LocalDate.parse("2026-04-01"));

        assertThat(call("POST", STUDENTS + linked + ":createPortalAccount", school.admin, "{}").status())
                .as("already has an account").isEqualTo(409);
        assertThat(call("POST", STUDENTS + unnumbered + ":createPortalAccount", school.admin, "{}").status())
                .as("signs in with the admission number").isEqualTo(409);
        assertThat(call("POST", STUDENTS + student + ":createPortalAccount", token(school.tenant, UUID.randomUUID(),
                null, List.of(UUID.randomUUID().toString()), List.of(), "*"), "{}").status()).isEqualTo(404);
        assertThat(call("POST", STUDENTS + student + ":createPortalAccount", school.admin, "{\"sendInvite\":true}")
                .status()).as("invitations cannot be sent yet").isEqualTo(422);
        Reply unconfigured = call("POST", STUDENTS + student + ":createPortalAccount", school.admin, "{}");
        assertThat(unconfigured.status()).as("iam is not running here").isEqualTo(503);
        assertThat(school.one("SELECT count(*) FROM sis.student WHERE tenant_id = :tenantId AND id = :id "
                + "AND user_id IS NULL", Integer.class, student)).isEqualTo(1);

        UUID lonely = UUID.randomUUID();
        school.sql("INSERT INTO sis.guardian (id, tenant_id, person_id, email, row_version) "
                + "VALUES (:a, :tenantId, :b, 'lonely@example.test', 1)", lonely, UUID.randomUUID());
        assertThat(call("POST", "/v1/sis/guardians/" + lonely + ":createPortalAccount", school.admin, "{}").status())
                .as("a guardian of no student").isEqualTo(409);
        UUID parent = school.guardian(null, UUID.randomUUID(), student, "active");
        assertThat(call("POST", "/v1/sis/guardians/" + parent + ":createPortalAccount", token(school.tenant,
                UUID.randomUUID(), null, List.of(UUID.randomUUID().toString()), List.of(), "*"), "{}").status())
                .as("the child's institution is outside the caller's scope").isEqualTo(403);
        assertThat(call("POST", "/v1/sis/guardians/" + parent + ":createPortalAccount", school.admin, "{}").status())
                .isEqualTo(503);
    }

    // ---- sis.GetStudentSummary, sis.ListMyChildren ----

    @Test
    void theSummaryIsForStaffInScopeTheStudentAndTheirFamilyOnly() throws Exception {
        School school = new School();
        UUID student = school.student("D-1", LocalDate.parse("2026-04-01"));
        UUID parentAccount = UUID.randomUUID();
        school.guardian(parentAccount, UUID.randomUUID(), student, "active");
        school.incident(student, true);
        school.incident(student, false);
        String path = STUDENTS + student + ":summary";

        Reply staff = call("GET", path, school.admin, null);
        assertThat(staff.status()).as(staff.text()).isEqualTo(200);
        assertThat(staff.body().path("student").path("id").asString()).isEqualTo(student.toString());
        assertThat(staff.body().path("person").path("displayName").asString())
                .as("people is not running: the admission number names the person").isEqualTo("D-1");
        assertThat(staff.body().path("currentEnrollment").path("academicYearId").asString())
                .isEqualTo(school.year.toString());
        assertThat(staff.body().path("guardians")).hasSize(1);
        assertThat(staff.body().path("openIncidents").asInt()).isEqualTo(2);
        assertThat(staff.body().has("attendance")).isFalse();

        String parent = token(school.tenant, parentAccount, null, List.of(), List.of(),
                "sis.student.get_student_summary");
        Reply family = call("GET", path, parent, null);
        assertThat(family.status()).as(family.text()).isEqualTo(200);
        assertThat(family.body().path("openIncidents").asInt()).as("only what was shared").isEqualTo(1);

        assertThat(call("GET", path, token(school.tenant, UUID.randomUUID(), null, List.of("*"), List.of("*"),
                "sis.student.get_student_summary"), null).status())
                .as("another parent of the school, without the staff read").isEqualTo(404);
        assertThat(call("GET", path, token(school.tenant, UUID.randomUUID(), null,
                List.of(UUID.randomUUID().toString()), List.of(), "sis.student.get_student_summary",
                "sis.student.read"), null).status()).as("staff of another school").isEqualTo(404);
        assertThat(call("GET", path, token(UUID.randomUUID(), parentAccount, null, List.of(), List.of(),
                "sis.student.get_student_summary"), null).status()).as("another tenant").isEqualTo(404);
        assertThat(call("GET", STUDENTS + student + ":summary?academicYearId=" + UUID.randomUUID(), school.admin,
                null).body().has("currentEnrollment")).as("no placement in that year").isFalse();
    }

    @Test
    void aGuardianListsTheirOwnChildrenOnly() throws Exception {
        School school = new School();
        UUID first = school.student("E-1", LocalDate.parse("2026-04-01"));
        UUID second = school.student("E-2", LocalDate.parse("2026-04-01"));
        UUID formerly = school.student("E-3", LocalDate.parse("2026-04-01"));
        UUID someoneElses = school.student("E-4", LocalDate.parse("2026-04-01"));
        UUID account = UUID.randomUUID();
        UUID person = UUID.randomUUID();
        UUID guardian = school.guardian(account, person, first, "active");
        school.link(guardian, second, "active");
        school.link(guardian, formerly, "inactive");
        school.guardian(UUID.randomUUID(), UUID.randomUUID(), someoneElses, "active");

        Reply mine = call("GET", "/v1/sis/me/children", token(school.tenant, account, null, List.of(), List.of(),
                "sis.guardian.list_my_children"), null);
        assertThat(mine.status()).as(mine.text()).isEqualTo(200);
        assertThat(ids(mine, "student")).containsExactlyInAnyOrder(first.toString(), second.toString());
        assertThat(mine.body().path("data").get(0).path("relationship").asString()).isEqualTo("Mother");
        assertThat(mine.body().path("data").get(0).path("person").path("displayName").asString()).startsWith("E-");

        Reply byPerson = call("GET", "/v1/sis/me/children?size=1", token(school.tenant, UUID.randomUUID(), person,
                List.of(), List.of(), "sis.guardian.list_my_children"), null);
        assertThat(byPerson.body().path("data")).as("the guardian's person, signed in with another account")
                .hasSize(1);
        assertThat(byPerson.body().path("paging").path("next").asString()).contains("cursor=");
        assertThat(ids(call("GET", "/v1/sis/me/children", school.admin, null), "student"))
                .as("an administrator who is nobody's guardian").isEmpty();
        assertThat(ids(call("GET", "/v1/sis/me/children", token(UUID.randomUUID(), account, null, List.of(),
                List.of(), "sis.guardian.list_my_children"), null), "student")).as("another tenant").isEmpty();
    }

    // ---- sis.MarkIncidentActionTaken, sis.MarkIncidentSeen ----

    @Test
    void aGuardianAcknowledgesWhatTheSchoolSharedAboutTheirChild() throws Exception {
        School school = new School();
        UUID child = school.student("F-1", LocalDate.parse("2026-04-01"));
        UUID other = school.student("F-2", LocalDate.parse("2026-04-01"));
        UUID account = UUID.randomUUID();
        school.guardian(account, UUID.randomUUID(), child, "active");
        UUID shared = school.incident(child, true);
        UUID kept = school.incident(child, false);
        UUID notMine = school.incident(other, true);
        String parent = token(school.tenant, account, null, List.of(), List.of(),
                "sis.discipline_incident.mark_incident_seen");
        String incidents = "/v1/sis/discipline-incidents/";

        assertThat(call("POST", incidents + kept + ":markSeen", parent, "{}").status())
                .as("not shared with guardians").isEqualTo(404);
        assertThat(call("POST", incidents + notMine + ":markSeen", parent, "{}").status())
                .as("another family's child").isEqualTo(404);
        Reply seen = call("POST", incidents + shared + ":markSeen", parent, "{}");
        assertThat(seen.status()).as(seen.text()).isEqualTo(200);
        assertThat(seen.body().path("seenByGuardian").asBoolean()).isTrue();
        assertThat(call("POST", incidents + shared + ":markSeen", parent, "{}").status()).isEqualTo(409);
        assertThat(call("POST", incidents + kept + ":markSeen", school.admin, "{}").status())
                .as("staff: not shared").isEqualTo(409);

        Reply acted = call("POST", incidents + kept + ":markActionTaken", school.admin,
                "{\"response\":\"  Met the parents.  \"}");
        assertThat(acted.status()).as(acted.text()).isEqualTo(200);
        assertThat(acted.body().path("response").asString()).isEqualTo("Met the parents.");
        assertThat(call("POST", incidents + kept + ":markActionTaken", school.admin, "{}").status()).isEqualTo(409);
    }

    // ---- sis.ProposePromotions, sis.ApplyPromotions ----

    @Test
    void approvedPromotionsBecomeNextYearsPlacementsAllOrNothing() throws Exception {
        School school = new School();
        UUID promoted = school.student("G-1", LocalDate.parse("2025-06-01"));
        UUID detained = school.student("G-2", LocalDate.parse("2025-06-01"));
        UUID unplaced = UUID.randomUUID();
        school.sql("INSERT INTO sis.student (id, tenant_id, person_id, institution_id, row_version) "
                + "VALUES (:a, :tenantId, :b, :c, 1)", unplaced, UUID.randomUUID(), school.institution);
        UUID up = school.promotion(promoted, "approved", UUID.randomUUID());
        UUID stay = school.promotion(detained, "approved", school.grade);
        UUID proposed = school.promotion(school.student("G-3", LocalDate.parse("2025-06-01")), "proposed",
                UUID.randomUUID());
        UUID nowhere = school.promotion(unplaced, "approved", UUID.randomUUID());
        String apply = "/v1/sis/promotions:apply";

        assertThat(call("POST", "/v1/sis/promotions:propose", school.admin, "{\"fromClassSectionId\":\""
                + school.classSection + "\",\"toAcademicYearId\":\"" + school.nextYear + "\",\"toGradeLevelId\":\""
                + UUID.randomUUID() + "\"}").status()).as("academics is not running here").isEqualTo(503);
        assertThat(call("POST", apply, school.admin, "{\"promotionIds\":[]}").status()).isEqualTo(422);
        assertThat(call("POST", apply, school.admin, ids(up, proposed)).status()).as("not approved").isEqualTo(409);
        assertThat(call("POST", apply, school.admin, ids(up, nowhere)).status()).as("no open placement")
                .isEqualTo(409);
        assertThat(call("POST", apply, token(school.tenant, UUID.randomUUID(), null,
                List.of(UUID.randomUUID().toString()), List.of(), "sis.promotion.apply_promotions"), ids(up))
                .status()).as("outside the caller's institutions").isEqualTo(404);
        assertThat(school.one("SELECT count(*) FROM sis.student_enrollment WHERE tenant_id = :tenantId "
                + "AND student_id = :id", Integer.class, promoted)).as("a refusal applies nothing").isEqualTo(1);

        Reply applied = call("POST", apply, school.admin, ids(up, stay));

        assertThat(applied.status()).as(applied.text()).isEqualTo(202);
        assertThat(applied.body().path("status").asString()).isEqualTo("succeeded");
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        assertThat(school.one("SELECT enrollment_status || ' ' || effective_to FROM sis.student_enrollment "
                + "WHERE tenant_id = :tenantId AND student_id = :id AND effective_to IS NOT NULL", String.class,
                promoted)).isEqualTo("promoted " + today);
        assertThat(school.one("SELECT enrollment_status FROM sis.student_enrollment WHERE tenant_id = :tenantId "
                + "AND student_id = :id AND effective_to IS NOT NULL", String.class, detained)).isEqualTo("detained");
        assertThat(school.one("SELECT academic_year_id::text || ' ' || change_reason || ' ' || effective_from "
                + "|| ' ' || (previous_enrollment_id IS NOT NULL) FROM sis.student_enrollment WHERE tenant_id = "
                + ":tenantId AND student_id = :id AND effective_to IS NULL", String.class, promoted))
                .isEqualTo(school.nextYear + " promotion " + today + " true");
        assertThat(call("GET", "/v1/sis/promotions/" + up, school.admin, null).body().path("status").asString())
                .isEqualTo("applied");
        assertThat(call("POST", apply, school.admin, ids(up)).status()).as("applied once").isEqualTo(409);
    }

    // ---- sis.ApproveSubjectRegistration, sis.LoadCourseSubjects ----

    @Test
    void anApprovedRegistrationAddsItsSubjectsToTheCourseRegistration() throws Exception {
        School school = new School();
        UUID student = school.student("H-1", LocalDate.parse("2026-04-01"));
        UUID submitted = school.registration(student, "submitted", 2);
        UUID empty = school.registration(school.student("H-2", LocalDate.parse("2026-04-01")), "submitted", 0);
        UUID draft = school.registration(school.student("H-3", LocalDate.parse("2026-04-01")), "draft", 1);
        String registrations = "/v1/sis/subject-registrations/";

        assertThat(call("POST", registrations + empty + ":approve", school.admin, "{}").status())
                .as("lists no subjects").isEqualTo(409);
        assertThat(call("POST", registrations + draft + ":approve", school.admin, "{}").status())
                .as("not submitted").isEqualTo(409);
        assertThat(call("POST", registrations + submitted + ":approve", school.admin, "{\"etag\":\"\\\"9\\\"\"}")
                .status()).isEqualTo(412);
        Reply approved = call("POST", registrations + submitted + ":approve", school.admin, "{}");
        assertThat(approved.status()).as(approved.text()).isEqualTo(200);
        assertThat(approved.body().path("status").asString()).isEqualTo("approved");
        assertThat(school.one("SELECT count(*) FROM sis.program_enrollment_subject s JOIN sis.subject_registration r "
                + "ON r.tenant_id = :tenantId AND r.program_enrollment_id = s.program_enrollment_id "
                + "WHERE s.tenant_id = :tenantId AND r.id = :id", Integer.class, submitted)).isEqualTo(2);

        assertThat(call("POST", registrations + submitted + ":loadCourseSubjects", school.admin, "{}").status())
                .as("only a draft is filled").isEqualTo(409);
        assertThat(call("POST", registrations + draft + ":loadCourseSubjects", school.admin, "{}").status())
                .as("academics is not running here").isEqualTo(503);
    }

    private static String ids(UUID... promotions) {
        List<String> quoted = new ArrayList<>();
        for (UUID promotion : promotions) {
            quoted.add("\"" + promotion + "\"");
        }
        return "{\"promotionIds\":[" + String.join(",", quoted) + "]}";
    }

    private static List<String> ids(Reply reply, String object) {
        List<String> found = new ArrayList<>();
        reply.body().path("data").forEach(item -> found.add(item.path(object).path("id").asString()));
        return found;
    }

    private Reply call(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        request.header("Authorization", "Bearer " + token);
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    /** A token of the account (and person, when given) with the permissions in the institutions. */
    private static String token(UUID tenant, UUID user, UUID person, List<String> institutions,
            List<String> tenantWide, String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", tenantWide);
        claims.put("ins", institutions);
        if (person != null) {
            claims.put("pid", person.toString());
        }
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }
}
