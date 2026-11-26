package com.mysms.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class OpenApiGroupsEnvironmentPostProcessorTest {

    @Test
    void eachServedAreaGetsItsOwnGroupOfItsOwnPaths() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("module",
                Map.of("mysms.auth.modules[0]", "hr", "mysms.auth.modules[1]", "payroll")));

        new OpenApiGroupsEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("springdoc.group-configs[0].group")).isEqualTo("hr");
        assertThat(environment.getProperty("springdoc.group-configs[0].paths-to-match")).isEqualTo("/v1/hr/**");
        assertThat(environment.getProperty("springdoc.group-configs[1].group")).isEqualTo("payroll");
        assertThat(environment.getProperty("springdoc.group-configs[1].paths-to-match")).isEqualTo("/v1/payroll/**");
    }

    @Test
    void aProcessServingNoAreaGetsNoGroups() {
        StandardEnvironment environment = new StandardEnvironment();
        new OpenApiGroupsEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
        assertThat(environment.getPropertySources().contains(OpenApiGroupsEnvironmentPostProcessor.PROPERTY_SOURCE))
                .isFalse();
    }
}
