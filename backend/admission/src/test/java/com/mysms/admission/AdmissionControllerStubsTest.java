package com.mysms.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.admission.controller.AdmissionCycleController;
import com.mysms.admission.controller.ApplicationController;
import org.junit.jupiter.api.Test;

/**
 * The operations admission does not serve yet answer 501 explicitly; a newly served operation changes the count on
 * purpose.
 */
class AdmissionControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(AdmissionCycleController.class)).as("AdmissionCycle").isEqualTo(0);
        assertThat(ControllerStubs.count(ApplicationController.class)).as("Application").isEqualTo(0);
    }
}
