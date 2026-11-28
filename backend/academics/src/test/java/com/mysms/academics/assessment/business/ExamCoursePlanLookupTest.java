package com.mysms.academics.assessment.business;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mysms.academics.assessment.domain.ExamEntity;
import com.mysms.academics.assessment.domain.ExamSessionEntity;
import com.mysms.academics.business.CourseService;
import com.mysms.authclient.SiblingAccess;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.UnavailableException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The course-plan credit lookup against a stand-in of the sibling academics service: its refusals as the gRPC hop. */
class ExamCoursePlanLookupTest {

    private static final String NEEDS = "Populating exam results also needs academics.course_subject.read.";

    private final CourseService courses = mock(CourseService.class);
    private final SiblingAccess sibling = mock(SiblingAccess.class);
    private final ExamCoursePlanLookup lookup = new ExamCoursePlanLookup(courses, sibling);
    private final ExamEntity exam = new ExamEntity();
    private final ExamSessionEntity session = new ExamSessionEntity();

    @BeforeEach
    void aCourseExam() {
        exam.setSubjectId(UUID.randomUUID());
        session.setCourseId(UUID.randomUUID());
        when(sibling.permits(ExamCoursePlanLookup.PERMISSION_COURSE_SUBJECT_READ)).thenReturn(true);
    }

    @Test
    void withoutThePermissionTheCallerIsRefusedNamingIt() {
        when(sibling.permits(ExamCoursePlanLookup.PERMISSION_COURSE_SUBJECT_READ)).thenReturn(false);
        assertThatThrownBy(() -> lookup.credits(exam, session)).isInstanceOf(PermissionDeniedException.class)
                .hasMessage(NEEDS);
    }

    @Test
    void academicsOwnRefusalsAnswerAsTheGrpcHopDid() {
        when(courses.listCourseSubjects(any(), any(), any(), any(), any(), any()))
                .thenThrow(new PermissionDeniedException("outside the caller's scope"))
                .thenThrow(new ResourceNotFoundException("academics.course", session.getCourseId()))
                .thenThrow(new InvalidStateException("academics refused"));
        assertThatThrownBy(() -> lookup.credits(exam, session)).as("a scope refusal names the permission")
                .isInstanceOf(PermissionDeniedException.class).hasMessage(NEEDS);
        assertThatThrownBy(() -> lookup.credits(exam, session)).as("an unknown course")
                .isInstanceOf(UnavailableException.class);
        assertThatThrownBy(() -> lookup.credits(exam, session)).as("academics' message never reaches the client")
                .isInstanceOf(UnavailableException.class).hasMessageNotContaining("academics refused");
    }
}
