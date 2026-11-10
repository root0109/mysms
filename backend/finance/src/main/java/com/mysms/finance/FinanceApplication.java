package com.mysms.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Fees & Payments microservice: owns schema "finance" and serves the finance contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class FinanceApplication {

    protected FinanceApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }
}
