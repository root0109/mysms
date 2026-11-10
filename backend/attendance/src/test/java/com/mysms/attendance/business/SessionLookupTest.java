package com.mysms.attendance.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.sdk.org.api.InstitutionApi;
import com.mysms.sdk.org.model.Institution;
import com.mysms.sdk.sis.api.StudentApi;
import com.mysms.sdk.sis.api.StudentEnrollmentApi;
import com.mysms.sdk.sis.model.Student;
import com.mysms.sdk.sis.model.StudentEnrollment;
import com.mysms.sdk.sis.model.PagingInfoStudentEnrollment;
import com.mysms.sdk.sis.model.PagingInfoStudent;
import com.mysms.sdk.timetable.api.SessionApi;
import com.mysms.sdk.timetable.model.Session;
import com.mysms.sdk.timetable.model.SessionStudent;
import com.mysms.sdk.timetable.model.PagingInfoSessionStudent;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * The session lookup against stand-ins of the three SDK clients (a unit test: the real services answer it in the
 * app module's component test): which students are expected, on which date, and how the other modules' refusals
 * become this operation's errors.
 */
class SessionLookupTest {

    private static final UUID SESSION = UUID.randomUUID();
    private static final UUID INSTITUTION = UUID.randomUUID();
    private static final UUID CLASS_SECTION = UUID.randomUUID();
    private static final UUID SUBJECT = UUID.randomUUID();
    private static final UUID STAFF = UUID.randomUUID();

    private final SessionApi sessions = mock(SessionApi.class);
    private final StudentEnrollmentApi enrollments = mock(StudentEnrollmentApi.class);
    private final StudentApi students = mock(StudentApi.class);
    private final InstitutionApi institutions = mock(InstitutionApi.class);
    private final SessionLookup lookup = new SessionLookup(sessions, enrollments, students, institutions);

    private Session session(String startAt) {
        Session session = new Session(INSTITUTION, OffsetDateTime.parse(startAt),
                OffsetDateTime.parse(startAt).plusMinutes(45), SUBJECT, STAFF);
        session.setClassSectionId(CLASS_SECTION);
        session.setStatus(Session.StatusEnum.CONFIRMED);
        return session;
    }

    private void given(Session session, String timezone) {
        // doReturn: a stand-in already told to fail must not fail while it is being told otherwise
        doReturn(ResponseEntity.ok(session)).when(sessions).getSession(SESSION, null);
        Institution institution = new Institution();
        institution.setTimezone(timezone);
        doReturn(ResponseEntity.ok(institution)).when(institutions).getInstitution(INSTITUTION, null);
    }

    private void givenSessionStudents(UUID... students) {
        PagingInfoSessionStudent list = new PagingInfoSessionStudent(List.of(students).stream().map(student -> {
            SessionStudent row = new SessionStudent();
            row.setStudentId(student);
            return row;
        }).toList(), new com.mysms.sdk.timetable.model.NavigationInfo(1, students.length, "/first"));
        when(sessions.listSessionStudents(eq(SESSION), eq(1), eq(SessionLookup.PAGE_SIZE), isNull(), isNull(),
                isNull(), isNull())).thenReturn(ResponseEntity.ok(list));
    }

    /** The navigation of roster page n, without a next page unless the test adds one. */
    private static com.mysms.sdk.sis.model.NavigationInfo rosterPage(int page) {
        return new com.mysms.sdk.sis.model.NavigationInfo(page, 1, "/v1/sis/class-sections/x/roster?page=1&size=500");
    }

    private static com.mysms.sdk.sis.model.NavigationInfo studentPage() {
        return new com.mysms.sdk.sis.model.NavigationInfo(1, 0, "/v1/sis/students?page=1&size=40");
    }

    private static StudentEnrollment enrollment(UUID student, LocalDate from, LocalDate to) {
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudentId(student);
        enrollment.setEffectiveFrom(from);
        enrollment.setEffectiveTo(to);
        return enrollment;
    }

    @Test
    void theSessionsOwnStudentsAreExpectedAndTheRosterIsNotRead() {
        given(session("2026-03-02T03:30:00Z"), "Asia/Kolkata");
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        givenSessionStudents(first, second, first);

        SessionLookup.SessionPlan plan = lookup.plan(SESSION);

        assertThat(plan.studentIds()).containsExactly(first, second);
        assertThat(plan.institutionId()).isEqualTo(INSTITUTION);
        assertThat(plan.classSectionId()).isEqualTo(CLASS_SECTION);
        assertThat(plan.subjectId()).isEqualTo(SUBJECT);
        assertThat(plan.staffId()).isEqualTo(STAFF);
        verify(enrollments, never()).listClassRoster(any(), any(), any(), any());
    }

    @Test
    void withoutSessionStudentsTheClassRosterOfTheDateIsExpected() {
        given(session("2026-03-02T03:30:00Z"), "Asia/Kolkata");
        givenSessionStudents();
        UUID placed = UUID.randomUUID();
        UUID open = UUID.randomUUID();
        UUID later = UUID.randomUUID();
        UUID ended = UUID.randomUUID();
        LocalDate day = LocalDate.parse("2026-03-02");
        when(enrollments.listClassRoster(CLASS_SECTION, 1, SessionLookup.PAGE_SIZE, null)).thenReturn(ResponseEntity.ok(
                new PagingInfoStudentEnrollment(List.of(enrollment(placed, day, day), enrollment(open, null, null),
                        enrollment(later, day.plusDays(1), null), enrollment(ended, null, day.minusDays(1))),
                        rosterPage(1).next("/v1/sis/class-sections/x/roster?page=2&size=500&cursor=next"))));
        UUID onSecondPage = UUID.randomUUID();
        when(enrollments.listClassRoster(CLASS_SECTION, 2, SessionLookup.PAGE_SIZE, "next")).thenReturn(
                ResponseEntity.ok(new PagingInfoStudentEnrollment(List.of(enrollment(onSecondPage, null, null)),
                        rosterPage(2))));

        assertThat(lookup.plan(SESSION).studentIds()).containsExactly(placed, open, onSecondPage);
    }

    @Test
    void theDateIsTheSessionsStartInItsInstitutionsTimeZone() {
        // 19:00 on 1 March in Los Angeles is already 2 March in UTC
        given(session("2026-03-02T03:00:00Z"), "America/Los_Angeles");
        givenSessionStudents(UUID.randomUUID());
        assertThat(lookup.plan(SESSION).date()).isEqualTo(LocalDate.parse("2026-03-01"));

        given(session("2026-03-02T03:00:00Z"), "Asia/Kolkata");
        assertThat(lookup.plan(SESSION).date()).isEqualTo(LocalDate.parse("2026-03-02"));

        given(session("2026-03-02T03:00:00Z"), "Not/AZone");
        assertThat(lookup.plan(SESSION).date()).as("an unusable zone means UTC").isEqualTo(LocalDate.parse("2026-03-02"));

        given(session("2026-03-02T03:00:00Z"), null);
        assertThat(lookup.plan(SESSION).date()).isEqualTo(LocalDate.parse("2026-03-02"));
    }

    @Test
    void anUnknownSessionIsAFieldErrorAndACancelledOneAConflict() {
        when(sessions.getSession(SESSION, null)).thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND,
                "Not Found", null, null, StandardCharsets.UTF_8));
        assertThatThrownBy(() -> lookup.plan(SESSION)).isInstanceOfSatisfying(ValidationFailedException.class,
                e -> assertThat(e.errors()).singleElement().satisfies(error -> {
                    assertThat(error.field()).isEqualTo("timetableSessionId");
                    assertThat(error.code()).isEqualTo("NOT_FOUND");
                }));

        Session cancelled = session("2026-03-02T03:30:00Z");
        cancelled.setStatus(Session.StatusEnum.CANCELLED);
        given(cancelled, "UTC");
        assertThatThrownBy(() -> lookup.plan(SESSION)).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void aRefusalInAnotherModuleIsThisOperationsRefusalNamingThePermission() {
        when(sessions.getSession(SESSION, null)).thenThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN,
                "Forbidden", null, null, StandardCharsets.UTF_8));
        assertThatThrownBy(() -> lookup.plan(SESSION)).isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("timetable.session.read");

        given(session("2026-03-02T03:30:00Z"), "UTC");
        doThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null,
                StandardCharsets.UTF_8)).when(institutions).getInstitution(INSTITUTION, null);
        assertThatThrownBy(() -> lookup.plan(SESSION)).as("an institution the caller cannot see")
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void aFailingOrUnconfiguredServiceIsUnavailableWithoutItsDetails() {
        when(sessions.getSession(SESSION, null)).thenThrow(HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "boom at 10.0.0.7", null, null, StandardCharsets.UTF_8));
        assertThatThrownBy(() -> lookup.plan(SESSION)).isInstanceOf(UnavailableException.class)
                .hasMessage("The timetable service could not answer.");

        doThrow(new ResourceAccessException("connect to 10.0.0.7 failed")).when(sessions).getSession(SESSION, null);
        assertThatThrownBy(() -> lookup.plan(SESSION)).isInstanceOf(UnavailableException.class)
                .hasMessage("The timetable service could not answer.");

        assertThatThrownBy(() -> SessionLookup.unconfigured().plan(SESSION))
                .isInstanceOf(UnavailableException.class).hasMessageContaining("not configured");
    }

    private static Student student(UUID id, UUID institution) {
        return new Student(UUID.randomUUID(), institution).id(id);
    }

    @Test
    void aNewStudentIsKnownFromTheClassRosterOfTheDateOrAsAStudentOfTheInstitution() {
        UUID classmate = UUID.randomUUID();
        UUID schoolmate = UUID.randomUUID();
        UUID elsewhere = UUID.randomUUID();
        UUID nobody = UUID.randomUUID();
        LocalDate day = LocalDate.parse("2026-03-02");
        when(enrollments.listClassRoster(CLASS_SECTION, 1, SessionLookup.PAGE_SIZE, null)).thenReturn(ResponseEntity.ok(
                new PagingInfoStudentEnrollment(List.of(enrollment(classmate, null, null),
                        enrollment(schoolmate, day.plusDays(1), null)), rosterPage(1))));
        // sis answers only the students the caller can see: one of this institution, one of another
        when(students.listStudents(eq(1), eq(SessionLookup.ID_CHUNK), isNull(), any(), isNull(), isNull())).thenReturn(
                ResponseEntity.ok(new PagingInfoStudent(List.of(student(schoolmate, INSTITUTION),
                        student(elsewhere, UUID.randomUUID())), studentPage())));

        assertThat(lookup.unknownStudents(INSTITUTION, CLASS_SECTION, day,
                List.of(classmate, schoolmate, elsewhere, nobody))).containsExactly(elsewhere, nobody);
        // the classmate was settled by the roster and is not asked for again
        verify(students).listStudents(1, SessionLookup.ID_CHUNK, null,
                "id = \"" + schoolmate + "\" OR id = \"" + elsewhere + "\" OR id = \"" + nobody + "\"", null, null);

        assertThat(lookup.unknownStudents(INSTITUTION, CLASS_SECTION, day, List.of(classmate))).isEmpty();
        assertThat(lookup.unknownStudents(INSTITUTION, null, day, List.of())).isEmpty();
    }

    @Test
    void newStudentsAreCheckedFortyAtATimeAndWithoutTheRosterWhenItIsNotReadable() {
        when(enrollments.listClassRoster(CLASS_SECTION, 1, SessionLookup.PAGE_SIZE, null)).thenThrow(
                HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", null, null,
                        StandardCharsets.UTF_8));
        when(students.listStudents(eq(1), eq(SessionLookup.ID_CHUNK), isNull(), any(), isNull(), isNull()))
                .thenReturn(ResponseEntity.ok(new PagingInfoStudent(List.of(), studentPage())));
        List<UUID> many = Stream.generate(UUID::randomUUID).limit(45).toList();

        assertThat(lookup.unknownStudents(INSTITUTION, CLASS_SECTION, LocalDate.parse("2026-03-02"), many))
                .containsExactlyElementsOf(many);
        verify(students, times(2)).listStudents(eq(1), eq(SessionLookup.ID_CHUNK), isNull(), any(),
                isNull(), isNull());

        doThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", null, null,
                StandardCharsets.UTF_8)).when(students).listStudents(any(), any(), any(), any(), any(), any());
        assertThatThrownBy(() -> lookup.unknownStudents(INSTITUTION, null, LocalDate.parse("2026-03-02"), many))
                .isInstanceOf(PermissionDeniedException.class).hasMessageContaining("sis.student.read");
        assertThatThrownBy(() -> SessionLookup.unconfigured().unknownStudents(INSTITUTION, null,
                LocalDate.parse("2026-03-02"), many)).isInstanceOf(UnavailableException.class);
    }
}
