package com.mysms.messaging.pubsub;

import com.mysms.messaging.events.EventBus;
import com.mysms.messaging.MessagingModules;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

/** Creates the Google Cloud Pub/Sub adapter when {@code mysms.events.bus=google-pubsub}. */
@AutoConfiguration
@ConditionalOnProperty(name = "mysms.events.bus", havingValue = "google-pubsub")
public class GooglePubSubEventsAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(EventBus.class)
    public EventBus googlePubSubEventBus(Environment environment, JsonMapper json) {
        String emulator = environment.getProperty("mysms.events.google-pubsub.emulator-host",
                environment.getProperty("PUBSUB_EMULATOR_HOST", ""));
        String project = environment.getProperty("mysms.events.google-pubsub.project-id", "");
        if (project.isBlank()) {
            project = environment.getProperty("GOOGLE_CLOUD_PROJECT", "");
        }
        if (project.isBlank()) {
            project = environment.getProperty("GCLOUD_PROJECT", "");
        }
        if (project.isBlank() && !emulator.isBlank()) {
            project = "mysms-local";
        }
        if (project.isBlank()) {
            throw new IllegalStateException("mysms.events.google-pubsub.project-id (GOOGLE_CLOUD_PROJECT) is "
                    + "required with the Google Cloud Pub/Sub bus");
        }
        Set<String> modules = Arrays.stream(environment.getProperty("mysms.events.google-pubsub.source-modules",
                        String.join(",", MessagingModules.ALL)).split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
        boolean createResources = environment.getProperty("mysms.events.google-pubsub.create-resources",
                Boolean.class, environment.getProperty("MYSMS_EVENTS_GCP_CREATE_RESOURCES", Boolean.class,
                        !emulator.isBlank()));
        GooglePubSubEventBus.Settings settings = new GooglePubSubEventBus.Settings(project,
                environment.getProperty("mysms.events.google-pubsub.topic-prefix", "mysms-events-"),
                environment.getProperty("mysms.events.google-pubsub.subscription-prefix", "mysms-"), modules,
                createResources, emulator,
                environment.getProperty("mysms.events.google-pubsub.topic-retention", Duration.class,
                        Duration.ofDays(3)),
                environment.getProperty("mysms.events.google-pubsub.publish-timeout", Duration.class,
                        Duration.ofSeconds(10)),
                environment.getProperty("mysms.events.google-pubsub.retry-delay", Duration.class,
                        Duration.ofSeconds(10)),
                environment.getProperty("mysms.events.google-pubsub.max-retry-delay", Duration.class,
                        Duration.ofSeconds(60)),
                environment.getProperty("mysms.events.google-pubsub.max-delivery-attempts", Integer.class, 5),
                environment.getProperty("mysms.events.google-pubsub.max-outstanding-messages", Long.class, 10L));
        return new GooglePubSubEventBus(settings, json);
    }
}
