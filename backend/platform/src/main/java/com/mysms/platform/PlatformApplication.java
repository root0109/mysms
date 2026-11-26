package com.mysms.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * The Platform microservice: its areas (platform, master, org) each own their schema, pool, transaction manager and
 * contract, served by one process. Components are named by fully qualified class name, as in the monolith, so two
 * areas may each have, say, a PersonMapper. In the monolith the same module runs inside {@code app}, whose
 * configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class PlatformApplication {

    protected PlatformApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
