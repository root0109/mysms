package com.mysms.messaging.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.CannotCreateTransactionException;

/**
 * A relay pass that shutdown interrupts is not a failure (the relay stops quietly and its events wait in the outbox),
 * while a real failure, repeated, takes the events health down.
 */
class EventRuntimeTest {

    private final EventBus bus = mock(EventBus.class);
    private final OutboxRelay relay = mock(OutboxRelay.class);
    private final EventRuntime runtime = new EventRuntime(bus, List.of(relay), List.of(), List.of(),
            Duration.ofSeconds(1));

    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    void aPassInterruptedByShutdownIsNoFailure() {
        when(bus.name()).thenReturn("test");
        when(bus.connected()).thenReturn(true);
        when(relay.module()).thenReturn("iam");
        when(relay.runOnce()).thenThrow(new CannotCreateTransactionException("Could not open JDBC Connection",
                new SQLException("iam-relay - Interrupted during connection acquisition",
                        new InterruptedException())));

        for (int pass = 0; pass < 3; pass++) {
            assertThat(runtime.relayOnce()).isZero();
            assertThat(Thread.interrupted()).as("the interrupt is kept for the executor").isTrue();
        }
        assertThat(runtime.health().up()).isTrue();
    }

    @Test
    void repeatedFailuresTakeTheHealthDown() {
        when(bus.name()).thenReturn("test");
        when(bus.connected()).thenReturn(true);
        when(relay.module()).thenReturn("iam");
        when(relay.runOnce()).thenThrow(new IllegalStateException("database down"));

        for (int pass = 0; pass < 3; pass++) {
            assertThat(runtime.relayOnce()).isZero();
        }
        assertThat(runtime.health().up()).isFalse();
        assertThat(runtime.health().details()).containsKey("relay.iam");
    }
}
