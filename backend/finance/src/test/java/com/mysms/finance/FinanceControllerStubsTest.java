package com.mysms.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.finance.controller.AcquisitionSourceController;
import com.mysms.finance.controller.CouponController;
import com.mysms.finance.controller.FeeConcessionController;
import com.mysms.finance.controller.FeeHeadController;
import com.mysms.finance.controller.FeePlanController;
import com.mysms.finance.controller.FeePlanInstallmentComponentController;
import com.mysms.finance.controller.FeeStructureController;
import com.mysms.finance.controller.InvoiceController;
import com.mysms.finance.controller.PaymentGatewayAccountController;
import com.mysms.finance.controller.PaymentController;
import com.mysms.finance.controller.RefundController;
import com.mysms.finance.controller.StudentFeeDueController;
import org.junit.jupiter.api.Test;

/**
 * The contract operations finance does not serve yet answer 501 NOT_IMPLEMENTED explicitly — none falls through, and a
 * newly served operation changes the count on purpose.
 */
class FinanceControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(AcquisitionSourceController.class)).as("AcquisitionSource").isEqualTo(0);
        assertThat(ControllerStubs.count(CouponController.class)).as("Coupon").isEqualTo(0);
        assertThat(ControllerStubs.count(FeeConcessionController.class)).as("FeeConcession").isEqualTo(0);
        assertThat(ControllerStubs.count(FeeHeadController.class)).as("FeeHead").isEqualTo(0);
        assertThat(ControllerStubs.count(FeePlanController.class)).as("FeePlan").isEqualTo(0);
        assertThat(ControllerStubs.count(FeePlanInstallmentComponentController.class))
                .as("FeePlanInstallmentComponent").isEqualTo(0);
        assertThat(ControllerStubs.count(FeeStructureController.class)).as("FeeStructure").isEqualTo(0);
        assertThat(ControllerStubs.count(InvoiceController.class)).as("Invoice").isEqualTo(0);
        assertThat(ControllerStubs.count(PaymentGatewayAccountController.class)).as("PaymentGatewayAccount")
                .isEqualTo(0);
        assertThat(ControllerStubs.count(PaymentController.class)).as("Payment").isEqualTo(0);
        assertThat(ControllerStubs.count(RefundController.class)).as("Refund").isEqualTo(0);
        assertThat(ControllerStubs.count(StudentFeeDueController.class)).as("StudentFeeDue").isEqualTo(0);
    }
}
