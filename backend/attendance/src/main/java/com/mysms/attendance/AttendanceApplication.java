package com.mysms.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Attendance microservice: owns schema "attendance". In the monolith the same module runs inside {@code app}, whose
 * configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication
public class AttendanceApplication {

    protected AttendanceApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(AttendanceApplication.class, args);
    }
}
