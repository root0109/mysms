package com.mysms.attendance;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.attendance.controller.AbsenceReasonController;
import com.mysms.attendance.controller.AttendanceRegisterController;
import com.mysms.attendance.controller.AttendanceSheetController;
import com.mysms.attendance.controller.AttendanceTypeController;
import com.mysms.attendance.controller.StaffAttendanceController;
import org.junit.jupiter.api.Test;

/**
 * The operations attendance does not serve yet answer 501 explicitly; a newly served operation changes the count on
 * purpose.
 */
class AttendanceControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(AbsenceReasonController.class)).as("AbsenceReason").isEqualTo(0);
        assertThat(ControllerStubs.count(AttendanceRegisterController.class)).as("AttendanceRegister").isEqualTo(0);
        assertThat(ControllerStubs.count(AttendanceSheetController.class)).as("AttendanceSheet").isEqualTo(0);
        assertThat(ControllerStubs.count(AttendanceTypeController.class)).as("AttendanceType").isEqualTo(0);
        assertThat(ControllerStubs.count(StaffAttendanceController.class)).as("StaffAttendance").isEqualTo(0);
    }
}
