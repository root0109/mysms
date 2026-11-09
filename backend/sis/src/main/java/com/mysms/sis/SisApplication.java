package com.mysms.sis;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Student Information microservice: owns schema "sis" and serves the sis contract.
 * Its configuration is sis-service.yml, which imports the module's own mysms-sis.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class SisApplication {

    public static final String CONFIG_NAME = "sis-service";

    protected SisApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(SisApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
