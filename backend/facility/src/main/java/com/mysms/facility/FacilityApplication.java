package com.mysms.facility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Rooms & Facilities microservice: owns schema "facility" and serves the facility contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class FacilityApplication {

    protected FacilityApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(FacilityApplication.class, args);
    }
}
