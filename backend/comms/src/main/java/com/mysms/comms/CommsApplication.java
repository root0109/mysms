package com.mysms.comms;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Communication & Community microservice: owns schema "comms" and serves the comms contract.
 * Its configuration is comms-service.yml, which imports the module's own mysms-comms.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class CommsApplication {

    public static final String CONFIG_NAME = "comms-service";

    protected CommsApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(CommsApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
