package com.mysms.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Identity & Access microservice: owns schema "iam" and serves the iam contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class IamApplication {

    protected IamApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(IamApplication.class, args);
    }
}
