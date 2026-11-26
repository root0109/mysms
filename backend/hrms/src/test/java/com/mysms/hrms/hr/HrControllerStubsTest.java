package com.mysms.hrms.hr;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.hrms.hr.controller.DesignationController;
import com.mysms.hrms.hr.controller.LeaveApplicationController;
import com.mysms.hrms.hr.controller.LeaveBalanceController;
import com.mysms.hrms.hr.controller.LeaveTypeController;
import com.mysms.hrms.hr.controller.StaffController;
import org.junit.jupiter.api.Test;

/**
 * The operations hr does not serve yet answer 501 explicitly; a newly served operation changes the count on purpose.
 */
class HrControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(DesignationController.class)).as("Designation").isEqualTo(0);
        assertThat(ControllerStubs.count(LeaveApplicationController.class)).as("LeaveApplication").isEqualTo(0);
        assertThat(ControllerStubs.count(LeaveBalanceController.class)).as("LeaveBalance").isEqualTo(0);
        assertThat(ControllerStubs.count(LeaveTypeController.class)).as("LeaveType").isEqualTo(0);
        assertThat(ControllerStubs.count(StaffController.class)).as("Staff").isEqualTo(0);
    }
}
