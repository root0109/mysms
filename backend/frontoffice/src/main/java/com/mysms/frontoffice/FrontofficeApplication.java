package com.mysms.frontoffice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Front Office microservice: owns schema "frontoffice" and serves the frontoffice contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class FrontofficeApplication {

    protected FrontofficeApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(FrontofficeApplication.class, args);
    }
}
