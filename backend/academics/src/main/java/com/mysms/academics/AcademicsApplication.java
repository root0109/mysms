package com.mysms.academics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Academic Structure microservice: owns schema "academics" and serves the academics contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class AcademicsApplication {

    protected AcademicsApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(AcademicsApplication.class, args);
    }
}
