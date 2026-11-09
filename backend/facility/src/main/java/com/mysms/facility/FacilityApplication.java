package com.mysms.facility;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Rooms & Facilities microservice: owns schema "facility" and serves the facility contract.
 * Its configuration is facility-service.yml, which imports the module's own mysms-facility.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class FacilityApplication {

    public static final String CONFIG_NAME = "facility-service";

    protected FacilityApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(FacilityApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
