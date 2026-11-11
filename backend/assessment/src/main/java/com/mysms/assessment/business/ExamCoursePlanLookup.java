package com.mysms.assessment.business;

import com.mysms.assessment.domain.ExamEntity;
import com.mysms.assessment.domain.ExamSessionEntity;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.paging.PagingUtil;
import com.mysms.sdk.academics.api.CourseApi;
import com.mysms.sdk.academics.model.CourseSubject;
import com.mysms.sdk.academics.model.PagingInfoCourseSubject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Reads the credit value for a higher-education exam subject from its owning course plan. */
public class ExamCoursePlanLookup {

    private static final Logger LOG = LoggerFactory.getLogger(ExamCoursePlanLookup.class);
    private static final int PAGE_SIZE = 500;
    private static final int MAX_PAGES = 20;
    private final CourseApi courses;

    public ExamCoursePlanLookup(CourseApi courses) {
        this.courses = courses;
    }

    public static ExamCoursePlanLookup unconfigured() {
        return new ExamCoursePlanLookup(null);
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
        if (courses == null) {
            throw new UnavailableException("The Academics service is not configured for course-plan lookup.");
        }
        Optional<String> cursor = Optional.empty();
        String filter = "subjectId = \"" + exam.getSubjectId() + "\"";
        for (int page = 1; page <= MAX_PAGES; page++) {
            int pageNumber = page;
            String after = cursor.orElse(null);
            PagingInfoCourseSubject result = call(() -> courses.listCourseSubjects(session.getCourseId(), pageNumber,
                    PAGE_SIZE, after, filter, null, "subjectId,credits"))
                    .orElseThrow(() -> new UnavailableException("The Academics service did not return the course plan."));
            for (CourseSubject subject : result.getData()) {
                if (exam.getSubjectId().equals(subject.getSubjectId())) {
                    BigDecimal credits = WireValues.decimal(subject.getCredits());
                    if (credits != null && credits.signum() <= 0) {
                        throw new ValidationFailedException(List.of(new FieldError("credits",
                                "course-plan credits must be positive", "MIN")));
                    }
                    return credits;
                }
            }
            cursor = PagingUtil.cursorOf(result.getPaging().getNext());
            if (cursor.isEmpty()) {
                throw new ValidationFailedException(List.of(new FieldError("subjectId",
                        "subject is not part of the exam session course plan", "NOT_FOUND")));
            }
        }
        throw new UnavailableException("The course plan exceeds the 10,000-subject lookup limit.");
    }

    private static <T> Optional<T> call(Supplier<ResponseEntity<T>> request) {
        try {
            return Optional.ofNullable(request.get().getBody());
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 403) {
                throw new PermissionDeniedException("Populating exam results also needs academics.course_subject.read.");
            }
            if (status.value() == 404) {
                return Optional.empty();
            }
            LOG.warn("Academics answered {} to course-plan lookup", status.value());
            throw new UnavailableException("The Academics service could not provide the course plan.");
        } catch (RestClientException e) {
            LOG.warn("Academics course-plan lookup failed", e);
            throw new UnavailableException("The Academics service could not provide the course plan.");
        }
    }
}
