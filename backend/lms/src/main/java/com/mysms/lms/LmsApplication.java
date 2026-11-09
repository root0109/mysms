package com.mysms.lms;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Learning Management microservice: owns schema "lms" and serves the lms contract.
 * Its configuration is lms-service.yml, which imports the module's own mysms-lms.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class LmsApplication {

    public static final String CONFIG_NAME = "lms-service";

    protected LmsApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(LmsApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
