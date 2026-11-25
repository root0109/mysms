package com.mysms.privacyclient.testing;

import com.mysms.privacyclient.LegalHoldGrpcClient;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Puts {@link TestLegalHolds} in place of privacy's legal-hold client in every component test of a module with a
 * privacy task handler (privacy-client's test jar is on the component tests' classpath only).
 */
@AutoConfiguration
public final class TestLegalHoldsAutoConfiguration {

    private TestLegalHoldsAutoConfiguration() {
    }

    /** Static, as a post-processor bean must be, so the configuration is not created early. */
    @Bean
    static BeanPostProcessor testLegalHolds() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String name) {
                return bean instanceof LegalHoldGrpcClient ? new TestLegalHolds() : bean;
            }
        };
    }
}
