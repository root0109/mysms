package com.mysms.comms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Communication & Community microservice: owns schema "comms" and serves the comms contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class CommsApplication {

    protected CommsApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(CommsApplication.class, args);
    }
}
