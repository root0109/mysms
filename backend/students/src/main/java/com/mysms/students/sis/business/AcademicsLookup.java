package com.mysms.students.sis.business;

import com.mysms.academics.v1.ClassSection;
import com.mysms.academics.v1.CourseSubject;
import com.mysms.academics.v1.GetClassSectionRequest;
import com.mysms.academics.v1.GetGradeLevelRequest;
import com.mysms.academics.v1.GetSectionRequest;
import com.mysms.academics.v1.GradeLevel;
import com.mysms.academics.v1.ListClassSectionsRequest;
import com.mysms.academics.v1.ListClassSectionsResponse;
import com.mysms.academics.v1.ListCourseSubjectsRequest;
import com.mysms.academics.v1.ListCourseSubjectsResponse;
import com.mysms.academics.v1.Section;
import com.mysms.common.v1.PageableRequest;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.UnavailableException;
import com.mysms.org.v1.GetAcademicYearRequest;
import com.mysms.students.sis.integration.AcademicsGrpcClient;
import com.mysms.students.sis.integration.OrgGrpcClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Gateway to what academics owns about classes and courses (an Adapter over its gRPC clients, so the sis workflows
 * never see the transport): the class section a promotion starts from and the one it leads to, the label of a class for a
 * child card, and the subjects of a course for a subject registration — and, from org, the academic year a class
 * belongs to, which a promotion's target names. Every call carries the caller's own identity, so the caller sees
 * through this gateway exactly what they may read in academics and org.
 */
@Component
public class AcademicsLookup {

    /** Page size of the course-subject list; the largest the API serves. */
    static final int PAGE_SIZE = 500;
    /** Pages read before the lookup gives up: far beyond the subjects of any course. */
    static final int MAX_PAGES = 4;

    /** Where a class section sits: its institution, year, grade and section. */
    public record ClassPlace(UUID classSectionId, UUID institutionId, UUID academicYearId, UUID gradeLevelId,
                             UUID sectionId) {
    }

    /** A subject of a course's plan: the plan row, the subject, and whether the plan lists it as an elective. */
    public record PlannedSubject(UUID courseSubjectId, UUID subjectId, boolean elective) {
    }

    private final AcademicsGrpcClient academics;
    private final OrgGrpcClient org;

    public AcademicsLookup(AcademicsGrpcClient academics, OrgGrpcClient org) {
        this.academics = academics;
        this.org = org;
    }

    /**
     * Where a promotion leads must be the institution's own: the academic year (org), the grade and, when named, the
     * section (academics). Each that does not exist for this caller, or belongs to another institution, is a field
     * error of the request (the caller's missing read permission is 403, an unavailable service 503).
     */
    public List<FieldError> targetErrors(UUID institutionId, UUID academicYearId, UUID gradeLevelId,
            UUID sectionId, String purpose) {
        String institution = institutionId.toString();
        List<FieldError> errors = new ArrayList<>();
        if (!Downstream.required("org", "org.academic_year.read", purpose,
                () -> org.getAcademicYear(GetAcademicYearRequest.newBuilder().setId(academicYearId.toString())
                        .build()).getAcademicYear())
                .map(year -> institution.equals(year.getInstitutionId())).orElse(false)) {
            errors.add(new FieldError("toAcademicYearId", "academic year does not exist in the class's institution",
                    "NOT_FOUND"));
        }
        if (!gradeLevel(gradeLevelId, purpose).map(grade -> institution.equals(grade.getInstitutionId()))
                .orElse(false)) {
            errors.add(new FieldError("toGradeLevelId", "grade level does not exist in the class's institution",
                    "NOT_FOUND"));
        }
        if (sectionId != null && !Downstream.required("academics", "academics.section.read", purpose,
                () -> academics.getSection(GetSectionRequest.newBuilder().setId(sectionId.toString()).build())
                        .getSection())
                .map(section -> institution.equals(section.getInstitutionId())).orElse(false)) {
            errors.add(new FieldError("toSectionId", "section does not exist in the class's institution",
                    "NOT_FOUND"));
        }
        return errors;
    }

    /** The class section, or empty when academics has none with the id for this caller (403 / 503 as they are). */
    public Optional<ClassPlace> classSection(UUID classSectionId, String purpose) {
        return Downstream.required("academics", "academics.class_section.read", purpose,
                () -> academics.getClassSection(classSectionRequest(classSectionId)).getClassSection())
                .map(AcademicsLookup::place);
    }

    /** The active class section of the institution for the year, grade and section, if academics has one. */
    public Optional<ClassPlace> classSectionOf(UUID institutionId, UUID academicYearId, UUID gradeLevelId,
            UUID sectionId, String purpose) {
        String filter = "institutionId = \"" + institutionId + "\" AND academicYearId = \"" + academicYearId
                + "\" AND gradeLevelId = \"" + gradeLevelId + "\" AND sectionId = \"" + sectionId + "\"";
        ListClassSectionsResponse found = Downstream.required("academics", "academics.class_section.read", purpose,
                () -> academics.listClassSections(ListClassSectionsRequest.newBuilder().setFilter(filter)
                        .setPageable(PageableRequest.newBuilder().setPage(1).setSize(10)).build()))
                .orElseThrow(() -> new UnavailableException("The academics service did not list the classes."));
        return found.getItemsList().stream().filter(ClassSection::getIsActive).findFirst().map(AcademicsLookup::place);
    }

    /**
     * The class as people say it ("Grade 5-A"): the grade's name and the section's name. Optional: empty when
     * academics cannot answer, or the caller may not read one of the three rows there.
     */
    public Optional<String> classLabel(UUID classSectionId) {
        if (classSectionId == null) {
            return Optional.empty();
        }
        Optional<ClassSection> section = Downstream.optional("academics",
                () -> academics.getClassSection(classSectionRequest(classSectionId)).getClassSection());
        if (section.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> grade = Downstream.optional("academics", () -> academics.getGradeLevel(GetGradeLevelRequest
                .newBuilder().setId(section.get().getGradeLevelId()).build()).getGradeLevel()).map(GradeLevel::getName);
        Optional<String> name = Downstream.optional("academics", () -> academics.getSection(GetSectionRequest
                .newBuilder().setId(section.get().getSectionId()).build()).getSection()).map(Section::getName);
        return grade.map(g -> name.map(n -> g + "-" + n).orElse(g));
    }

    /** Every subject of the course's plan, in academics' order. A course academics does not show is empty. */
    public List<PlannedSubject> courseSubjects(UUID courseId, String purpose) {
        List<PlannedSubject> subjects = new ArrayList<>();
        String cursor = "";
        for (int page = 1; page <= MAX_PAGES; page++) {
            PageableRequest pageable = PageableRequest.newBuilder().setPage(page).setSize(PAGE_SIZE).setCursor(cursor)
                    .build();
            Optional<ListCourseSubjectsResponse> list = Downstream.required("academics",
                    "academics.course_subject.read", purpose, () -> academics.listCourseSubjects(
                            ListCourseSubjectsRequest.newBuilder().setCourseId(courseId.toString())
                                    .setPageable(pageable).build()));
            if (list.isEmpty()) {
                return List.of();
            }
            for (CourseSubject subject : list.get().getItemsList()) {
                subjects.add(new PlannedSubject(UUID.fromString(subject.getId()), UUID.fromString(subject.getSubjectId()),
                        "elective".equalsIgnoreCase(subject.getSubjectCategory())));
            }
            cursor = list.get().getPagination().getNextCursor();
            if (cursor.isEmpty()) {
                return subjects;
            }
        }
        throw new InvalidStateException("The course lists more subjects than a registration can load.");
    }

    private Optional<GradeLevel> gradeLevel(UUID gradeLevelId, String purpose) {
        return Downstream.required("academics", "academics.grade_level.read", purpose,
                () -> academics.getGradeLevel(GetGradeLevelRequest.newBuilder().setId(gradeLevelId.toString()).build())
                        .getGradeLevel());
    }

    private static GetClassSectionRequest classSectionRequest(UUID classSectionId) {
        return GetClassSectionRequest.newBuilder().setId(classSectionId.toString()).build();
    }

    private static ClassPlace place(ClassSection section) {
        return new ClassPlace(UUID.fromString(section.getId()), uuid(section.getInstitutionId()),
                uuid(section.getAcademicYearId()), uuid(section.getGradeLevelId()), uuid(section.getSectionId()));
    }

    private static UUID uuid(String value) {
        return value.isEmpty() ? null : UUID.fromString(value);
    }
}
