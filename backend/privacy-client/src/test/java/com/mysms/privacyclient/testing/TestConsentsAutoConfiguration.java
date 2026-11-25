package com.mysms.privacyclient.testing;

import com.mysms.privacyclient.PrivacyDecisionGrpcClient;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Puts {@link TestConsents} in place of privacy's gRPC client in every component test of a module that checks consent
 * (privacy-client's test jar is on the component tests' classpath only), as auth-client's test jar does for iam.
 */
@AutoConfiguration
public final class TestConsentsAutoConfiguration {

    private TestConsentsAutoConfiguration() {
    }

    /** Static, as a post-processor bean must be, so the configuration is not created early. */
    @Bean
    static BeanPostProcessor testConsents() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String name) {
                return bean instanceof PrivacyDecisionGrpcClient ? new TestConsents() : bean;
            }
        };
    }
}
