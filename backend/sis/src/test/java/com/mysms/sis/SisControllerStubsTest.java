package com.mysms.sis;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.sis.controller.ActivityTypeController;
import com.mysms.sis.controller.DisciplineIncidentController;
import com.mysms.sis.controller.GuardianController;
import com.mysms.sis.controller.ProgramEnrollmentController;
import com.mysms.sis.controller.PromotionController;
import com.mysms.sis.controller.RelationshipTypeController;
import com.mysms.sis.controller.StudentController;
import com.mysms.sis.controller.StudentActivityController;
import com.mysms.sis.controller.StudentCategoryController;
import com.mysms.sis.controller.StudentEnrollmentController;
import com.mysms.sis.controller.StudentHealthRecordController;
import com.mysms.sis.controller.StudentLeaveRequestController;
import com.mysms.sis.controller.SubjectRegistrationController;
import org.junit.jupiter.api.Test;

/**
 * The contract operations sis does not serve yet answer 501 NOT_IMPLEMENTED explicitly — none falls through, and a
 * newly served operation changes the count on purpose.
 */
class SisControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(ActivityTypeController.class)).as("ActivityType").isEqualTo(0);
        assertThat(ControllerStubs.count(DisciplineIncidentController.class)).as("DisciplineIncident").isEqualTo(0);
        assertThat(ControllerStubs.count(GuardianController.class)).as("Guardian").isEqualTo(0);
        assertThat(ControllerStubs.count(ProgramEnrollmentController.class)).as("ProgramEnrollment").isEqualTo(0);
        assertThat(ControllerStubs.count(PromotionController.class)).as("Promotion").isEqualTo(0);
        assertThat(ControllerStubs.count(RelationshipTypeController.class)).as("RelationshipType").isEqualTo(0);
        assertThat(ControllerStubs.count(StudentController.class)).as("Student").isEqualTo(0);
        assertThat(ControllerStubs.count(StudentActivityController.class)).as("StudentActivity").isEqualTo(0);
        assertThat(ControllerStubs.count(StudentCategoryController.class)).as("StudentCategory").isEqualTo(0);
        assertThat(ControllerStubs.count(StudentEnrollmentController.class)).as("StudentEnrollment").isEqualTo(0);
        assertThat(ControllerStubs.count(StudentHealthRecordController.class)).as("StudentHealthRecord").isEqualTo(0);
        assertThat(ControllerStubs.count(StudentLeaveRequestController.class)).as("StudentLeaveRequest").isEqualTo(0);
        assertThat(ControllerStubs.count(SubjectRegistrationController.class)).as("SubjectRegistration").isEqualTo(0);
    }
}
