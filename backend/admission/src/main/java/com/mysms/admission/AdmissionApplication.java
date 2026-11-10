package com.mysms.admission;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Admissions microservice: owns schema "admission" and serves the admission contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class AdmissionApplication {

    protected AdmissionApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(AdmissionApplication.class, args);
    }
}
