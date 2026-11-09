package com.mysms.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class CoreAutoConfigurationTest {

    @Test
    void providesTheCrossCuttingBeans() {
        CoreAutoConfiguration config = new CoreAutoConfiguration();
        assertThat(config.clock()).isEqualTo(Clock.systemUTC());
        assertThat(config.traceFilter()).isNotNull();
        assertThat(config.globalExceptionHandler()).isNotNull();
        assertThat(config.problemResponseWriter(JsonMapper.builder().build())).isNotNull();
        assertThat(config.tracePropagationInterceptor()).isNotNull();
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(config.dtoValidator(factory.getValidator())).isNotNull();
        }
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("trace", config.tracePropagationInterceptor());
        assertThat(config.sdkClientFactory(RestClient.builder(), beans.getBeanProvider(ClientHttpRequestInterceptor.class)))
                .isNotNull();
    }
}
