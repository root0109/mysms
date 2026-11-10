package com.mysms.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.admission.controller.AdmissionCycleController;
import com.mysms.admission.controller.ApplicationController;
import org.junit.jupiter.api.Test;

/**
 * The contract operations admission does not serve yet answer 501 NOT_IMPLEMENTED explicitly — none falls through, and
 * a
 * newly served operation changes the count on purpose.
 */
class AdmissionControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(AdmissionCycleController.class)).as("AdmissionCycle").isEqualTo(2);
        assertThat(ControllerStubs.count(ApplicationController.class)).as("Application").isEqualTo(9);
    }
}
