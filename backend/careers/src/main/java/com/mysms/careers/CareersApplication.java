package com.mysms.careers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Careers microservice: owns schema "careers" and serves the careers contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class CareersApplication {

    protected CareersApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(CareersApplication.class, args);
    }
}
