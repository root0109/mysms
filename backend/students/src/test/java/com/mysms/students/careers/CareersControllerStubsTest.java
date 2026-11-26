package com.mysms.students.careers;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.students.careers.controller.CandidatePreferenceController;
import com.mysms.students.careers.controller.FunctionAreaController;
import com.mysms.students.careers.controller.IndustryController;
import com.mysms.students.careers.controller.JobOpeningController;
import org.junit.jupiter.api.Test;

/**
 * The operations careers does not serve yet answer 501 explicitly; a newly served operation changes the count on
 * purpose.
 */
class CareersControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(CandidatePreferenceController.class)).as("CandidatePreference").isEqualTo(0);
        assertThat(ControllerStubs.count(FunctionAreaController.class)).as("FunctionArea").isEqualTo(0);
        assertThat(ControllerStubs.count(IndustryController.class)).as("Industry").isEqualTo(0);
        assertThat(ControllerStubs.count(JobOpeningController.class)).as("JobOpening").isEqualTo(0);
    }
}
