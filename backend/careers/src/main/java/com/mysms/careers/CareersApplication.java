package com.mysms.careers;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Careers microservice: owns schema "careers" and serves the careers contract.
 * Its configuration is careers-service.yml, which imports the module's own mysms-careers.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class CareersApplication {

    public static final String CONFIG_NAME = "careers-service";

    protected CareersApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(CareersApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
