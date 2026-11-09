package com.mysms.messaging.rabbitmq;

import java.io.PrintStream;
import java.net.URI;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Operator commands for the events on RabbitMQ (DEPLOYMENT.md "Events"), run from any service jar:
 *
 * <pre>
 * MYSMS_RABBITMQ_URI=amqps://mysms-ops:...@broker:5671/mysms \
 *   java -cp academics.jar -Dloader.main=com.mysms.messaging.rabbitmq.EventsAdmin \
 *     org.springframework.boot.loader.launch.PropertiesLauncher replay-dead academics [max]
 * </pre>
 *
 * <ul>
 *   <li>{@code replay-dead <module> [max]}: returns a module's dead-lettered events to its queue.</li>
 *   <li>{@code replay-unrouted [max]}: publishes again the events no queue was bound to.</li>
 * </ul>
 * The broker user needs the {@code impersonator} tag: replayed events keep their original publisher's `user-id`.
 */
public final class EventsAdmin {

    private static final int DEFAULT_MAX = 10_000;

    private EventsAdmin() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.getenv(), System.out, System.err));
    }

    /** Runs one command; the process exit code (0 done, 1 failed, 2 usage). */
    static int run(String[] args, Map<String, String> environment, PrintStream out, PrintStream err) {
        String uri = environment.getOrDefault("MYSMS_RABBITMQ_URI", "");
        if (args.length == 0 || uri.isBlank()) {
            err.println("usage: MYSMS_RABBITMQ_URI=amqp(s)://... EventsAdmin replay-dead <module> [max] | "
                    + "replay-unrouted [max]   (MYSMS_RABBITMQ_EXCHANGE, default mysms.events)");
            return 2;
        }
        String exchange = environment.getOrDefault("MYSMS_RABBITMQ_EXCHANGE", "mysms.events");
        try (RabbitEventBus bus = new RabbitEventBus(RabbitEventBus.Settings.of(URI.create(uri), exchange,
                RabbitEventBus.SELF), JsonMapper.builder().build(), "mysms-events-admin")) {
            switch (args[0]) {
                case "replay-dead" -> {
                    if (args.length < 2) {
                        err.println("replay-dead needs the consuming module");
                        return 2;
                    }
                    out.println("returned " + bus.replayDead(args[1], max(args, 2)) + " events to "
                            + RabbitEventBus.queueOf(args[1]));
                }
                case "replay-unrouted" -> out.println("published again " + bus.replayUnrouted(max(args, 1))
                        + " events");
                default -> {
                    err.println("unknown command " + args[0]);
                    return 2;
                }
            }
            return 0;
        } catch (RuntimeException e) {
            err.println("failed: " + e.getMessage());
            return 1;
        }
    }

    private static int max(String[] args, int at) {
        return args.length > at ? Integer.parseInt(args[at]) : DEFAULT_MAX;
    }
}
