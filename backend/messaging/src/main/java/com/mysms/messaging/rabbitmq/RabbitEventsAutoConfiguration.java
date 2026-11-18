package com.mysms.messaging.rabbitmq;

import com.mysms.messaging.events.EventBus;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code mysms.events.bus=rabbitmq} (the default) on {@code mysms.events.rabbitmq.uri}; a service refuses to start
 * without the URI rather than run without events. Events are accepted from the broker user
 * {@code mysms.events.rabbitmq.publisher-user} names for their module ({@code self} in the monolith).
 */
@AutoConfiguration
@ConditionalOnProperty(name = "mysms.events.bus", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitEventsAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(EventBus.class)
    public EventBus rabbitEventBus(Environment environment, JsonMapper json) {
        String uri = environment.getProperty("mysms.events.rabbitmq.uri", "");
        if (uri.isBlank()) {
            throw new IllegalStateException("mysms.events.rabbitmq.uri (MYSMS_RABBITMQ_URI) is required with the "
                    + "RabbitMQ bus; set mysms.events.bus=in-process for a single process without a broker");
        }
        String exchange = environment.getProperty("mysms.events.rabbitmq.exchange", "mysms.events");
        String publisherUser = environment.getProperty("mysms.events.rabbitmq.publisher-user", "");
        RabbitEventBus.Settings defaults = RabbitEventBus.Settings.of(URI.create(uri), exchange, publisherUser);
        RabbitEventBus.Settings settings = new RabbitEventBus.Settings(defaults.uri(), exchange,
                environment.getProperty("mysms.events.rabbitmq.delivery-limit", Integer.class, defaults.deliveryLimit()),
                environment.getProperty("mysms.events.rabbitmq.prefetch", Integer.class, defaults.prefetch()),
                environment.getProperty("mysms.events.rabbitmq.publish-timeout", Duration.class,
                        defaults.publishTimeout()),
                environment.getProperty("mysms.events.rabbitmq.retry-delay", Duration.class, defaults.retryDelay()),
                environment.getProperty("mysms.events.rabbitmq.max-retry-delay", Duration.class,
                        defaults.maxRetryDelay()),
                defaults.publisherUser(),
                environment.getProperty("mysms.events.rabbitmq.unrouted-retention", Duration.class,
                        defaults.unroutedRetention()));
        return new RabbitEventBus(settings, json,
                "mysms-" + environment.getProperty("spring.application.name", "service"));
    }
}
