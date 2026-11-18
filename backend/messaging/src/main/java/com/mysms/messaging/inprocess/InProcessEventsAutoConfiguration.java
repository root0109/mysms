package com.mysms.messaging.inprocess;

import com.mysms.messaging.events.EventBus;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/** The in-process event adapter for a single JVM. */
@AutoConfiguration
@ConditionalOnProperty(name = "mysms.events.bus", havingValue = "in-process")
public class InProcessEventsAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(EventBus.class)
    public EventBus inProcessEventBus(Environment environment) {
        return new InProcessEventBus(environment.getProperty("mysms.events.in-process.delivery-limit", Integer.class,
                InProcessEventBus.DEFAULT_DELIVERY_LIMIT));
    }
}
