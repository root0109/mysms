package com.mysms.timetable;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Timetable microservice: owns schema "timetable" and serves the timetable contract.
 * Its configuration is timetable-service.yml, which imports the module's own mysms-timetable.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class TimetableApplication {

    public static final String CONFIG_NAME = "timetable-service";

    protected TimetableApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(TimetableApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
