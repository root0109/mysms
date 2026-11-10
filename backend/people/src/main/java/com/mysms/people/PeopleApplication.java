package com.mysms.people;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The People & Profiles microservice: owns schema "people" and serves the people contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class PeopleApplication {

    protected PeopleApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(PeopleApplication.class, args);
    }
}
