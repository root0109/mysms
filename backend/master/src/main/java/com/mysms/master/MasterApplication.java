package com.mysms.master;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Master Data microservice: owns schema "master" and serves the master contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class MasterApplication {

    protected MasterApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(MasterApplication.class, args);
    }
}
