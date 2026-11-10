package com.mysms.timetable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Timetable microservice: owns schema "timetable" and serves the timetable contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class TimetableApplication {

    protected TimetableApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(TimetableApplication.class, args);
    }
}
