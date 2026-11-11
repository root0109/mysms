package com.mysms.assessment.business;

import com.mysms.assessment.domain.ExamEntity;
import com.mysms.assessment.domain.ExamSessionEntity;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.paging.PagingUtil;
import com.mysms.sdk.sis.api.StudentEnrollmentApi;
import com.mysms.sdk.sis.api.SubjectRegistrationApi;
import com.mysms.sdk.sis.model.PagingInfoStudentEnrollment;
import com.mysms.sdk.sis.model.PagingInfoSubjectRegistration;
import com.mysms.sdk.sis.model.StudentEnrollment;
import com.mysms.sdk.sis.model.SubjectRegistration;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Reads the active K-12 roster or approved course registrations that SIS owns. */
public class ExamRosterLookup {

    private static final Logger LOG = LoggerFactory.getLogger(ExamRosterLookup.class);
    private static final int PAGE_SIZE = 500;
    private static final int MAX_PAGES = 20;
    private final StudentEnrollmentApi enrollments;
    private final SubjectRegistrationApi registrations;

    public ExamRosterLookup(StudentEnrollmentApi enrollments, SubjectRegistrationApi registrations) {
        this.enrollments = enrollments;
        this.registrations = registrations;
    }

    public static ExamRosterLookup unconfigured() {
        return new ExamRosterLookup(null, null);
    }

    public List<UUID> students(ExamEntity exam, ExamSessionEntity session) {
        if (enrollments == null || registrations == null) {
            throw new UnavailableException("The SIS service is not configured for exam roster lookup.");
        }
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
        Optional<String> cursor = Optional.empty();
        for (int page = 1; page <= MAX_PAGES; page++) {
            int pageNumber = page;
            String after = cursor.orElse(null);
            PagingInfoStudentEnrollment result = call("sis.student_enrollment.list_class_roster",
                    () -> enrollments.listClassRoster(classSectionId, pageNumber, PAGE_SIZE, after))
                    .orElseThrow(() -> new ValidationFailedException(List.of(new FieldError("classSectionId",
                            "class section does not exist or is not visible", "NOT_FOUND"))));
            for (StudentEnrollment enrollment : result.getData()) {
                LocalDate from = enrollment.getEffectiveFrom();
                LocalDate until = enrollment.getEffectiveTo();
                if ((from == null || !from.isAfter(examDate)) && (until == null || !until.isBefore(examDate))) {
                    students.add(enrollment.getStudentId());
                }
            }
            cursor = PagingUtil.cursorOf(result.getPaging().getNext());
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
        Optional<String> cursor = Optional.empty();
        for (int page = 1; page <= MAX_PAGES; page++) {
            int pageNumber = page;
            String after = cursor.orElse(null);
            PagingInfoSubjectRegistration result = call("sis.subject_registration.read",
                    () -> registrations.listSubjectRegistrations(pageNumber, PAGE_SIZE, after,
                            filter.toString(), null, "studentId,status,courseId,batchId,academicYearId"))
                    .orElseThrow(() -> new UnavailableException("The SIS service did not list course registrations."));
            for (SubjectRegistration registration : result.getData()) {
                if (registration.getStudentId() != null) {
                    students.add(registration.getStudentId());
                }
            }
            cursor = PagingUtil.cursorOf(result.getPaging().getNext());
            if (cursor.isEmpty()) {
                return List.copyOf(students);
            }
        }
        throw new InvalidStateException("The course roster exceeds the 10,000-student exam registration limit.");
    }

    private static <T> Optional<T> call(String permission, Supplier<ResponseEntity<T>> request) {
        try {
            return Optional.ofNullable(request.get().getBody());
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 403) {
                throw new PermissionDeniedException("Populating exam results also needs " + permission + ".");
            }
            if (status.value() == 404) {
                return Optional.empty();
            }
            LOG.warn("SIS answered {} to exam roster lookup", status.value());
            throw new UnavailableException("The SIS service could not provide the exam roster.");
        } catch (RestClientException e) {
            LOG.warn("SIS exam roster lookup failed", e);
            throw new UnavailableException("The SIS service could not provide the exam roster.");
        }
    }
}
