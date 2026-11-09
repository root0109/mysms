package com.mysms.transport;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Transport microservice: owns schema "transport" and serves the transport contract.
 * Its configuration is transport-service.yml, which imports the module's own mysms-transport.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class TransportApplication {

    public static final String CONFIG_NAME = "transport-service";

    protected TransportApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(TransportApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
