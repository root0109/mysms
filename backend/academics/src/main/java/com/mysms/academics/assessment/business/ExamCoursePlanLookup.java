package com.mysms.academics.assessment.business;

import com.mysms.academics.api.model.CourseSubject;
import com.mysms.academics.assessment.domain.ExamEntity;
import com.mysms.academics.assessment.domain.ExamSessionEntity;
import com.mysms.academics.business.CourseService;
import com.mysms.authclient.SiblingAccess;
import com.mysms.core.error.ApiException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.paging.Page;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Reads the credit value for a higher-education exam subject from its owning course plan in the sibling academics
 * area, in process and as the caller: the caller needs academics' read permission, and academics scopes the rows.
 */
@Component
public class ExamCoursePlanLookup {

    static final String PERMISSION_COURSE_SUBJECT_READ = "academics.course_subject.read";
    private static final int PAGE_SIZE = 500;
    private static final int MAX_PAGES = 20;
    private static final Logger LOG = LoggerFactory.getLogger(ExamCoursePlanLookup.class);
    private final CourseService courses;
    private final SiblingAccess sibling;

    public ExamCoursePlanLookup(CourseService courses, SiblingAccess sibling) {
        this.courses = courses;
        this.sibling = sibling;
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
        if (!sibling.permits(PERMISSION_COURSE_SUBJECT_READ)) {
            throw needsPermission();
        }
        String filter = "subjectId = \"" + exam.getSubjectId() + "\"";
        String cursor = null;
        for (int page = 1; page <= MAX_PAGES; page++) {
            Page<CourseSubject> result = coursePlanPage(session.getCourseId(), page, cursor, filter);
            for (CourseSubject subject : result.items()) {
                if (exam.getSubjectId().equals(subject.getSubjectId())) {
                    BigDecimal credits = subject.getCredits() == null ? null : WireValues.decimal(subject.getCredits());
                    if (credits != null && credits.signum() <= 0) {
                        throw new ValidationFailedException(List.of(new FieldError("credits",
                                "course-plan credits must be positive", "MIN")));
                    }
                    return credits;
                }
            }
            if (!result.hasNext()) {
                throw new ValidationFailedException(List.of(new FieldError("subjectId",
                        "subject is not part of the exam session course plan", "NOT_FOUND")));
            }
            cursor = result.nextCursor();
        }
        throw new UnavailableException("The course plan exceeds the 10,000-subject lookup limit.");
    }

    /**
     * One page of the course plan, answered as the gRPC hop answered it: an unknown course is 503 (the course plan
     * could not be read), a refusal for academics' scope is this operation's 403 naming the permission, and any other
     * refusal is 503 — academics' status and message never reach the client.
     */
    private Page<CourseSubject> coursePlanPage(UUID courseId, int page, String cursor, String filter) {
        try {
            return courses.listCourseSubjects(courseId, page, PAGE_SIZE, cursor, filter, null);
        } catch (ApiException e) {
            if (e.status().value() == HttpStatus.NOT_FOUND.value()) {
                throw new UnavailableException("The academics service did not return the course plan.");
            }
            if (e.status().value() == HttpStatus.FORBIDDEN.value()) {
                throw needsPermission();
            }
            LOG.warn("the academics service answered {} ({})", e.status().value(), e.code());
            throw new UnavailableException("The academics service could not answer.");
        }
    }

    private static PermissionDeniedException needsPermission() {
        return new PermissionDeniedException("Populating exam results also needs " + PERMISSION_COURSE_SUBJECT_READ
                + ".");
    }
}
