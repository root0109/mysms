package com.mysms.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;

class ModulePropertiesEnvironmentPostProcessorTest {

    @TempDir
    Path root;

    /** A classpath root holding an application.properties and, for a module, its operation table. */
    private URL classpathRoot(String directory, String properties, String operationTable) throws IOException {
        Path dir = Files.createDirectories(root.resolve(directory));
        Files.writeString(dir.resolve("application.properties"), properties);
        if (operationTable != null) {
            Path table = dir.resolve("META-INF/mysms/operations/" + operationTable + ".json");
            Files.createDirectories(table.getParent());
            Files.writeString(table, "[]");
        }
        return dir.toUri().toURL();
    }

    private StandardEnvironment run(URL... classpath) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("operator", Map.of("mysms.events.bus", "none")));
        try (URLClassLoader loader = new URLClassLoader(classpath, null)) {
            SpringApplication application = new SpringApplication();
            application.setResourceLoader(new DefaultResourceLoader(loader));
            new ModulePropertiesEnvironmentPostProcessor().postProcessEnvironment(environment, application);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return environment;
    }

    @Test
    void aMicroserviceHasOnlyItsOwnFileWhichSpringBootLoads() throws IOException {
        StandardEnvironment environment = run(
                classpathRoot("students", "spring.application.name=students\nmysms.auth.modules[0]=sis\n", "sis"));

        assertThat(environment.getPropertySources().stream().map(s -> s.getName()))
                .noneMatch(name -> name.startsWith(ModulePropertiesEnvironmentPostProcessor.PROPERTY_SOURCE_PREFIX));
    }

    @Test
    void theMonolithCombinesEveryModuleFileBelowItsOwnAndAnyOperatorSetting() throws IOException {
        StandardEnvironment environment = run(
                classpathRoot("app", "spring.application.name=mysms\n", null),
                classpathRoot("students", """
                        spring.application.name=students
                        mysms.auth.modules[0]=sis
                        mysms.auth.modules[1]=admission
                        mysms.sis.datasource.maximum-pool-size=10
                        mysms.events.bus=rabbitmq
                        """, "sis"),
                classpathRoot("identity", "spring.application.name=identity\nmysms.auth.modules[0]=iam\n"
                        + "mysms.iam.smtp.port=587\n", "iam"));

        assertThat(environment.getProperty("mysms.sis.datasource.maximum-pool-size")).isEqualTo("10");
        assertThat(environment.getProperty("mysms.iam.smtp.port")).isEqualTo("587");
        // the operator's value wins over every module file
        assertThat(environment.getProperty("mysms.events.bus")).isEqualTo("none");
        assertThat(environment.getPropertySources().stream().map(s -> s.getName()))
                .endsWith("mysmsModuleProperties:students", "mysmsModuleProperties:identity");
    }

    @Test
    void aFileThatIsNotAModulesIsNeverCombined() throws IOException {
        StandardEnvironment environment = run(
                classpathRoot("app", "spring.application.name=mysms\n", null),
                classpathRoot("thirdparty", "spring.application.name=vendor\nvendor.setting=1\n", null),
                classpathRoot("nameless", "other.setting=2\n", null));

        assertThat(environment.getProperty("vendor.setting")).isNull();
        assertThat(environment.getProperty("other.setting")).isNull();
    }
}
