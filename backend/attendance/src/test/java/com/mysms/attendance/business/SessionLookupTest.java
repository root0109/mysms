package com.mysms.attendance.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Timestamp;
import com.mysms.attendance.integration.OrgGrpcClient;
import com.mysms.attendance.integration.SisGrpcClient;
import com.mysms.attendance.integration.TimetableGrpcClient;
import com.mysms.common.v1.Pagination;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.testkit.GrpcRefusals;
import com.mysms.org.v1.GetInstitutionResponse;
import com.mysms.org.v1.Institution;
import com.mysms.sis.v1.ListClassRosterRequest;
import com.mysms.sis.v1.ListClassRosterResponse;
import com.mysms.sis.v1.ListStudentsRequest;
import com.mysms.sis.v1.ListStudentsResponse;
import com.mysms.sis.v1.Student;
import com.mysms.sis.v1.StudentEnrollment;
import com.mysms.timetable.v1.GetSessionResponse;
import com.mysms.timetable.v1.ListSessionStudentsResponse;
import com.mysms.timetable.v1.Session;
import com.mysms.timetable.v1.SessionStudent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

/**
 * The session lookup against stand-ins of the three gRPC clients: which students are expected, on which date, and how
 * the other modules' refusals become this operation's errors.
 */
class SessionLookupTest {

    private static final UUID SESSION = UUID.randomUUID();
    private static final UUID INSTITUTION = UUID.randomUUID();
    private static final UUID CLASS_SECTION = UUID.randomUUID();
    private static final UUID SUBJECT = UUID.randomUUID();
    private static final UUID STAFF = UUID.randomUUID();

    private final TimetableGrpcClient timetable = mock(TimetableGrpcClient.class);
    private final SisGrpcClient sis = mock(SisGrpcClient.class);
    private final OrgGrpcClient org = mock(OrgGrpcClient.class);
    private final SessionLookup lookup = new SessionLookup(timetable, sis, org);

    private static Session.Builder session(String startAt) {
        Instant start = Instant.parse(startAt);
        return Session.newBuilder().setId(SESSION.toString()).setInstitutionId(INSTITUTION.toString())
                .setStartAt(Timestamp.newBuilder().setSeconds(start.getEpochSecond()))
                .setClassSectionId(CLASS_SECTION.toString()).setSubjectId(SUBJECT.toString())
                .setStaffId(STAFF.toString()).setStatus(Session.Status.STATUS_CONFIRMED);
    }

    private void given(Session.Builder session, String timezone) {
        // doReturn: a stand-in already told to fail must not fail while it is being told otherwise
        doReturn(GetSessionResponse.newBuilder().setSession(session).build()).when(timetable).getSession(any());
        Institution.Builder institution = Institution.newBuilder().setId(INSTITUTION.toString());
        if (timezone != null) {
            institution.setTimezone(timezone);
        }
        doReturn(GetInstitutionResponse.newBuilder().setInstitution(institution).build()).when(org)
                .getInstitution(any());
    }

    private void givenSessionStudents(UUID... students) {
        ListSessionStudentsResponse.Builder list = ListSessionStudentsResponse.newBuilder()
                .setPagination(Pagination.newBuilder().setPage(1));
        for (UUID student : students) {
            list.addItems(SessionStudent.newBuilder().setStudentId(student.toString()));
        }
        when(timetable.listSessionStudents(any())).thenReturn(list.build());
    }

    private static StudentEnrollment enrollment(UUID student, LocalDate from, LocalDate to) {
        StudentEnrollment.Builder enrollment = StudentEnrollment.newBuilder().setStudentId(student.toString());
        if (from != null) {
            enrollment.setEffectiveFrom(from.toString());
        }
        if (to != null) {
            enrollment.setEffectiveTo(to.toString());
        }
        return enrollment.build();
    }

    private static ListClassRosterResponse roster(String nextCursor, StudentEnrollment... rows) {
        return ListClassRosterResponse.newBuilder().addAllItems(List.of(rows))
                .setPagination(Pagination.newBuilder().setNextCursor(nextCursor)).build();
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
        assertThat(plan.courseId()).as("not set on the session").isNull();
        verify(sis, never()).listClassRoster(any());
    }

    @Test
    void withoutSessionStudentsTheClassRosterOfTheDateIsExpected() {
        given(session("2026-03-02T03:30:00Z"), "Asia/Kolkata");
        givenSessionStudents();
        UUID placed = UUID.randomUUID();
        UUID open = UUID.randomUUID();
        UUID later = UUID.randomUUID();
        UUID ended = UUID.randomUUID();
        UUID onSecondPage = UUID.randomUUID();
        LocalDate day = LocalDate.parse("2026-03-02");
        when(sis.listClassRoster(any())).thenAnswer(call -> {
            ListClassRosterRequest request = call.getArgument(0);
            return request.getPageable().getPage() == 1
                    ? roster("next", enrollment(placed, day, day), enrollment(open, null, null),
                            enrollment(later, day.plusDays(1), null), enrollment(ended, null, day.minusDays(1)))
                    : roster("", enrollment(onSecondPage, null, null));
        });

        assertThat(lookup.plan(SESSION).studentIds()).containsExactly(placed, open, onSecondPage);
        ArgumentCaptor<ListClassRosterRequest> asked = ArgumentCaptor.forClass(ListClassRosterRequest.class);
        verify(sis, times(2)).listClassRoster(asked.capture());
        assertThat(asked.getAllValues().get(1).getPageable().getCursor()).isEqualTo("next");
        assertThat(asked.getAllValues().get(0).getClassSectionId()).isEqualTo(CLASS_SECTION.toString());
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
        when(timetable.getSession(any())).thenThrow(GrpcRefusals.refused("timetable", HttpStatus.NOT_FOUND,
                "NOT_FOUND"));
        assertThatThrownBy(() -> lookup.plan(SESSION)).isInstanceOfSatisfying(ValidationFailedException.class,
                e -> assertThat(e.errors()).singleElement().satisfies(error -> {
                    assertThat(error.field()).isEqualTo("timetableSessionId");
                    assertThat(error.code()).isEqualTo("NOT_FOUND");
                }));

        given(session("2026-03-02T03:30:00Z").setStatus(Session.Status.STATUS_CANCELLED), "UTC");
        assertThatThrownBy(() -> lookup.plan(SESSION)).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void aRefusalInAnotherModuleIsThisOperationsRefusalNamingThePermission() {
        when(timetable.getSession(any())).thenThrow(GrpcRefusals.refused("timetable", HttpStatus.FORBIDDEN,
                "PERMISSION_DENIED"));
        assertThatThrownBy(() -> lookup.plan(SESSION)).isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("timetable.session.read");

        given(session("2026-03-02T03:30:00Z"), "UTC");
        doThrow(GrpcRefusals.refused("org", HttpStatus.NOT_FOUND, "NOT_FOUND")).when(org).getInstitution(any());
        assertThatThrownBy(() -> lookup.plan(SESSION)).as("an institution the caller cannot see")
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void aFailingOrUnreachableServiceIsUnavailableWithoutItsDetails() {
        when(timetable.getSession(any())).thenThrow(GrpcRefusals.refused("timetable",
                HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL"));
        assertThatThrownBy(() -> lookup.plan(SESSION)).isInstanceOf(UnavailableException.class)
                .hasMessage("The timetable service could not answer.");

        doThrow(GrpcRefusals.unreachable("timetable")).when(timetable).getSession(any());
        assertThatThrownBy(() -> lookup.plan(SESSION)).isInstanceOf(UnavailableException.class)
                .hasMessage("The timetable service could not answer.");

        doReturn(GetSessionResponse.newBuilder().setSession(session("2026-03-02T03:30:00Z").clearStartAt()).build())
                .when(timetable).getSession(any());
        assertThatThrownBy(() -> lookup.plan(SESSION)).as("a session without its start")
                .isInstanceOf(UnavailableException.class);
    }

    private static Student student(UUID id, UUID institution) {
        return Student.newBuilder().setId(id.toString()).setInstitutionId(institution.toString()).build();
    }

    @Test
    void aNewStudentIsKnownFromTheClassRosterOfTheDateOrAsAStudentOfTheInstitution() {
        UUID classmate = UUID.randomUUID();
        UUID schoolmate = UUID.randomUUID();
        UUID elsewhere = UUID.randomUUID();
        UUID nobody = UUID.randomUUID();
        LocalDate day = LocalDate.parse("2026-03-02");
        when(sis.listClassRoster(any())).thenReturn(roster("", enrollment(classmate, null, null),
                enrollment(schoolmate, day.plusDays(1), null)));
        // sis answers only the students the caller can see: one of this institution, one of another
        when(sis.listStudents(any())).thenReturn(ListStudentsResponse.newBuilder()
                .addItems(student(schoolmate, INSTITUTION)).addItems(student(elsewhere, UUID.randomUUID())).build());

        assertThat(lookup.unknownStudents(INSTITUTION, CLASS_SECTION, day,
                List.of(classmate, schoolmate, elsewhere, nobody))).containsExactly(elsewhere, nobody);
        // the classmate was settled by the roster and is not asked for again
        ArgumentCaptor<ListStudentsRequest> asked = ArgumentCaptor.forClass(ListStudentsRequest.class);
        verify(sis).listStudents(asked.capture());
        assertThat(asked.getValue().getFilter())
                .isEqualTo("id = \"" + schoolmate + "\" OR id = \"" + elsewhere + "\" OR id = \"" + nobody + "\"");
        assertThat(asked.getValue().getPageable().getSize()).isEqualTo(SessionLookup.ID_CHUNK);

        assertThat(lookup.unknownStudents(INSTITUTION, CLASS_SECTION, day, List.of(classmate))).isEmpty();
        assertThat(lookup.unknownStudents(INSTITUTION, null, day, List.of())).isEmpty();
    }

    @Test
    void newStudentsAreCheckedFortyAtATimeAndWithoutTheRosterWhenItIsNotReadable() {
        when(sis.listClassRoster(any())).thenThrow(GrpcRefusals.refused("sis", HttpStatus.FORBIDDEN,
                "PERMISSION_DENIED"));
        when(sis.listStudents(any())).thenReturn(ListStudentsResponse.getDefaultInstance());
        List<UUID> many = Stream.generate(UUID::randomUUID).limit(45).toList();

        assertThat(lookup.unknownStudents(INSTITUTION, CLASS_SECTION, LocalDate.parse("2026-03-02"), many))
                .containsExactlyElementsOf(many);
        verify(sis, times(2)).listStudents(any());

        doThrow(GrpcRefusals.refused("sis", HttpStatus.FORBIDDEN, "PERMISSION_DENIED")).when(sis).listStudents(any());
        assertThatThrownBy(() -> lookup.unknownStudents(INSTITUTION, null, LocalDate.parse("2026-03-02"), many))
                .isInstanceOf(PermissionDeniedException.class).hasMessageContaining("sis.student.read");
    }
}
