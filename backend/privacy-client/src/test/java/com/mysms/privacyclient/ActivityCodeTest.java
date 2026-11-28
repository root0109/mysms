package com.mysms.privacyclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ActivityCodeTest {

    @Test
    void anAreaMayHoldAnUnderscore() {
        assertThat(new ActivityCode("time_attendance.device_capture").module()).isEqualTo("time_attendance");
        assertThat(new ActivityCode("comms.marketing_broadcast").module()).isEqualTo("comms");
    }

    @Test
    void refusesWhatIsNotAreaDotActivity() {
        for (String bad : new String[] {"_comms.notice", "Comms.notice", "comms", "comms.notice.x", "comms.Notice"}) {
            assertThatThrownBy(() -> new ActivityCode(bad)).as(bad).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new ActivityCode(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
