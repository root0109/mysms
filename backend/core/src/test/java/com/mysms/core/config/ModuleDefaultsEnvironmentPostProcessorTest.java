package com.mysms.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class ModuleDefaultsEnvironmentPostProcessorTest {

    private static StandardEnvironment environmentWith(Map<String, Object> variables, String... modules) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("variables", variables));
        environment.getPropertySources().addLast(new MapPropertySource(
                ModuleDefaultsEnvironmentPostProcessor.PROPERTY_SOURCE,
                ModuleDefaultsEnvironmentPostProcessor.defaults(List.of(modules))));
        return environment;
    }

    @Test
    void oneModuleIsItsOwnMicroserviceAndReadsItsOwnVariablesBeforeTheSharedOnes() {
        StandardEnvironment environment = environmentWith(Map.of(
                "MYSMS_DB_URL", "jdbc:postgresql://shared/mysms",
                "MYSMS_FINANCE_DB_USER", "finance_app",
                "MYSMS_DB_USER", "shared_app"), "finance");

        assertThat(environment.getProperty("spring.application.name")).isEqualTo("finance");
        assertThat(environment.getProperty("mysms.auth.modules[0]")).isEqualTo("finance");
        assertThat(environment.getProperty("mysms.finance.datasource.url")).isEqualTo("jdbc:postgresql://shared/mysms");
        assertThat(environment.getProperty("mysms.finance.datasource.username")).isEqualTo("finance_app");
        assertThat(environment.getProperty("mysms.finance.datasource.password")).isEmpty();
        assertThat(environment.getProperty("mysms.finance.datasource.maximum-pool-size")).isEqualTo("10");
        // a microservice publishes as its module's broker user, chosen by the bus (blank here)
        assertThat(environment.getProperty("mysms.events.rabbitmq.publisher-user")).isEmpty();
        assertThat(environment.getProperty("mysms.events.bus")).isEqualTo("rabbitmq");
    }

    @Test
    void severalModulesAreTheMonolithAndEveryModuleGetsItsOwnDatasource() {
        StandardEnvironment environment = environmentWith(Map.of(), "sis", "academics");

        assertThat(environment.getProperty("spring.application.name"))
                .isEqualTo(ModuleDefaultsEnvironmentPostProcessor.MONOLITH);
        // sorted, so every start of the same jars sees the same list
        assertThat(environment.getProperty("mysms.auth.modules[0]")).isEqualTo("academics");
        assertThat(environment.getProperty("mysms.auth.modules[1]")).isEqualTo("sis");
        assertThat(environment.getProperty("mysms.sis.datasource.url")).isEqualTo("jdbc:postgresql://localhost:5432/mysms");
        assertThat(environment.getProperty("mysms.academics.datasource.username")).isEqualTo("mysms");
        assertThat(environment.getProperty("mysms.events.rabbitmq.publisher-user")).isEqualTo("self");
    }

    @Test
    void anyOtherSourceOverridesTheDefaults() {
        StandardEnvironment environment = environmentWith(Map.of(
                "mysms.events.bus", "in-process",
                "mysms.iam.datasource.maximum-pool-size", "4"), "iam");

        assertThat(environment.getProperty("mysms.events.bus")).isEqualTo("in-process");
        assertThat(environment.getProperty("mysms.iam.datasource.maximum-pool-size")).isEqualTo("4");
    }

    /** A module's own application: it scans its own package, com.mysms.finance in a real module. */
    @org.springframework.boot.autoconfigure.SpringBootApplication(scanBasePackages = "com.mysms.finance")
    private static final class OneModule {
    }

    /** The monolith's shape: one application that scans every module's package. */
    @org.springframework.context.annotation.ComponentScan(basePackages = {"com.mysms.sis", "com.mysms.academics.business",
            "com.mysms.core", "com.mysms.app", "org.example"})
    private static final class SeveralModules {
    }

    /** Packages named by a class in them: here core's own, so no module. */
    @org.springframework.context.annotation.ComponentScan(basePackageClasses = ModuleDefaultsEnvironmentPostProcessor.class)
    private static final class ByMarkerClass {
    }

    @Test
    void theModulesAreThePackagesTheApplicationScans() {
        assertThat(ModuleDefaultsEnvironmentPostProcessor.modulesOf(List.of(OneModule.class))).containsExactly("finance");
        assertThat(ModuleDefaultsEnvironmentPostProcessor.modulesOf(List.of(SeveralModules.class, "a.package.name")))
                .as("sorted; libraries, the monolith's shell and foreign packages are not modules")
                .containsExactly("academics", "sis");
        // packages may be named by a class in them; an application that names none scans its own (core's: a library)
        assertThat(ModuleDefaultsEnvironmentPostProcessor.modulesOf(List.of(ByMarkerClass.class))).isEmpty();
        assertThat(ModuleDefaultsEnvironmentPostProcessor.modulesOf(List.of(getClass()))).isEmpty();
    }

    @Test
    void aProcessWithoutModulesGetsNoDefaults() {
        StandardEnvironment environment = new StandardEnvironment();
        new ModuleDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getPropertySources().contains(ModuleDefaultsEnvironmentPostProcessor.PROPERTY_SOURCE))
                .isFalse();
    }
}
