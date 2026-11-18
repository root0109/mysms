package com.mysms.academics;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.academics.controller.BatchController;
import com.mysms.academics.controller.ClassSectionController;
import com.mysms.academics.controller.CourseController;
import com.mysms.academics.controller.CurriculumUnitController;
import com.mysms.academics.controller.GradeLevelController;
import com.mysms.academics.controller.LessonPlanController;
import com.mysms.academics.controller.ProgramController;
import com.mysms.academics.controller.ProgramLevelController;
import com.mysms.academics.controller.SectionController;
import com.mysms.academics.controller.SubjectController;
import com.mysms.academics.controller.TeachingAssignmentController;
import org.junit.jupiter.api.Test;

/**
 * The contract operations academics does not serve yet answer 501 NOT_IMPLEMENTED explicitly: none falls through, and a
 * newly served operation changes the count on purpose.
 */
class AcademicsControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(BatchController.class)).as("Batch").isEqualTo(0);
        assertThat(ControllerStubs.count(ClassSectionController.class)).as("ClassSection").isEqualTo(0);
        assertThat(ControllerStubs.count(CourseController.class)).as("Course").isEqualTo(0);
        assertThat(ControllerStubs.count(CurriculumUnitController.class)).as("CurriculumUnit").isEqualTo(0);
        assertThat(ControllerStubs.count(GradeLevelController.class)).as("GradeLevel").isEqualTo(0);
        assertThat(ControllerStubs.count(LessonPlanController.class)).as("LessonPlan").isEqualTo(0);
        assertThat(ControllerStubs.count(ProgramController.class)).as("Program").isEqualTo(0);
        assertThat(ControllerStubs.count(ProgramLevelController.class)).as("ProgramLevel").isEqualTo(0);
        assertThat(ControllerStubs.count(SectionController.class)).as("Section").isEqualTo(0);
        assertThat(ControllerStubs.count(SubjectController.class)).as("Subject").isEqualTo(0);
        assertThat(ControllerStubs.count(TeachingAssignmentController.class)).as("TeachingAssignment").isEqualTo(0);
    }
}
