package com.mysms.org;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Organisation & Calendar microservice: owns schema "org" and serves the org contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class OrgApplication {

    protected OrgApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(OrgApplication.class, args);
    }
}
