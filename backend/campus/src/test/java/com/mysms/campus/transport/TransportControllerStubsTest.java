package com.mysms.campus.transport;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.campus.transport.controller.RouteController;
import com.mysms.campus.transport.controller.StudentTransportController;
import org.junit.jupiter.api.Test;

/**
 * The operations transport does not serve yet answer 501 explicitly; a newly served operation changes the count on
 * purpose.
 */
class TransportControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(RouteController.class)).as("Route").isEqualTo(0);
        assertThat(ControllerStubs.count(StudentTransportController.class)).as("StudentTransport").isEqualTo(0);
    }
}
