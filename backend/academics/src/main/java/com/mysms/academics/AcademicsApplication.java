package com.mysms.academics;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Academic Structure microservice: owns schema "academics" and serves the academics contract.
 * Its configuration is academics-service.yml, which imports the module's own mysms-academics.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class AcademicsApplication {

    public static final String CONFIG_NAME = "academics-service";

    protected AcademicsApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(AcademicsApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
