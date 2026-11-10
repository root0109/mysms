package com.mysms.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Platform & Tenancy microservice: owns schema "platform" and serves the platform contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class PlatformApplication {

    protected PlatformApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
