package com.mysms.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class CoreAutoConfigurationTest {

    @Test
    void providesTheCrossCuttingBeans() {
        CoreAutoConfiguration config = new CoreAutoConfiguration();
        assertThat(config.clock()).isEqualTo(Clock.systemUTC());
        assertThat(config.traceFilter()).isNotNull();
        assertThat(config.globalExceptionHandler(config.messageUtil())).isNotNull();
        assertThat(config.problemResponseWriter(JsonMapper.builder().build())).isNotNull();
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(config.dtoValidator(factory.getValidator())).isNotNull();
        }
    }
}
