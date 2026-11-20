package com.mysms.finance.business;

import com.mysms.academics.v1.ClassSection;
import com.mysms.academics.v1.Course;
import com.mysms.academics.v1.ListClassSectionsRequest;
import com.mysms.academics.v1.ListClassSectionsResponse;
import com.mysms.academics.v1.ListCoursesRequest;
import com.mysms.academics.v1.ListCoursesResponse;
import com.mysms.common.v1.PageableRequest;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.finance.integration.AcademicsGrpcClient;
import com.mysms.finance.integration.OrgGrpcClient;
import com.mysms.finance.integration.SisGrpcClient;
import com.mysms.finance.model.DueTarget;
import com.mysms.org.v1.AcademicYear;
import com.mysms.org.v1.GetAcademicYearRequest;
import com.mysms.sis.v1.ListClassRosterRequest;
import com.mysms.sis.v1.ListClassRosterResponse;
import com.mysms.sis.v1.ListProgramEnrollmentsRequest;
import com.mysms.sis.v1.ListProgramEnrollmentsResponse;
import com.mysms.sis.v1.ProgramEnrollment;
import com.mysms.sis.v1.StudentEnrollment;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Which students a fee run charges, read from the modules that own them with the caller's identity (an Adapter over the
 * sis, academics and org gRPC clients): a class section's roster, named course registrations, and for a fee structure
 * every student placed in its class (grade level) or registered on its program's courses in its academic year, with
 * the year's first day as the schedule start. A module that cannot answer is 503; nothing is generated from a partial
 * roster.
 */
@Component
public class RosterLookup {

    private static final String SIS = "sis";
    private static final String ACADEMICS = "academics";
    private static final String ORG = "org";
    private static final String PURPOSE = "Generating fee dues";
    private static final int PAGE_SIZE = 500;
    private static final int MAX_PAGES = 20;
    /**
     * Ids one by-id list names: 40 {@code id = "…"} terms stay inside the filter's length and term limits
     * (FilterParser), as the other modules' by-id lookups do.
     */
    static final int ID_CHUNK = 40;
    /** Courses one registrations list names: 30 {@code courseId = "…"} terms plus the year and status terms. */
    static final int COURSE_CHUNK = 30;

    private final SisGrpcClient sis;
    private final AcademicsGrpcClient academics;
    private final OrgGrpcClient org;

    public RosterLookup(SisGrpcClient sis, AcademicsGrpcClient academics, OrgGrpcClient org) {
        this.sis = sis;
        this.academics = academics;
        this.org = org;
    }

    /** The students placed in the class section in the academic year (enrolled or detained there). */
    public List<DueTarget> classRoster(UUID classSectionId, UUID academicYearId) {
        Map<UUID, DueTarget> students = new LinkedHashMap<>();
        String cursor = "";
        for (int page = 1; page <= MAX_PAGES; page++) {
            PageableRequest pageable = page(page, cursor);
            ListClassRosterResponse result = Downstream.required(SIS, "sis.student_enrollment.list_class_roster",
                    PURPOSE, () -> sis.listClassRoster(ListClassRosterRequest.newBuilder()
                            .setClassSectionId(classSectionId.toString()).setPageable(pageable).build()))
                    .orElseThrow(() -> new ValidationFailedException(List.of(new FieldError("classSectionId",
                            "class section does not exist or is not visible", "NOT_FOUND"))));
            for (StudentEnrollment enrollment : result.getItemsList()) {
                if (placed(enrollment, academicYearId)) {
                    UUID student = UUID.fromString(enrollment.getStudentId());
                    students.putIfAbsent(student, new DueTarget(student, UUID.fromString(enrollment.getId()), null));
                }
            }
            cursor = result.getPagination().getNextCursor();
            if (cursor.isEmpty()) {
                return List.copyOf(students.values());
            }
        }
        throw new InvalidStateException("The class roster has more students than one fee run charges.");
    }

    /**
     * The named course registrations of the academic year; one that sis does not show, or of another year, is 422.
     * They are read {@value #ID_CHUNK} a call (sis.ListProgramEnrollments by id), not one call per registration.
     */
    public List<DueTarget> registrations(List<UUID> programEnrollmentIds, UUID academicYearId) {
        Map<UUID, ProgramEnrollment> shown = programEnrollments(programEnrollmentIds);
        List<DueTarget> targets = new ArrayList<>();
        List<FieldError> errors = new ArrayList<>();
        for (int i = 0; i < programEnrollmentIds.size(); i++) {
            UUID id = programEnrollmentIds.get(i);
            ProgramEnrollment found = shown.get(id);
            if (found == null || found.getStudentId().isEmpty()) {
                errors.add(new FieldError("programEnrollmentIds[" + i + "]", "registration does not exist", "NOT_FOUND"));
            } else if (!academicYearId.toString().equals(found.getAcademicYearId())) {
                errors.add(new FieldError("programEnrollmentIds[" + i + "]", "registration is of another academic year",
                        "OTHER_YEAR"));
            } else {
                targets.add(new DueTarget(UUID.fromString(found.getStudentId()), null, id));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        return targets;
    }

    /**
     * Every student a fee structure covers: placed in a class of its grade level (or of any grade level when it names
     * neither a grade level nor a program) of its institution in its year, or registered on a course of its program
     * in its year (running registrations only).
     */
    public List<DueTarget> structureRoster(UUID institutionId, UUID academicYearId, UUID gradeLevelId,
            UUID programId) {
        if (programId != null) {
            return programRoster(institutionId, academicYearId, programId);
        }
        StringBuilder filter = new StringBuilder("academicYearId = \"").append(academicYearId).append('"')
                .append(" AND institutionId = \"").append(institutionId).append('"');
        if (gradeLevelId != null) {
            filter.append(" AND gradeLevelId = \"").append(gradeLevelId).append('"');
        }
        Map<UUID, DueTarget> students = new LinkedHashMap<>();
        String cursor = "";
        for (int page = 1; page <= MAX_PAGES; page++) {
            PageableRequest pageable = page(page, cursor);
            ListClassSectionsResponse result = Downstream.required(ACADEMICS, "academics.class_section.read", PURPOSE,
                    () -> academics.listClassSections(ListClassSectionsRequest.newBuilder()
                            .setFilter(filter.toString()).setPageable(pageable).build()))
                    .orElseThrow(() -> new UnavailableException("The academics service did not list class sections."));
            for (ClassSection section : result.getItemsList()) {
                for (DueTarget target : classRoster(UUID.fromString(section.getId()), academicYearId)) {
                    students.putIfAbsent(target.studentId(), target);
                }
            }
            cursor = result.getPagination().getNextCursor();
            if (cursor.isEmpty()) {
                return List.copyOf(students.values());
            }
        }
        throw new InvalidStateException("The fee structure covers more class sections than one fee run charges.");
    }

    /** The first day of the academic year, the start of a published structure's schedule. */
    public LocalDate yearStart(UUID academicYearId) {
        AcademicYear year = Downstream.required(ORG, "org.academic_year.read", PURPOSE,
                () -> org.getAcademicYear(GetAcademicYearRequest.newBuilder().setId(academicYearId.toString())
                        .build()).getAcademicYear())
                .orElseThrow(() -> new ValidationFailedException(List.of(new FieldError("academicYearId",
                        "academic year does not exist", "NOT_FOUND"))));
        if (year.getStartDate().isEmpty()) {
            throw new UnavailableException("The org service did not give the academic year's start.");
        }
        return LocalDate.parse(year.getStartDate());
    }

    private List<DueTarget> programRoster(UUID institutionId, UUID academicYearId, UUID programId) {
        String courseFilter = "programId = \"" + programId + "\" AND institutionId = \"" + institutionId + "\"";
        Map<UUID, DueTarget> students = new LinkedHashMap<>();
        String cursor = "";
        for (int page = 1; page <= MAX_PAGES; page++) {
            PageableRequest pageable = page(page, cursor);
            ListCoursesResponse result = Downstream.required(ACADEMICS, "academics.course.read", PURPOSE,
                    () -> academics.listCourses(ListCoursesRequest.newBuilder().setFilter(courseFilter)
                            .setPageable(pageable).build()))
                    .orElseThrow(() -> new UnavailableException("The academics service did not list courses."));
            List<UUID> courses = result.getItemsList().stream().map(Course::getId).map(UUID::fromString).toList();
            for (int start = 0; start < courses.size(); start += COURSE_CHUNK) {
                List<UUID> chunk = courses.subList(start, Math.min(start + COURSE_CHUNK, courses.size()));
                for (DueTarget target : courseRegistrations(chunk, academicYearId)) {
                    students.putIfAbsent(target.studentId(), target);
                }
            }
            cursor = result.getPagination().getNextCursor();
            if (cursor.isEmpty()) {
                return List.copyOf(students.values());
            }
        }
        throw new InvalidStateException("The program has more courses than one fee run charges.");
    }

    /**
     * The running registrations of the year on any of the courses, one list for the whole chunk of courses rather
     * than one per course; each course keeps the {@value #MAX_PAGES}-page allowance it had on its own.
     */
    private List<DueTarget> courseRegistrations(List<UUID> courseIds, UUID academicYearId) {
        String filter = "(" + anyOf("courseId", courseIds) + ") AND academicYearId = \"" + academicYearId
                + "\" AND status = \"running\"";
        List<DueTarget> students = new ArrayList<>();
        String cursor = "";
        for (int page = 1; page <= MAX_PAGES * courseIds.size(); page++) {
            PageableRequest pageable = page(page, cursor);
            ListProgramEnrollmentsResponse result = Downstream.required(SIS, "sis.program_enrollment.read", PURPOSE,
                    () -> sis.listProgramEnrollments(ListProgramEnrollmentsRequest.newBuilder().setFilter(filter)
                            .setPageable(pageable).build()))
                    .orElseThrow(() -> new UnavailableException("The sis service did not list course registrations."));
            for (ProgramEnrollment registration : result.getItemsList()) {
                if (!registration.getStudentId().isEmpty()) {
                    students.add(new DueTarget(UUID.fromString(registration.getStudentId()), null,
                            UUID.fromString(registration.getId())));
                }
            }
            cursor = result.getPagination().getNextCursor();
            if (cursor.isEmpty()) {
                return students;
            }
        }
        throw new InvalidStateException("The course has more registrations than one fee run charges.");
    }

    /** The registrations sis shows this caller among the ids, {@value #ID_CHUNK} ids a call: id → registration. */
    private Map<UUID, ProgramEnrollment> programEnrollments(List<UUID> ids) {
        List<UUID> distinct = ids.stream().distinct().toList();
        Map<UUID, ProgramEnrollment> shown = new HashMap<>();
        for (int start = 0; start < distinct.size(); start += ID_CHUNK) {
            String filter = anyOf("id", distinct.subList(start, Math.min(start + ID_CHUNK, distinct.size())));
            PageableRequest pageable = PageableRequest.newBuilder().setPage(1).setSize(ID_CHUNK).build();
            ListProgramEnrollmentsResponse result = Downstream.required(SIS, "sis.program_enrollment.read", PURPOSE,
                    () -> sis.listProgramEnrollments(ListProgramEnrollmentsRequest.newBuilder().setFilter(filter)
                            .setPageable(pageable).build()))
                    .orElseThrow(() -> new UnavailableException("The sis service did not list course registrations."));
            for (ProgramEnrollment registration : result.getItemsList()) {
                shown.put(UUID.fromString(registration.getId()), registration);
            }
        }
        return shown;
    }

    /** A filter matching any of the ids on the field: {@code field = "a" OR field = "b" …}. */
    static String anyOf(String field, List<UUID> ids) {
        return ids.stream().map(id -> field + " = \"" + id + "\"").collect(Collectors.joining(" OR "));
    }

    /** One page of a downstream list: the first by number, the next ones by the cursor the previous page gave. */
    private static PageableRequest page(int number, String cursor) {
        return PageableRequest.newBuilder().setPage(number).setSize(PAGE_SIZE).setCursor(cursor).build();
    }

    private static boolean placed(StudentEnrollment enrollment, UUID academicYearId) {
        StudentEnrollment.EnrollmentStatus status = enrollment.getEnrollmentStatus();
        return !enrollment.getStudentId().isEmpty() && academicYearId.toString().equals(enrollment.getAcademicYearId())
                && (status == StudentEnrollment.EnrollmentStatus.ENROLLMENT_STATUS_UNSPECIFIED
                        || status == StudentEnrollment.EnrollmentStatus.ENROLLMENT_STATUS_ENROLLED
                        || status == StudentEnrollment.EnrollmentStatus.ENROLLMENT_STATUS_DETAINED);
    }
}
