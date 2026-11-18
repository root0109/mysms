package com.mysms.finance.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mysms.academics.v1.ClassSection;
import com.mysms.academics.v1.Course;
import com.mysms.academics.v1.ListClassSectionsResponse;
import com.mysms.academics.v1.ListCoursesResponse;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.testkit.GrpcRefusals;
import com.mysms.finance.integration.AcademicsGrpcClient;
import com.mysms.finance.integration.OrgGrpcClient;
import com.mysms.finance.integration.SisGrpcClient;
import com.mysms.finance.model.DueTarget;
import com.mysms.org.v1.AcademicYear;
import com.mysms.org.v1.GetAcademicYearRequest;
import com.mysms.org.v1.GetAcademicYearResponse;
import com.mysms.sis.v1.GetProgramEnrollmentRequest;
import com.mysms.sis.v1.GetProgramEnrollmentResponse;
import com.mysms.sis.v1.ListClassRosterRequest;
import com.mysms.sis.v1.ListClassRosterResponse;
import com.mysms.sis.v1.ListProgramEnrollmentsResponse;
import com.mysms.sis.v1.ProgramEnrollment;
import com.mysms.sis.v1.StudentEnrollment;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Which students a fee run charges, as sis, academics and org answer (mocks stand in for the three clients). */
class RosterLookupTest {

    private final UUID year = UUID.randomUUID();
    private final UUID institution = UUID.randomUUID();
    private SisGrpcClient sis;
    private AcademicsGrpcClient academics;
    private OrgGrpcClient org;
    private RosterLookup rosters;

    @BeforeEach
    void services() {
        sis = mock(SisGrpcClient.class);
        academics = mock(AcademicsGrpcClient.class);
        org = mock(OrgGrpcClient.class);
        rosters = new RosterLookup(sis, academics, org);
    }

    @Test
    void aClassRosterIsItsPlacedStudentsOfTheYear() {
        UUID section = UUID.randomUUID();
        StudentEnrollment placed = enrollment(UUID.randomUUID(), year,
                StudentEnrollment.EnrollmentStatus.ENROLLMENT_STATUS_ENROLLED);
        StudentEnrollment left = enrollment(UUID.randomUUID(), year,
                StudentEnrollment.EnrollmentStatus.ENROLLMENT_STATUS_LEFT);
        StudentEnrollment lastYear = enrollment(UUID.randomUUID(), UUID.randomUUID(),
                StudentEnrollment.EnrollmentStatus.ENROLLMENT_STATUS_UNSPECIFIED);
        when(sis.listClassRoster(roster(section))).thenReturn(ListClassRosterResponse.newBuilder()
                .addItems(placed).addItems(left).addItems(lastYear).build());
        assertThat(rosters.classRoster(section, year)).containsExactly(new DueTarget(
                UUID.fromString(placed.getStudentId()), UUID.fromString(placed.getId()), null));

        UUID unknown = UUID.randomUUID();
        when(sis.listClassRoster(roster(unknown))).thenThrow(GrpcRefusals.refused("sis", HttpStatus.NOT_FOUND,
                "NOT_FOUND"));
        assertThatThrownBy(() -> rosters.classRoster(unknown, year)).isInstanceOf(ValidationFailedException.class);
        when(sis.listClassRoster(roster(section))).thenThrow(GrpcRefusals.unreachable("sis"));
        assertThatThrownBy(() -> rosters.classRoster(section, year)).isInstanceOf(UnavailableException.class);
    }

    @Test
    void namedRegistrationsMustBeOfTheYear() {
        UUID own = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        when(sis.getProgramEnrollment(get(own))).thenReturn(GetProgramEnrollmentResponse.newBuilder()
                .setProgramEnrollment(registration(own, year)).build());
        when(sis.getProgramEnrollment(get(other))).thenReturn(GetProgramEnrollmentResponse.newBuilder()
                .setProgramEnrollment(registration(other, UUID.randomUUID())).build());
        when(sis.getProgramEnrollment(get(missing))).thenThrow(GrpcRefusals.refused("sis", HttpStatus.NOT_FOUND,
                "NOT_FOUND"));
        assertThat(rosters.registrations(List.of(own), year)).hasSize(1);
        assertThatThrownBy(() -> rosters.registrations(List.of(own, other, missing), year))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void aStructureCoversTheClassesOfItsGradeLevelOrTheCoursesOfItsProgram() {
        UUID section = UUID.randomUUID();
        when(academics.listClassSections(any())).thenReturn(ListClassSectionsResponse.newBuilder()
                .addItems(ClassSection.newBuilder().setId(section.toString())).build());
        StudentEnrollment placed = enrollment(UUID.randomUUID(), year,
                StudentEnrollment.EnrollmentStatus.ENROLLMENT_STATUS_UNSPECIFIED);
        when(sis.listClassRoster(roster(section))).thenReturn(ListClassRosterResponse.newBuilder().addItems(placed)
                .build());
        assertThat(rosters.structureRoster(institution, year, UUID.randomUUID(), null)).hasSize(1);

        when(academics.listCourses(any())).thenReturn(ListCoursesResponse.newBuilder()
                .addItems(Course.newBuilder().setId(UUID.randomUUID().toString())).build());
        ProgramEnrollment running = registration(UUID.randomUUID(), year);
        when(sis.listProgramEnrollments(any())).thenReturn(ListProgramEnrollmentsResponse.newBuilder()
                .addItems(running).build());
        assertThat(rosters.structureRoster(institution, year, null, UUID.randomUUID())).containsExactly(
                new DueTarget(UUID.fromString(running.getStudentId()), null, UUID.fromString(running.getId())));
    }

    @Test
    void theScheduleOfAStructureStartsOnTheYearsFirstDay() {
        when(org.getAcademicYear(year(year))).thenReturn(GetAcademicYearResponse.newBuilder()
                .setAcademicYear(AcademicYear.newBuilder().setStartDate("2026-04-01")).build());
        assertThat(rosters.yearStart(year)).isEqualTo(LocalDate.parse("2026-04-01"));
        UUID unknown = UUID.randomUUID();
        when(org.getAcademicYear(year(unknown))).thenThrow(GrpcRefusals.refused("org", HttpStatus.NOT_FOUND,
                "NOT_FOUND"));
        assertThatThrownBy(() -> rosters.yearStart(unknown)).isInstanceOf(ValidationFailedException.class);
        UUID undated = UUID.randomUUID();
        when(org.getAcademicYear(year(undated))).thenReturn(GetAcademicYearResponse.getDefaultInstance());
        assertThatThrownBy(() -> rosters.yearStart(undated)).isInstanceOf(UnavailableException.class);
    }

    private static ListClassRosterRequest roster(UUID section) {
        return ListClassRosterRequest.newBuilder().setClassSectionId(section.toString())
                .setPageable(com.mysms.common.v1.PageableRequest.newBuilder().setPage(1).setSize(500)).build();
    }

    private static GetProgramEnrollmentRequest get(UUID id) {
        return GetProgramEnrollmentRequest.newBuilder().setId(id.toString()).build();
    }

    private static GetAcademicYearRequest year(UUID id) {
        return GetAcademicYearRequest.newBuilder().setId(id.toString()).build();
    }

    private static StudentEnrollment enrollment(UUID student, UUID year, StudentEnrollment.EnrollmentStatus status) {
        return StudentEnrollment.newBuilder().setId(UUID.randomUUID().toString()).setStudentId(student.toString())
                .setAcademicYearId(year.toString()).setEnrollmentStatus(status).build();
    }

    private static ProgramEnrollment registration(UUID id, UUID year) {
        return ProgramEnrollment.newBuilder().setId(id.toString()).setStudentId(UUID.randomUUID().toString())
                .setAcademicYearId(year.toString()).build();
    }
}
