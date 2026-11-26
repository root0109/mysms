package com.mysms.hrms.payroll;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.hrms.payroll.controller.DisbursementAccountController;
import com.mysms.hrms.payroll.controller.PayCategoryController;
import com.mysms.hrms.payroll.controller.PayComponentController;
import com.mysms.hrms.payroll.controller.PaymentTypeController;
import com.mysms.hrms.payroll.controller.PayrollPaymentController;
import com.mysms.hrms.payroll.controller.PayslipController;
import com.mysms.hrms.payroll.controller.SalaryTemplateController;
import com.mysms.hrms.payroll.controller.StaffBankAccountController;
import com.mysms.hrms.payroll.controller.StaffSalaryController;
import org.junit.jupiter.api.Test;

/**
 * The operations payroll does not serve yet answer 501 explicitly; a newly served operation changes the count on
 * purpose.
 */
class PayrollControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(DisbursementAccountController.class)).as("DisbursementAccount").isEqualTo(0);
        assertThat(ControllerStubs.count(PayCategoryController.class)).as("PayCategory").isEqualTo(0);
        assertThat(ControllerStubs.count(PayComponentController.class)).as("PayComponent").isEqualTo(0);
        assertThat(ControllerStubs.count(PaymentTypeController.class)).as("PaymentType").isEqualTo(0);
        assertThat(ControllerStubs.count(PayrollPaymentController.class)).as("PayrollPayment").isEqualTo(0);
        assertThat(ControllerStubs.count(PayslipController.class)).as("Payslip").isEqualTo(0);
        assertThat(ControllerStubs.count(SalaryTemplateController.class)).as("SalaryTemplate").isEqualTo(0);
        assertThat(ControllerStubs.count(StaffBankAccountController.class)).as("StaffBankAccount").isEqualTo(0);
        assertThat(ControllerStubs.count(StaffSalaryController.class)).as("StaffSalary").isEqualTo(0);
    }
}
