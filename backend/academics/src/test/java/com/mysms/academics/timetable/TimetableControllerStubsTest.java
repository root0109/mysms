package com.mysms.academics.timetable;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.testkit.ControllerStubs;
import com.mysms.academics.timetable.controller.PeriodController;
import com.mysms.academics.timetable.controller.SessionController;
import org.junit.jupiter.api.Test;

/**
 * The operations timetable does not serve yet answer 501 explicitly; a newly served operation changes the count on
 * purpose.
 */
class TimetableControllerStubsTest {

    @Test
    void unservedOperationsAnswerNotImplemented() {
        assertThat(ControllerStubs.count(PeriodController.class)).as("Period").isEqualTo(0);
        assertThat(ControllerStubs.count(SessionController.class)).as("Session").isEqualTo(0);
    }
}
