package com.mysms.messaging.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.messaging.MessagingModules;
import org.junit.jupiter.api.Test;

/** The broker user a consumer expects an event from, resolved per serving module. */
class RabbitEventBusTest {

    @Test
    void anEventIsExpectedFromTheBrokerUserOfTheModuleServingItsArea() {
        assertThat(RabbitEventBus.expectedPublisher(RabbitEventBus.DEFAULT_PUBLISHER_USER, "payroll", "guest"))
                .isEqualTo("mysms-hrms");
        assertThat(RabbitEventBus.expectedPublisher(RabbitEventBus.DEFAULT_PUBLISHER_USER, "hr", "guest"))
                .isEqualTo("mysms-hrms");
        assertThat(RabbitEventBus.expectedPublisher(RabbitEventBus.DEFAULT_PUBLISHER_USER, "time_attendance", "guest"))
                .isEqualTo("mysms-hrms");
        assertThat(RabbitEventBus.expectedPublisher("svc-{module}", "people", "guest")).isEqualTo("svc-identity");
        assertThat(RabbitEventBus.expectedPublisher(RabbitEventBus.DEFAULT_PUBLISHER_USER, "finance", "guest"))
                .isEqualTo("mysms-finance");
    }

    @Test
    void theMonolithExpectsItsOwnUserAndAnUnknownAreaNoUser() {
        assertThat(RabbitEventBus.expectedPublisher(RabbitEventBus.SELF, "payroll", "guest")).isEqualTo("guest");
        assertThat(RabbitEventBus.expectedPublisher(RabbitEventBus.DEFAULT_PUBLISHER_USER, "nowhere", "guest"))
                .isNull();
    }

    @Test
    void everySourceAreaIsServedByOneOfTheTenModules() {
        assertThat(MessagingModules.AREAS.stream().map(MessagingModules::moduleOf).distinct())
                .containsExactlyInAnyOrder("platform", "identity", "privacy", "comms", "hrms", "students",
                        "academics", "finance", "campus", "learning");
    }
}
