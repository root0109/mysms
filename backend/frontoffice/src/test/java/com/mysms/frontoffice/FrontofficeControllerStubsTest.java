package com.mysms.frontoffice;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.frontoffice.controller.CallLogController;
import com.mysms.frontoffice.controller.DirectoryContactController;
import com.mysms.frontoffice.controller.PostalRecordController;
import com.mysms.frontoffice.controller.VisitorLogController;
import org.junit.jupiter.api.Test;

/**
 * The operations frontoffice does not serve yet answer 501 explicitly; a newly served operation changes the count on
 * purpose.
 */
class FrontofficeControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(CallLogController.class)).as("CallLog").isEqualTo(0);
        assertThat(ControllerStubs.count(DirectoryContactController.class)).as("DirectoryContact").isEqualTo(0);
        assertThat(ControllerStubs.count(PostalRecordController.class)).as("PostalRecord").isEqualTo(0);
        assertThat(ControllerStubs.count(VisitorLogController.class)).as("VisitorLog").isEqualTo(0);
    }
}
