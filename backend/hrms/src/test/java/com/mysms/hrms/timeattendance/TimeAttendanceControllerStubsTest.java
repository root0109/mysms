package com.mysms.hrms.timeattendance;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.hrms.timeattendance.controller.StaffAttendanceController;
import org.junit.jupiter.api.Test;

/**
 * The operations time_attendance does not serve yet answer 501 explicitly; a newly served operation changes the count
 * on purpose.
 */
class TimeAttendanceControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(StaffAttendanceController.class)).as("StaffAttendance").isEqualTo(0);
    }
}
