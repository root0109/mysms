package com.mysms.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.assessment.controller.AssignmentController;
import com.mysms.assessment.controller.AssignmentSubmissionController;
import com.mysms.assessment.controller.AssignmentTypeController;
import com.mysms.assessment.controller.ExamController;
import com.mysms.assessment.controller.ExamResultController;
import com.mysms.assessment.controller.ExamSessionController;
import com.mysms.assessment.controller.ExamTypeController;
import com.mysms.assessment.controller.GradingBandController;
import com.mysms.assessment.controller.GradingSchemeController;
import com.mysms.assessment.controller.HomeworkController;
import com.mysms.assessment.controller.HomeworkSubmissionController;
import com.mysms.assessment.controller.MarksheetController;
import com.mysms.assessment.controller.MarksheetLineController;
import com.mysms.assessment.controller.ResultTemplateController;
import org.junit.jupiter.api.Test;

/**
 * Every assessment operation listed as served by the batch has a controller implementation; no contract route falls
 * through to a 501 placeholder.
 */
class AssessmentControllerStubsTest {

    @Test
    void noAssessmentOperationAnswersNotImplemented() {
        assertThat(ControllerStubs.count(AssignmentController.class)).as("Assignment").isZero();
        assertThat(ControllerStubs.count(AssignmentSubmissionController.class)).as("AssignmentSubmission").isZero();
        assertThat(ControllerStubs.count(AssignmentTypeController.class)).as("AssignmentType").isEqualTo(0);
        assertThat(ControllerStubs.count(ExamController.class)).as("Exam").isZero();
        assertThat(ControllerStubs.count(ExamResultController.class)).as("ExamResult").isEqualTo(0);
        assertThat(ControllerStubs.count(ExamSessionController.class)).as("ExamSession").isEqualTo(0);
        assertThat(ControllerStubs.count(ExamTypeController.class)).as("ExamType").isEqualTo(0);
        assertThat(ControllerStubs.count(GradingBandController.class)).as("GradingBand").isEqualTo(0);
        assertThat(ControllerStubs.count(GradingSchemeController.class)).as("GradingScheme").isZero();
        assertThat(ControllerStubs.count(HomeworkController.class)).as("Homework").isEqualTo(0);
        assertThat(ControllerStubs.count(HomeworkSubmissionController.class)).as("HomeworkSubmission").isEqualTo(0);
        assertThat(ControllerStubs.count(MarksheetController.class)).as("Marksheet").isZero();
        assertThat(ControllerStubs.count(MarksheetLineController.class)).as("MarksheetLine").isZero();
        assertThat(ControllerStubs.count(ResultTemplateController.class)).as("ResultTemplate").isZero();
    }
}
