package com.mysms.transport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Transport microservice: owns schema "transport" and serves the transport contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class TransportApplication {

    protected TransportApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(TransportApplication.class, args);
    }
}
