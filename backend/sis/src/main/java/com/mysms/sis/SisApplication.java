package com.mysms.sis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Student Information microservice: owns schema "sis" and serves the sis contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class SisApplication {

    protected SisApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(SisApplication.class, args);
    }
}
