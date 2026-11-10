package com.mysms.hr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Workforce / HR microservice: owns schema "hr" and serves the hr contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class HrApplication {

    protected HrApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(HrApplication.class, args);
    }
}
