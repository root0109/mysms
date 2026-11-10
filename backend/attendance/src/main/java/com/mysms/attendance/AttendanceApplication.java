package com.mysms.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Attendance microservice: owns schema "attendance" and serves the attendance contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class AttendanceApplication {

    protected AttendanceApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(AttendanceApplication.class, args);
    }
}
