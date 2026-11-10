package com.mysms.assessment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Assessment & Results microservice: owns schema "assessment" and serves the assessment contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class AssessmentApplication {

    protected AssessmentApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(AssessmentApplication.class, args);
    }
}
