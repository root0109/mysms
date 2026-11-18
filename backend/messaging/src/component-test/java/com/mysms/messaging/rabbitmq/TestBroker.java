package com.mysms.messaging.rabbitmq;

import java.net.URI;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * A real RabbitMQ for the tests: Testcontainers RabbitMQ 4.1, or the broker at {@code MYSMS_TEST_AMQP_URI} (CI without
 * Docker). Each test uses its own exchange and consumer names, so a shared broker needs no cleanup.
 */
final class TestBroker {

    private static final String IMAGE = "rabbitmq:4.1-alpine";
    private static URI uri;

    private TestBroker() {
    }

    static synchronized URI uri() {
        if (uri == null) {
            String given = System.getenv("MYSMS_TEST_AMQP_URI");
            if (given != null && !given.isBlank()) {
                uri = URI.create(given);
            } else {
                GenericContainer<?> broker = new GenericContainer<>(IMAGE).withExposedPorts(5672)
                        .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));
                broker.start();
                uri = URI.create("amqp://guest:guest@" + broker.getHost() + ":" + broker.getMappedPort(5672));
            }
        }
        return uri;
    }
}
