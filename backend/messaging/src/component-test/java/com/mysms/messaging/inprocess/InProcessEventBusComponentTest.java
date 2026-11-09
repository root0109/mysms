package com.mysms.messaging.inprocess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.RejectedEventException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InProcessEventBusComponentTest {

    private static final UUID TENANT = UUID.fromString("0190b0a0-0000-7000-8000-00000000000a");

    @Test
    void retriesFailedDeliveriesThenParksThemAndKeepsOtherConsumersIndependent() {
        InProcessEventBus bus = new InProcessEventBus(2);
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger otherConsumer = new AtomicInteger();
        bus.subscribe("coretest", Set.of("coretest.item.deleted"), event -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("temporary failure");
        });
        bus.subscribe("sis", Set.of("coretest.item.deleted"), event -> otherConsumer.incrementAndGet());
        EventEnvelope event = event("coretest.item.deleted");

        assertThatThrownBy(() -> bus.publish(event)).isInstanceOf(IllegalStateException.class);
        bus.publish(event);
        bus.publish(event);

        assertThat(attempts.get()).isEqualTo(2);
        assertThat(otherConsumer.get()).isEqualTo(3);
        assertThat(bus.name()).isEqualTo("in-process");
        bus.close();
        bus.publish(event);
        assertThat(otherConsumer.get()).isEqualTo(3);
    }

    @Test
    void aRejectedEventIsParkedOnItsFirstDelivery() {
        InProcessEventBus bus = new InProcessEventBus();
        AtomicInteger attempts = new AtomicInteger();
        bus.subscribe("hr", Set.of("coretest.item.created"), event -> {
            attempts.incrementAndGet();
            throw new RejectedEventException("not supported");
        });
        EventEnvelope event = event("coretest.item.created");

        bus.publish(event);
        bus.publish(event);

        assertThat(attempts.get()).isOne();
        assertThatThrownBy(() -> new InProcessEventBus(0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static EventEnvelope event(String type) {
        return new EventEnvelope(UUID.randomUUID(), "coretest", type, Optional.of(TENANT), "coretest.item",
                UUID.randomUUID(), Instant.now(), "{}", Optional.empty());
    }
}
