package com.mysms.people;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The People & Profiles microservice: owns schema "people" and serves the people contract.
 * Its configuration is people-service.yml, which imports the module's own mysms-people.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class PeopleApplication {

    public static final String CONFIG_NAME = "people-service";

    protected PeopleApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(PeopleApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
