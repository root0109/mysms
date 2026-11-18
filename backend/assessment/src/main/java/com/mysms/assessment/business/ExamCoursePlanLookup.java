package com.mysms.assessment.business;

import com.mysms.academics.v1.CourseSubject;
import com.mysms.academics.v1.ListCourseSubjectsRequest;
import com.mysms.academics.v1.ListCourseSubjectsResponse;
import com.mysms.assessment.domain.ExamEntity;
import com.mysms.assessment.domain.ExamSessionEntity;
import com.mysms.assessment.integration.AcademicsGrpcClient;
import com.mysms.common.v1.PageableRequest;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.mapping.WireValues;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/** Reads the credit value for a higher-education exam subject from its owning course plan, with the caller's identity. */
@Component
public class ExamCoursePlanLookup {

    private static final String SERVICE = "academics";
    private static final int PAGE_SIZE = 500;
    private static final int MAX_PAGES = 20;
    private final AcademicsGrpcClient academics;

    public ExamCoursePlanLookup(AcademicsGrpcClient academics) {
        this.academics = academics;
    }

    /** K-12 sessions have no course-plan credits; higher-education credits are snapshotted on the attendee. */
    public BigDecimal credits(ExamEntity exam, ExamSessionEntity session) {
        if (session.getCourseId() == null) {
            return null;
        }
        if (exam.getSubjectId() == null) {
            throw new ValidationFailedException(List.of(new FieldError("subjectId",
                    "a course exam needs a subject to read its credit value", "REQUIRED")));
        }
        String subjectId = exam.getSubjectId().toString();
        String filter = "subjectId = \"" + subjectId + "\"";
        String cursor = "";
        for (int page = 1; page <= MAX_PAGES; page++) {
            PageableRequest pageable = PageableRequest.newBuilder().setPage(page).setSize(PAGE_SIZE).setCursor(cursor)
                    .build();
            ListCourseSubjectsResponse result = Downstream.required(SERVICE, "academics.course_subject.read",
                            "Populating exam results", () -> academics.listCourseSubjects(ListCourseSubjectsRequest
                                    .newBuilder().setCourseId(session.getCourseId().toString()).setFilter(filter)
                                    .setPageable(pageable).build()))
                    .orElseThrow(() -> new UnavailableException("The academics service did not return the course plan."));
            for (CourseSubject subject : result.getItemsList()) {
                if (subjectId.equals(subject.getSubjectId())) {
                    BigDecimal credits = subject.hasCredits() ? WireValues.decimal(subject.getCredits()) : null;
                    if (credits != null && credits.signum() <= 0) {
                        throw new ValidationFailedException(List.of(new FieldError("credits",
                                "course-plan credits must be positive", "MIN")));
                    }
                    return credits;
                }
            }
            cursor = result.getPagination().getNextCursor();
            if (cursor.isEmpty()) {
                throw new ValidationFailedException(List.of(new FieldError("subjectId",
                        "subject is not part of the exam session course plan", "NOT_FOUND")));
            }
        }
        throw new UnavailableException("The course plan exceeds the 10,000-subject lookup limit.");
    }
}
