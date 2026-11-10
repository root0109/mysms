package com.mysms.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Learning Management microservice: owns schema "lms" and serves the lms contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class LmsApplication {

    protected LmsApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(LmsApplication.class, args);
    }
}
