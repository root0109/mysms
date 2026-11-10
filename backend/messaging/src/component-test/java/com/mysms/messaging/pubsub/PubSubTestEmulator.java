package com.mysms.messaging.pubsub;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/** Real Google Pub/Sub emulator for component tests, or an externally managed emulator from the test environment. */
final class PubSubTestEmulator {

    private static final String IMAGE = "gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators";
    private static String host;

    private PubSubTestEmulator() {
    }

    static synchronized String host() {
        if (host == null) {
            String given = System.getenv("MYSMS_TEST_PUBSUB_EMULATOR_HOST");
            if (given != null && !given.isBlank()) {
                host = given;
            } else {
                GenericContainer<?> emulator = new GenericContainer<>(IMAGE)
                        .withExposedPorts(8085)
                        .withCommand("gcloud", "beta", "emulators", "pubsub", "start", "--project=mysms-test",
                                "--host-port=0.0.0.0:8085")
                        .waitingFor(Wait.forLogMessage(".*Server started, listening on (port )?8085.*", 1));
                emulator.start();
                host = emulator.getHost() + ":" + emulator.getMappedPort(8085);
            }
        }
        return host;
    }
}
