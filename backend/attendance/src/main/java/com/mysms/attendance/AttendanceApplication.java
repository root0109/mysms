package com.mysms.attendance;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Attendance microservice: owns schema "attendance" and serves the attendance contract.
 * Its configuration is attendance-service.yml, which imports the module's own mysms-attendance.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class AttendanceApplication {

    public static final String CONFIG_NAME = "attendance-service";

    protected AttendanceApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(AttendanceApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
