package com.mysms.academics.assessment.business;

import com.mysms.academics.assessment.domain.ExamEntity;
import com.mysms.academics.assessment.domain.ExamSessionEntity;
import com.mysms.academics.assessment.integration.SisGrpcClient;
import com.mysms.common.v1.PageableRequest;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.sis.v1.ListClassRosterRequest;
import com.mysms.sis.v1.ListClassRosterResponse;
import com.mysms.sis.v1.ListSubjectRegistrationsRequest;
import com.mysms.sis.v1.ListSubjectRegistrationsResponse;
import com.mysms.sis.v1.StudentEnrollment;
import com.mysms.sis.v1.SubjectRegistration;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Reads the active K-12 roster or approved course registrations that SIS owns, with the caller's identity. */
@Component
public class ExamRosterLookup {

    private static final String SERVICE = "sis";
    private static final String PURPOSE = "Populating exam results";
    private static final int PAGE_SIZE = 500;
    private static final int MAX_PAGES = 20;
    private final SisGrpcClient sis;

    public ExamRosterLookup(SisGrpcClient sis) {
        this.sis = sis;
    }

    public List<UUID> students(ExamEntity exam, ExamSessionEntity session) {
        if (exam.getClassSectionId() != null) {
            return classRoster(exam.getClassSectionId(), exam.getStartAt().toLocalDate());
        }
        if (session.getCourseId() == null) {
            throw new ValidationFailedException(List.of(new FieldError("classSectionId",
                    "exam needs a class section or course registration roster", "REQUIRED")));
        }
        return courseRoster(session);
    }

    private List<UUID> classRoster(UUID classSectionId, LocalDate examDate) {
        Set<UUID> students = new LinkedHashSet<>();
        String cursor = "";
        for (int page = 1; page <= MAX_PAGES; page++) {
            PageableRequest pageable = page(page, cursor);
            ListClassRosterResponse result = Downstream.required(SERVICE, "sis.student_enrollment.list_class_roster",
                    PURPOSE, () -> sis.listClassRoster(ListClassRosterRequest.newBuilder()
                            .setClassSectionId(classSectionId.toString()).setPageable(pageable).build()))
                    .orElseThrow(() -> new ValidationFailedException(List.of(new FieldError("classSectionId",
                            "class section does not exist or is not visible", "NOT_FOUND"))));
            for (StudentEnrollment enrollment : result.getItemsList()) {
                LocalDate from = enrollment.hasEffectiveFrom() ? LocalDate.parse(enrollment.getEffectiveFrom()) : null;
                LocalDate until = enrollment.hasEffectiveTo() ? LocalDate.parse(enrollment.getEffectiveTo()) : null;
                if ((from == null || !from.isAfter(examDate)) && (until == null || !until.isBefore(examDate))) {
                    students.add(UUID.fromString(enrollment.getStudentId()));
                }
            }
            cursor = result.getPagination().getNextCursor();
            if (cursor.isEmpty()) {
                return List.copyOf(students);
            }
        }
        throw new InvalidStateException("The class roster exceeds the 10,000-student exam registration limit.");
    }

    private List<UUID> courseRoster(ExamSessionEntity session) {
        StringBuilder filter = new StringBuilder("courseId = \"").append(session.getCourseId()).append('"');
        if (session.getBatchId() != null) {
            filter.append(" AND batchId = \"").append(session.getBatchId()).append('"');
        }
        if (session.getAcademicYearId() != null) {
            filter.append(" AND academicYearId = \"").append(session.getAcademicYearId()).append('"');
        }
        filter.append(" AND status = \"approved\"");
        Set<UUID> students = new LinkedHashSet<>();
        String cursor = "";
        for (int page = 1; page <= MAX_PAGES; page++) {
            PageableRequest pageable = page(page, cursor);
            ListSubjectRegistrationsResponse result = Downstream.required(SERVICE, "sis.subject_registration.read",
                    PURPOSE, () -> sis.listSubjectRegistrations(ListSubjectRegistrationsRequest.newBuilder()
                            .setFilter(filter.toString()).setPageable(pageable).build()))
                    .orElseThrow(() -> new UnavailableException("The sis service did not list course registrations."));
            for (SubjectRegistration registration : result.getItemsList()) {
                if (!registration.getStudentId().isEmpty()) {
                    students.add(UUID.fromString(registration.getStudentId()));
                }
            }
            cursor = result.getPagination().getNextCursor();
            if (cursor.isEmpty()) {
                return List.copyOf(students);
            }
        }
        throw new InvalidStateException("The course roster exceeds the 10,000-student exam registration limit.");
    }

    /** One page of a sis list: the first by number, the next ones by the cursor the previous page gave. */
    private static PageableRequest page(int number, String cursor) {
        return PageableRequest.newBuilder().setPage(number).setSize(PAGE_SIZE).setCursor(cursor).build();
    }
}
